import { expect, test, type Page } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const teacher = { id: 'teacher-packages-fixture', email: 'teacher-packages@example.test', fullName: 'Nguyễn Minh An', role: 'TEACHER', status: 'ACTIVE' };
const envelope = (data: unknown, meta?: unknown) => ({ success: true, message: 'ok', data, errors: null, meta: meta ?? null });
const failed = { success: false, message: 'temporary error', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null };
const packageFixtures = [
  { id: 'package-1', name: 'Toán cơ bản', subjectId: 'math', subjectName: 'Toán', description: 'Nền tảng đại số và hình học', priceVnd: 500000, totalSessions: 10, sessionDurationMinutes: 60, durationDays: 60, status: 'ACTIVE', version: 2 },
  { id: 'package-2', name: 'Toán nâng cao', subjectId: 'math', subjectName: 'Toán', description: 'Luyện chuyên đề nâng cao', priceVnd: 800000, totalSessions: 12, sessionDurationMinutes: 60, durationDays: 90, status: 'INACTIVE', version: 4 },
];

async function setup(page: Page, options: { empty?: boolean; failGet?: boolean; holdGet?: boolean; approval?: string; many?: boolean; conflict?: boolean } = {}) {
  const requests: Array<{ method: string; path: string; query: string; body?: string }> = [];
  let failGet = options.failGet ?? false;
  let release: (() => void) | undefined;
  const gate = options.holdGet ? new Promise<void>(resolve => { release = resolve; }) : null;
  let records = options.many ? Array.from({ length: 13 }, (_, index) => ({ ...packageFixtures[index % packageFixtures.length], id: `package-${index + 1}`, name: `Gói Toán ${index + 1}` })) : packageFixtures;
  await page.route('**/api/teacher/profile', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope({ profileStatus: options.approval ?? 'APPROVED' })) }));
  await page.route('**/api/teacher/packages**', async route => {
    const request = route.request();
    const url = new URL(request.url());
    requests.push({ method: request.method(), path: url.pathname, query: url.search, body: request.postData() ?? undefined });
    if (request.method() === 'PATCH') {
      if (options.conflict) return route.fulfill({ status: 409, contentType: 'application/json', body: JSON.stringify({ success: false, message: 'conflict', data: null, errors: [{ code: 'CONCURRENT_MODIFICATION' }], meta: null }) });
      const id = url.pathname.split('/').at(-2);
      const body = JSON.parse(request.postData() ?? '{}');
      records = records.map(item => item.id === id ? { ...item, status: body.status, version: item.version + 1 } : item);
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope({})) });
    }
    if (request.method() === 'GET') {
      if (gate) await gate;
      if (failGet) return route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify(failed) });
      const pageNumber = Number(url.searchParams.get('page') ?? 0);
      const pageItems = options.empty ? [] : options.many ? (pageNumber === 0 ? records.slice(0, 12) : records.slice(12)) : records;
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(pageItems, { page: pageNumber, size: 12, totalElements: options.empty ? 0 : options.many ? 13 : records.length, totalPages: options.many ? 2 : 1 })) });
    }
    return route.fulfill({ status: 405, body: '' });
  });
  return { requests, retry: () => { failGet = false; }, release: () => release?.() };
}

async function openPage(page: Page) {
  await page.addInitScript(user => {
    try {
      localStorage.setItem('tutor-match.session', JSON.stringify({ user, remember: false, sessionId: 'packages-fixture-session' }));
      document.cookie = 'accessToken=packages-fixture-access; path=/; SameSite=Lax';
    } catch {
      // The initial about:blank document has no storage origin; the route navigation does.
    }
  }, teacher);
  await page.goto('/teacher/packages');
}

test('teacher packages show terms, statuses and actions at 375/768/1280', async ({ page }) => {
  const { requests } = await setup(page);
  await openPage(page);
  await expect(page.getByRole('heading', { name: 'Gói học của bạn' })).toBeVisible();
  await expect(page.getByText('500.000đ')).toBeVisible();
  await expect(page.getByText('Đang mở bán')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Ngừng bán' })).toBeVisible();
  expect(requests).toContainEqual(expect.objectContaining({ method: 'GET', path: '/api/teacher/packages', query: '?page=0&size=12' }));
  await page.keyboard.press('Tab');
  await expect(page.locator(':focus-visible').first()).toBeVisible();
  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    if (variant !== 'before') expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidence, `teacher-packages-${variant}-${width}.png`), fullPage: true });
    if (width === 375 && variant !== 'before') {
      const lastAction = page.getByRole('button', { name: 'Mở bán' });
      await page.evaluate(async () => {
        document.documentElement.style.scrollBehavior = 'auto';
        window.scrollTo(0, document.documentElement.scrollHeight);
        await new Promise(resolve => setTimeout(resolve, 100));
      });
      const clearance = await page.evaluate(() => {
        const button = [...document.querySelectorAll('button')].find(node => node.textContent?.trim() === 'Mở bán');
        const nav = [...document.querySelectorAll('nav')].map(node => ({ rect: node.getBoundingClientRect(), style: getComputedStyle(node) }))
          .find(item => item.style.position === 'fixed' && item.rect.bottom >= window.innerHeight - 1);
        return { buttonBottom: button?.getBoundingClientRect().bottom, navTop: nav?.rect.top };
      });
      expect(clearance.buttonBottom).toBeDefined();
      expect(clearance.navTop).toBeDefined();
      expect(clearance.buttonBottom!).toBeLessThan(clearance.navTop! - 8);
    }
  }
});

test('teacher can navigate to create/edit and toggle status using current version', async ({ page }) => {
  const { requests } = await setup(page);
  await openPage(page);
  await page.getByRole('button', { name: 'Chỉnh sửa' }).first().click();
  await expect(page).toHaveURL(/\/teacher\/packages\/package-1$/);
  await page.goBack();
  await expect(page.getByText('Toán cơ bản')).toBeVisible();
  await page.getByRole('button', { name: 'Ngừng bán' }).click();
  await expect(page.getByRole('button', { name: 'Mở bán' }).first()).toBeVisible();
  expect(requests).toContainEqual(expect.objectContaining({ method: 'PATCH', path: '/api/teacher/packages/package-1/status', body: JSON.stringify({ status: 'INACTIVE', version: 2 }) }));
  await page.getByRole('button', { name: 'Tạo gói học' }).click();
  await expect(page).toHaveURL(/\/teacher\/packages\/create$/);
});

test('loading, empty, GET error/retry and approval gate remain distinct', async ({ page }) => {
  const loading = await setup(page, { holdGet: true });
  await openPage(page);
  await expect(page.getByRole('status', { name: 'Đang tải gói học' })).toBeVisible();
  loading.release();
  await expect(page.getByText('Toán cơ bản')).toBeVisible();

  await page.unrouteAll({ behavior: 'wait' });
  const retry = await setup(page, { failGet: true });
  await page.reload();
  await expect(page.getByText('Chưa tải được gói học')).toBeVisible();
  retry.retry();
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByText('Toán cơ bản')).toBeVisible();

  await page.unrouteAll({ behavior: 'wait' });
  await setup(page, { empty: true, approval: 'PENDING_APPROVAL' });
  await page.reload();
  await expect(page.getByText('Chưa có gói học')).toBeVisible();
  await expect(page.getByText('Hồ sơ cần được duyệt trước khi tạo gói học')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Tạo gói học' })).toBeDisabled();
});

test('pagination requests the selected page with unchanged page size', async ({ page }) => {
  const { requests } = await setup(page, { many: true });
  await openPage(page);
  await expect(page.getByRole('heading', { name: 'Gói Toán 1', exact: true })).toBeVisible();
  await page.locator('.ant-pagination-item-2').click();
  await expect(page.getByText('Gói Toán 13')).toBeVisible();
  expect(requests).toContainEqual(expect.objectContaining({ method: 'GET', path: '/api/teacher/packages', query: '?page=1&size=12' }));
});

test('concurrent status conflict shows recovery message and refetches server truth', async ({ page }) => {
  const { requests } = await setup(page, { conflict: true });
  await openPage(page);
  await page.getByRole('button', { name: 'Ngừng bán' }).click();
  await expect(page.getByText('Dữ liệu gói đã thay đổi, vui lòng tải lại.')).toBeVisible();
  expect(requests.filter(request => request.method === 'GET' && request.path === '/api/teacher/packages')).toHaveLength(2);
});
