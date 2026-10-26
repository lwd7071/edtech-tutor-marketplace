import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const admin = { id: 'admin-extensions', email: 'admin@example.test', fullName: 'Quản trị viên', role: 'ADMIN', status: 'ACTIVE' };
const extension = {
  id: 'extension-001-abcd', studentPackageId: 'package-0123', studentId: 'student-09',
  reason: 'Chuẩn bị thi học kỳ, cần thêm thời gian hoàn thành buổi học.',
  requestedExpiryDate: '2026-10-28T00:00:00Z', approvedExpiryDate: null,
  status: 'PENDING', adminNote: null, reviewedBy: null, reviewedAt: null,
  createdAt: '2026-10-14T08:25:00Z',
};

async function seedAdmin(page: import('@playwright/test').Page) {
  await page.addInitScript(user => {
    localStorage.setItem('tutor-match.session', JSON.stringify({ user, remember: false, sessionId: 'admin-extensions-session' }));
    document.cookie = 'accessToken=admin-extensions-access; path=/; SameSite=Lax';
  }, admin);
}

async function mockExtensions(page: import('@playwright/test').Page, options: { empty?: boolean; failListOnce?: boolean } = {}) {
  const state = { queries: [] as Array<Record<string, string>>, mutations: [] as Array<{ method: string; url: string; body: unknown; idempotencyKey: string | null }> };
  let record = { ...extension, status: 'PENDING' as 'PENDING' | 'APPROVED' | 'REJECTED', approvedExpiryDate: null as string | null, adminNote: null as string | null };
  let listFailed = false;
  await page.route('**/api/admin/extension-requests**', async route => {
    const request = route.request();
    const url = new URL(request.url());
    if (request.method() === 'GET') {
      state.queries.push(Object.fromEntries(url.searchParams.entries()));
      if (options.failListOnce && !listFailed) {
        listFailed = true;
        await route.fulfill({ status: 422, contentType: 'application/json', body: JSON.stringify({ message: 'Unavailable' }) });
        return;
      }
      const visible = url.searchParams.get('status') === 'APPROVED' && record.status === 'PENDING' ? { ...record, status: 'APPROVED' as const } : record;
      const data = options.empty ? [] : [visible];
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, message: 'ok', data, errors: null, meta: { page: 0, size: 20, totalElements: data.length, totalPages: data.length ? 1 : 0 } }) });
      return;
    }
    const body = request.postDataJSON();
    state.mutations.push({ method: request.method(), url: url.pathname, body, idempotencyKey: request.headers()['idempotency-key'] ?? null });
    if (url.pathname.endsWith('/approve')) record = { ...record, status: 'APPROVED', approvedExpiryDate: (body as { approvedExpiryDate: string }).approvedExpiryDate, adminNote: (body as { adminNote?: string }).adminNote ?? null };
    if (url.pathname.endsWith('/reject')) record = { ...record, status: 'REJECTED' };
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, message: 'ok', data: record, errors: null, meta: null }) });
  });
  return state;
}

test('Admin extension queue is responsive and uses fields from the source DTO', async ({ page }) => {
  const baseline = process.env.BASELINE_ADMIN_EXTENSIONS === '1';
  await seedAdmin(page);
  const state = await mockExtensions(page);
  await page.goto('/admin/extensions', { waitUntil: 'domcontentloaded' });
  await expect(page.getByRole('heading', { name: baseline ? 'Hàng đợi Gia hạn Gói học' : 'Yêu cầu gia hạn' })).toBeVisible();
  await expect(page.getByText('Chuẩn bị thi học kỳ, cần thêm thời gian hoàn thành buổi học.', { exact: true }).first()).toBeVisible();
  expect(state.queries[0]).toMatchObject({ page: '0', size: '20' });
  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    if (!baseline) {
      const dimensions = await page.evaluate(() => ({ body: document.body.scrollWidth, viewport: window.innerWidth }));
      expect(dimensions.body).toBeLessThanOrEqual(dimensions.viewport);
      const mobileFilter = page.getByLabel('Lọc trạng thái gia hạn');
      if (width <= 680) await expect(mobileFilter).toBeVisible();
      else await expect(mobileFilter).toBeHidden();
    }
    await page.screenshot({ path: resolve(evidence, `admin-extensions-${baseline ? 'before' : 'after'}-${width}.png`), fullPage: true });
  }
});

test('Admin extension status tabs retain the query contract', async ({ page }) => {
  await seedAdmin(page);
  const state = await mockExtensions(page);
  await page.goto('/admin/extensions', { waitUntil: 'domcontentloaded' });
  await page.getByRole('tab', { name: 'Đã gia hạn' }).click();
  await expect.poll(() => state.queries.some(query => query.status === 'APPROVED' && query.page === '0' && query.size === '20')).toBeTruthy();
});

test('Admin approves an extension with selected expiry and optional note', async ({ page }) => {
  await seedAdmin(page);
  const state = await mockExtensions(page);
  await page.goto('/admin/extensions', { waitUntil: 'domcontentloaded' });
  await page.getByRole('button', { name: 'Chi tiết' }).click();
  await expect(page.getByRole('dialog').getByText('Chuẩn bị thi học kỳ, cần thêm thời gian hoàn thành buổi học.', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Duyệt' }).last().click();
  await page.getByLabel('Ghi chú admin').fill('Đồng ý theo lịch học.');
  await page.getByRole('button', { name: 'Xác nhận gia hạn' }).click();
  await expect(page.getByRole('dialog')).toHaveCount(0);
  expect(state.mutations).toContainEqual(expect.objectContaining({ method: 'POST', url: '/api/admin/extension-requests/extension-001-abcd/approve', body: { approvedExpiryDate: '2026-10-28T00:00:00.000Z', adminNote: 'Đồng ý theo lịch học.' }, idempotencyKey: expect.any(String) }));
});

test('Admin rejects an extension only after entering a reason', async ({ page }) => {
  await seedAdmin(page);
  const state = await mockExtensions(page);
  await page.goto('/admin/extensions', { waitUntil: 'domcontentloaded' });
  await page.getByRole('button', { name: 'Từ chối' }).click();
  await page.getByRole('button', { name: 'Xác nhận từ chối' }).click();
  await expect(page.getByText('Vui lòng nhập lý do từ chối')).toBeVisible();
  await page.getByLabel('Lý do từ chối (bắt buộc)').fill('Gói đã quá hạn theo chính sách.');
  await page.getByRole('button', { name: 'Xác nhận từ chối' }).click();
  await expect(page.getByRole('dialog')).toHaveCount(0);
  expect(state.mutations).toContainEqual(expect.objectContaining({ method: 'POST', url: '/api/admin/extension-requests/extension-001-abcd/reject', body: { reason: 'Gói đã quá hạn theo chính sách.' }, idempotencyKey: expect.any(String) }));
});

test('Admin extension query recovers from error and renders empty state', async ({ page }) => {
  await seedAdmin(page);
  await mockExtensions(page, { failListOnce: true });
  await page.goto('/admin/extensions', { waitUntil: 'domcontentloaded' });
  await expect(page.getByText('Không tải được yêu cầu gia hạn.', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByText('Chuẩn bị thi học kỳ, cần thêm thời gian hoàn thành buổi học.', { exact: true }).first()).toBeVisible();
  await page.unrouteAll({ behavior: 'wait' });
  await mockExtensions(page, { empty: true });
  await page.reload({ waitUntil: 'domcontentloaded' });
  await expect(page.getByText('Chưa có yêu cầu gia hạn trong danh sách này.')).toBeVisible();
});
