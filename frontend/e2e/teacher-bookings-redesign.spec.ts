import { expect, test, type Page } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const teacher = { id: 'teacher-bookings-fixture', email: 'teacher@example.test', fullName: 'Nguyễn Minh An', role: 'TEACHER', status: 'ACTIVE' };
const bookings = [
  { id: 'booking-1', teacher: { id: 'teacher-1', fullName: 'Nguyễn Minh An' }, student: { id: 'student-1', fullName: 'Nguyễn Minh Anh' }, studentPackageId: 'package-1', subject: { id: 'subject-math', name: 'Toán học' }, startTime: '2026-10-18T02:00:00Z', endTime: '2026-10-18T03:00:00Z', deliveryMode: 'ONLINE', status: 'SCHEDULED', trial: false, outsideAvailabilityWarning: false, version: 4, settlementStatus: 'AWAITING_CONFIRMATION', settlement: { status: 'AWAITING_CONFIRMATION', confirmationDeadline: '2026-10-19T02:00:00Z', version: 1 } },
  { id: 'booking-2', teacher: { id: 'teacher-1', fullName: 'Nguyễn Minh An' }, student: { id: 'student-2', fullName: 'Trần Gia Bảo' }, studentPackageId: 'package-2', subject: { id: 'subject-physics', name: 'Vật lý' }, startTime: '2026-10-20T08:00:00Z', endTime: '2026-10-20T09:00:00Z', deliveryMode: 'OFFLINE', status: 'COMPLETED', trial: true, outsideAvailabilityWarning: false, version: 2, settlementStatus: null, settlement: null },
];
const envelope = (data: unknown, page = 0) => ({ success: true, message: 'ok', data, errors: null, meta: { page, size: 12, totalElements: data instanceof Array && data.length ? 24 : 0, totalPages: data instanceof Array && data.length ? 2 : 0 } });

async function openPage(page: Page) {
  await page.addInitScript(user => {
    localStorage.setItem('tutor-match.session', JSON.stringify({ user, remember: false, sessionId: 'teacher-bookings-session' }));
    document.cookie = 'accessToken=teacher-bookings-access; path=/; SameSite=Lax';
  }, teacher);
  await page.goto('/teacher/bookings');
}

async function interceptBookings(page: Page, options: { fail?: boolean; hold?: boolean; empty?: boolean } = {}) {
  const requests: Array<{ method: string; path: string; params: Record<string, string> }> = [];
  let releaseGet: (() => void) | undefined;
  const gate = options.hold ? new Promise<void>(resolve => { releaseGet = resolve; }) : null;
  await page.route('**/api/teacher/bookings*', async route => {
    const request = route.request();
    const url = new URL(request.url());
    requests.push({ method: request.method(), path: url.pathname, params: Object.fromEntries(url.searchParams.entries()) });
    if (gate) await gate;
    if (options.fail) return route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ success: false, message: 'unavailable', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null }) });
    const pageNumber = Number(url.searchParams.get('page') ?? 0);
    const data = options.empty ? [] : pageNumber === 0 ? bookings : [bookings[0]];
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(data, pageNumber)) });
  });
  return { requests, releaseGet: () => releaseGet?.() };
}

test('teacher booking list follows the approved hierarchy and fits 375/768/1280', async ({ page }) => {
  const { requests } = await interceptBookings(page);
  await openPage(page);
  await expect(page.getByRole('heading', { name: 'Lịch dạy' })).toBeVisible();
  expect(requests).toContainEqual(expect.objectContaining({ method: 'GET', path: '/api/teacher/bookings' }));
  if (variant !== 'before') {
    await expect(page.getByText('Nguyễn Minh Anh')).toBeVisible();
    await expect(page.getByText('Chờ xác nhận')).toBeVisible();
    await expect(page.getByRole('link', { name: 'Chọn học viên để tạo buổi' })).toHaveAttribute('href', '/teacher/students');
  }
  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    if (variant !== 'before') expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidence, `teacher-bookings-${variant}-${width}.png`), fullPage: true });
    if (width === 375 && variant !== 'before') {
      await page.evaluate(() => window.scrollTo(0, document.documentElement.scrollHeight));
      const clearance = await page.evaluate(() => {
        const lastAction = [...document.querySelectorAll('a')].find(element => element.textContent?.trim() === 'Xem chi tiết');
        const pagination = document.querySelector('.ant-pagination');
        const nav = [...document.querySelectorAll('nav')].map(node => ({ rect: node.getBoundingClientRect(), style: getComputedStyle(node) })).find(item => item.style.position === 'fixed' && item.rect.bottom >= window.innerHeight - 1);
        return { actionBottom: lastAction?.getBoundingClientRect().bottom, paginationBottom: pagination?.getBoundingClientRect().bottom, navTop: nav?.rect.top };
      });
      expect(clearance.actionBottom).toBeLessThan(clearance.navTop! - 8);
      expect(clearance.paginationBottom).toBeLessThan(clearance.navTop! - 8);
    }
  }
});

test('status/date filters preserve teacher API params and detail/create destinations', async ({ page }) => {
  const { requests } = await interceptBookings(page);
  await openPage(page);
  const statusFilter = page.getByRole('combobox', { name: 'Trạng thái buổi học' });
  await statusFilter.click();
  await statusFilter.press('ArrowDown');
  await statusFilter.press('Enter');
  await expect.poll(() => requests.at(-1)?.params.status).toBe('COMPLETED');
  await page.locator('input[type="date"]').fill('2026-10-18');
  await expect.poll(() => requests.at(-1)?.params.from).toBe('2026-10-17T17:00:00.000Z');
  expect(requests.at(-1)?.params).toEqual(expect.objectContaining({ status: 'COMPLETED', from: '2026-10-17T17:00:00.000Z', to: '2026-10-18T17:00:00.000Z', page: '0', size: '12', sort: 'startTime,asc' }));
  await expect(page.getByRole('link', { name: 'Xem chi tiết' }).first()).toHaveAttribute('href', '/teacher/bookings/booking-1');
  await expect(page.getByRole('link', { name: 'Chọn học viên để tạo buổi' })).toHaveAttribute('href', '/teacher/students');
  await page.getByRole('link', { name: 'Chọn học viên để tạo buổi' }).focus();
  await page.keyboard.press('Tab');
  await expect(page.getByRole('link', { name: 'Xem chi tiết' }).first()).toBeFocused();
  expect(await page.getByRole('link', { name: 'Xem chi tiết' }).first().evaluate(element => element.matches(':focus-visible'))).toBe(true);
  await page.getByRole('listitem', { name: '2' }).click();
  await expect.poll(() => requests.at(-1)?.params.page).toBe('1');
});

test('loading, GET error/retry and empty state remain distinct', async ({ page }) => {
  const held = await interceptBookings(page, { hold: true });
  await openPage(page);
  await expect(page.getByRole('status')).toContainText('Đang tải lịch dạy');
  held.releaseGet();
  await expect(page.getByText('Nguyễn Minh Anh')).toBeVisible();

  await page.unrouteAll({ behavior: 'wait' });
  await interceptBookings(page, { fail: true });
  await page.reload();
  await expect(page.getByText('Chưa tải được lịch')).toBeVisible();
  await page.unrouteAll({ behavior: 'wait' });
  await interceptBookings(page, { empty: true });
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByText('Chưa có buổi học với điều kiện đã chọn.')).toBeVisible();
});
