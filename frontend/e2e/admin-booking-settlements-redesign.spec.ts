import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const admin = { id: 'admin-settlements', email: 'admin@example.test', fullName: 'Quản trị viên', role: 'ADMIN', status: 'ACTIVE' };
const settlement = {
  bookingId: 'booking-0001-uuid', studentId: 'student-0002-uuid', studentName: 'Nguyễn Minh An',
  teacherId: 'teacher-0003-uuid', teacherName: 'Trần Gia Bảo', bookingStatus: 'COMPLETED',
  startTime: '2026-10-14T09:00:00Z', endTime: '2026-10-14T10:00:00Z', status: 'AWAITING_ADMIN_DECISION',
  teacherConfirmedAt: '2026-10-14T10:01:00Z', studentConfirmedAt: null,
  confirmationDeadline: '2026-10-15T10:00:00Z', reopenDeadline: null, netAmountVnd: 275000,
  disputeReason: null, disputedAt: null, version: 8,
};

async function seedAdmin(page: import('@playwright/test').Page) {
  await page.addInitScript(user => {
    localStorage.setItem('tutor-match.session', JSON.stringify({ user, remember: false, sessionId: 'admin-settlements-session' }));
    document.cookie = 'accessToken=admin-settlements-access; path=/; SameSite=Lax';
  }, admin);
}

async function mockSettlements(page: import('@playwright/test').Page, options: { empty?: boolean; failListOnce?: boolean; row?: typeof settlement; mutationStatus?: number } = {}) {
  const state = { queries: [] as Array<Record<string, string>>, mutations: [] as Array<{ method: string; url: string; body: unknown }> };
  let record = { ...(options.row ?? settlement) };
  let listFailed = false;
  await page.route('**/api/admin/booking-settlements**', async route => {
    const request = route.request();
    const url = new URL(request.url());
    if (request.method() === 'GET') {
      state.queries.push(Object.fromEntries(url.searchParams.entries()));
      if (options.failListOnce && !listFailed) {
        listFailed = true;
        await route.fulfill({ status: 422, contentType: 'application/json', body: JSON.stringify({ message: 'Unavailable' }) });
        return;
      }
      const data = options.empty ? [] : [record];
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, message: 'ok', data, errors: null, meta: { page: 0, size: 20, totalElements: data.length, totalPages: data.length ? 1 : 0 } }) });
      return;
    }
    const body = request.postDataJSON();
    state.mutations.push({ method: request.method(), url: url.pathname, body });
    if (options.mutationStatus) {
      await route.fulfill({ status: options.mutationStatus, contentType: 'application/json', body: JSON.stringify({ message: options.mutationStatus === 409 ? 'Version conflict' : 'Unavailable' }) });
      return;
    }
    const action = url.pathname.split('/').at(-1);
    record = { ...record, status: action === 'release' ? 'RELEASED' : action === 'retain' ? 'RETAINED' : 'REOPENED' };
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, message: 'ok', data: record, errors: null, meta: null }) });
  });
  return state;
}

test('Admin settlement queue is responsive and shows a distinct booking context', async ({ page }) => {
  const baseline = process.env.BASELINE_ADMIN_SETTLEMENTS === '1';
  await seedAdmin(page);
  const state = await mockSettlements(page);
  await page.goto('/admin/booking-settlements', { waitUntil: 'domcontentloaded' });
  await expect(page.getByRole('heading', { name: 'Quyết toán buổi học' })).toBeVisible();
  await expect(page.getByText('275.000 ₫')).toBeVisible();
  await expect(page.getByText('Nguyễn Minh An')).toBeVisible();
  expect(state.queries[0]).toMatchObject({ page: '0', size: '20' });
  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    const dimensions = await page.evaluate(() => ({ body: document.body.scrollWidth, viewport: window.innerWidth }));
    if (!baseline) expect(dimensions.body).toBeLessThanOrEqual(dimensions.viewport);
    await page.screenshot({ path: resolve(evidence, `admin-booking-settlements-${baseline ? 'before' : 'after'}-${width}.png`), fullPage: true });
  }
});

test('Admin settlement status filter preserves page and size query values', async ({ page }) => {
  await seedAdmin(page);
  const state = await mockSettlements(page);
  await page.goto('/admin/booking-settlements', { waitUntil: 'domcontentloaded' });
  await page.getByLabel('Lọc trạng thái quyết toán').click();
  await page.getByText('Chờ quyết định', { exact: true }).last().click();
  await expect.poll(() => state.queries.some(query => query.status === 'AWAITING_ADMIN_DECISION' && query.page === '0' && query.size === '20')).toBeTruthy();
});

test('Admin releases settlement with required note and current version', async ({ page }) => {
  await seedAdmin(page);
  const state = await mockSettlements(page);
  await page.goto('/admin/booking-settlements', { waitUntil: 'domcontentloaded' });
  await page.getByRole('button', { name: /chi tiết|xem chi tiết/i }).click();
  await expect(page.getByText('Chưa xác nhận', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Chuyển gia sư' }).click();
  const reason = page.getByLabel('Lý do xử lý quyết toán');
  await expect(page.getByRole('button', { name: 'Chuyển gia sư' }).last()).toBeDisabled();
  await reason.fill('Học viên không phản hồi trong hạn xác nhận.');
  await page.getByRole('button', { name: 'Chuyển gia sư' }).last().click();
  await expect(page.getByRole('dialog')).toHaveCount(0);
  expect(state.mutations).toContainEqual({ method: 'POST', url: '/api/admin/booking-settlements/booking-0001-uuid/release', body: { version: 8, note: 'Học viên không phản hồi trong hạn xác nhận.' } });
});

test('Admin dispute decisions require a recorded dispute reason', async ({ page }) => {
  await seedAdmin(page);
  const row = { ...settlement, status: 'DISPUTE_PENDING', disputeReason: null };
  await mockSettlements(page, { row });
  await page.goto('/admin/booking-settlements', { waitUntil: 'domcontentloaded' });
  await page.getByRole('button', { name: /chi tiết|xem chi tiết/i }).click();
  await expect(page.getByText('Không có lý do khiếu nại được ghi nhận.', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Mở lại' })).toHaveCount(0);
});

test('Admin settlement query retries after failure and renders empty state', async ({ page }) => {
  await seedAdmin(page);
  await mockSettlements(page, { failListOnce: true });
  await page.goto('/admin/booking-settlements', { waitUntil: 'domcontentloaded' });
  await expect(page.getByText('Chưa tải được quyết toán', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByText('275.000 ₫')).toBeVisible();
  await page.unrouteAll({ behavior: 'wait' });
  await mockSettlements(page, { empty: true });
  await page.reload({ waitUntil: 'domcontentloaded' });
  await expect(page.getByText('Chưa có quyết toán buổi học.', { exact: true })).toBeVisible();
});
