import { expect, test, type Page } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const teacher = { id: 'teacher-availability-fixture', email: 'teacher@example.test', fullName: 'Nguyễn Minh An', role: 'TEACHER', status: 'ACTIVE' };
const availability = [
  { id: 'slot-1', dayOfWeek: 'MONDAY', startTime: '08:00:00', endTime: '09:00:00', timezone: 'Asia/Ho_Chi_Minh', isActive: true },
  { id: 'slot-2', dayOfWeek: 'TUESDAY', startTime: '09:00:00', endTime: '10:00:00', timezone: 'Asia/Ho_Chi_Minh', isActive: true },
];
const envelope = (data: unknown) => ({ success: true, message: 'ok', data, errors: null, meta: null });

async function openPage(page: Page) {
  await page.addInitScript(user => {
    try {
      localStorage.setItem('tutor-match.session', JSON.stringify({ user, remember: false, sessionId: 'availability-session' }));
      document.cookie = 'accessToken=availability-access; path=/; SameSite=Lax';
    } catch {
      // The initial about:blank document has no storage origin; the route navigation does.
    }
  }, teacher);
  await page.goto('/teacher/availability');
}

async function interceptAvailability(page: Page, options: { failGet?: boolean; failPut?: boolean; holdGet?: boolean; data?: unknown } = {}) {
  const requests: Array<{ method: string; path: string; body?: string }> = [];
  let releaseGet: (() => void) | undefined;
  const getGate = options.holdGet ? new Promise<void>(resolve => { releaseGet = resolve; }) : null;
  await page.route('**/api/teacher/availability', async route => {
    const request = route.request();
    requests.push({ method: request.method(), path: new URL(request.url()).pathname, body: request.postData() ?? undefined });
    if (request.method() === 'GET') {
      if (getGate) await getGate;
      if (options.failGet) return route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ success: false, message: 'error', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null }) });
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(options.data ?? availability)) });
    }
    if (options.failPut) return route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ success: false, message: 'error', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null }) });
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope([])) });
  });
  return { requests, releaseGet: () => releaseGet?.() };
}

test('availability design shows the same fixture at 375/768/1280 without page overflow', async ({ page }) => {
  const { requests } = await interceptAvailability(page);
  await openPage(page);
  await expect(page.getByRole('heading', { name: 'Lịch rảnh giảng dạy' })).toBeVisible();
  expect(requests).toContainEqual(expect.objectContaining({ method: 'GET', path: '/api/teacher/availability' }));
  if (variant !== 'before') {
    const timezone = await page.evaluate(() => Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Ho_Chi_Minh');
    await expect(page.getByText(`Thời gian theo múi giờ ${timezone}`)).toBeVisible();
    await expect(page.getByRole('button', { name: /^Thứ 2, 08:00–09:00,/ })).toHaveAttribute('aria-pressed', 'true');
    await expect(page.getByRole('button', { name: /^Thứ 3, 09:00–10:00,/ })).toHaveAttribute('aria-pressed', 'true');
  }
  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    if (variant !== 'before') expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidence, `teacher-availability-${variant}-${width}.png`), fullPage: true });
    if (width === 375 && variant !== 'before') {
      await page.evaluate(async () => {
        document.documentElement.style.scrollBehavior = 'auto';
        window.scrollTo(0, document.documentElement.scrollHeight);
        await new Promise(resolve => setTimeout(resolve, 100));
      });
      const clearance = await page.evaluate(() => {
        const save = [...document.querySelectorAll('button')].find(button => button.textContent?.trim() === 'Lưu lịch rảnh');
        const nav = [...document.querySelectorAll('nav')].map(node => ({ rect: node.getBoundingClientRect(), style: getComputedStyle(node) })).find(item => item.style.position === 'fixed' && item.rect.bottom >= window.innerHeight - 1);
        return { saveBottom: save?.getBoundingClientRect().bottom, navTop: nav?.rect.top };
      });
      expect(clearance.saveBottom).toBeDefined();
      expect(clearance.navTop).toBeDefined();
      expect(clearance.saveBottom!).toBeLessThan(clearance.navTop! - 8);
    }
  }
});

test('accessible slot buttons toggle by keyboard and submit exact weekday/timezone contract', async ({ page }) => {
  const { requests } = await interceptAvailability(page);
  await openPage(page);
  const monday = page.getByRole('button', { name: /^Thứ 2, 08:00–09:00,/ });
  await monday.focus();
  await expect(monday).toBeFocused();
  await page.keyboard.press('Space');
  await expect(monday).toHaveAttribute('aria-pressed', 'false');
  await page.getByRole('button', { name: /^Thứ 4, 10:00–11:00,/ }).click();
  await page.getByRole('button', { name: 'Lưu lịch rảnh' }).click();
  await expect(page.getByText('Cập nhật lịch rảnh thành công')).toBeVisible();
  const timezone = await page.evaluate(() => Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Ho_Chi_Minh');
  const put = requests.find(request => request.method === 'PUT');
  expect(put?.path).toBe('/api/teacher/availability');
  expect(JSON.parse(put?.body ?? '{}')).toEqual({ items: [
    { dayOfWeek: 'TUESDAY', startTime: '09:00', endTime: '10:00', timezone, isActive: true },
    { dayOfWeek: 'WEDNESDAY', startTime: '10:00', endTime: '11:00', timezone, isActive: true },
  ] });
});

test('GET failure blocks save; empty and PUT failure preserve the selected schedule', async ({ page }) => {
  await interceptAvailability(page, { failGet: true });
  await openPage(page);
  await expect(page.getByText('Chưa tải được lịch rảnh')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Lưu lịch rảnh' })).toBeDisabled();

  await page.unrouteAll({ behavior: 'wait' });
  await interceptAvailability(page, { data: [] });
  await page.reload();
  await expect(page.getByText('Chưa có khung giờ nào được chọn.')).toBeVisible();

  await page.unrouteAll({ behavior: 'wait' });
  const { requests } = await interceptAvailability(page, { failPut: true });
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Lịch rảnh giảng dạy' })).toBeVisible();
  await page.getByRole('button', { name: /^Thứ 2, 08:00–09:00,/ }).click();
  await page.getByRole('button', { name: 'Lưu lịch rảnh' }).click();
  await expect(page.getByText('Lưu lịch rảnh thất bại')).toBeVisible();
  await expect(page.getByRole('button', { name: /^Thứ 2, 08:00–09:00,/ })).toHaveAttribute('aria-pressed', 'false');
  expect(requests.some(request => request.method === 'PUT')).toBe(true);
});

test('loading exposes a status before the schedule data is ready', async ({ page }) => {
  const { releaseGet } = await interceptAvailability(page, { holdGet: true });
  await openPage(page);
  await expect(page.getByRole('status', { name: 'Đang tải lịch rảnh' })).toBeVisible();
  releaseGet();
  await expect(page.getByRole('heading', { name: 'Khung giờ trong tuần' })).toBeVisible();
});
