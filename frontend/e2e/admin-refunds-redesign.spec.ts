import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const admin = { id: 'admin-refunds', email: 'admin@example.test', fullName: 'Quản trị viên', role: 'ADMIN', status: 'ACTIVE' };
const refund = {
  id: 'refund-001-abcd', studentPackageId: 'package-0123', studentId: 'student-09',
  reason: 'Gia đình chuyển chỗ ở, không thể tiếp tục lịch học.', requestedSessions: 3,
  approvedSessions: null, refundAmountVnd: 420000, status: 'PENDING', adminNote: null,
  bankName: 'Ngân hàng Á Châu', bankBin: '970416', accountNumberMasked: '•••• 1234',
  accountHolderName: 'NGUYEN MINH AN', bankReference: null, proofUrl: null,
  processedBy: null, processedAt: null, version: 6, createdAt: '2026-10-14T08:25:00Z',
};

async function seedAdmin(page: import('@playwright/test').Page) {
  await page.addInitScript(user => {
    localStorage.setItem('tutor-match.session', JSON.stringify({ user, remember: false, sessionId: 'admin-refunds-session' }));
    document.cookie = 'accessToken=admin-refunds-access; path=/; SameSite=Lax';
  }, admin);
}

async function mockRefunds(page: import('@playwright/test').Page, options: { empty?: boolean; failListOnce?: boolean } = {}) {
  const state = { queries: [] as Array<Record<string, string>>, mutations: [] as Array<{ method: string; url: string; body: unknown; idempotencyKey: string | null }> };
  let record = { ...refund, status: 'PENDING' as 'PENDING' | 'APPROVED' | 'REFUNDED' | 'REJECTED', approvedSessions: null as number | null, refundAmountVnd: 420000, bankReference: null as string | null, proofUrl: null as string | null };
  let listFailed = false;
  await page.route('**/api/admin/refund-requests**', async route => {
    const request = route.request();
    const url = new URL(request.url());
    if (request.method() === 'GET') {
      state.queries.push(Object.fromEntries(url.searchParams.entries()));
      if (options.failListOnce && !listFailed) {
        listFailed = true;
        await route.fulfill({ status: 422, contentType: 'application/json', body: JSON.stringify({ message: 'Unavailable' }) });
        return;
      }
      const currentRecord = url.searchParams.get('status') === 'APPROVED' && record.status === 'PENDING' ? { ...record, status: 'APPROVED' as const } : record;
      const data = options.empty ? [] : [currentRecord];
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, message: 'ok', data, errors: null, meta: { page: 0, size: 20, totalElements: data.length, totalPages: data.length ? 1 : 0 } }) });
      return;
    }
    const raw = request.postDataBuffer()?.toString('utf8') ?? '';
    const idempotencyKey = request.headers()['idempotency-key'] ?? null;
    let body: unknown = raw;
    if (!request.headers()['content-type']?.includes('multipart/form-data')) body = request.postDataJSON();
    state.mutations.push({ method: request.method(), url: url.pathname, body, idempotencyKey });
    if (url.pathname.endsWith('/approve')) record = { ...record, status: 'APPROVED', approvedSessions: (body as { approvedSessions: number }).approvedSessions };
    if (url.pathname.endsWith('/reject')) record = { ...record, status: 'REJECTED' };
    if (url.pathname.endsWith('/complete')) record = { ...record, status: 'REFUNDED', bankReference: raw.includes('REFUND-TRANSFER-009') ? 'REFUND-TRANSFER-009' : null, proofUrl: '/private/proof' };
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, message: 'ok', data: record, errors: null, meta: null }) });
  });
  return state;
}

test('Admin refund queue is responsive and shows only source DTO data', async ({ page }) => {
  const baseline = process.env.BASELINE_ADMIN_REFUNDS === '1';
  await seedAdmin(page);
  const state = await mockRefunds(page);
  await page.goto('/admin/refunds', { waitUntil: 'domcontentloaded' });
  await expect(page.getByRole('heading', { name: baseline ? 'Hàng đợi Hoàn tiền Học viên' : 'Yêu cầu hoàn tiền' })).toBeVisible();
  await expect(page.getByText('420.000 ₫')).toBeVisible();
  await expect(page.getByText('•••• 1234')).toBeVisible();
  expect(state.queries[0]).toMatchObject({ page: '0', size: '20' });
  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    if (!baseline) {
      const dimensions = await page.evaluate(() => ({ body: document.body.scrollWidth, viewport: window.innerWidth }));
      expect(dimensions.body).toBeLessThanOrEqual(dimensions.viewport);
      const mobileFilter = page.getByLabel('Lọc trạng thái hoàn tiền');
      if (width <= 680) await expect(mobileFilter).toBeVisible();
      else await expect(mobileFilter).toBeHidden();
    }
    await page.screenshot({ path: resolve(evidence, `admin-refunds-${baseline ? 'before' : 'after'}-${width}.png`), fullPage: true });
  }
});

test('Admin refund status tabs keep the query contract', async ({ page }) => {
  await seedAdmin(page);
  const state = await mockRefunds(page);
  await page.goto('/admin/refunds', { waitUntil: 'domcontentloaded' });
  await page.getByRole('tab', { name: 'Đã duyệt' }).click();
  await expect.poll(() => state.queries.some(query => query.status === 'APPROVED' && query.page === '0' && query.size === '20')).toBeTruthy();
});

test('Admin approves a refund with session count and version', async ({ page }) => {
  await seedAdmin(page);
  const state = await mockRefunds(page);
  await page.goto('/admin/refunds', { waitUntil: 'domcontentloaded' });
  await page.getByRole('button', { name: 'Duyệt' }).first().click();
  await page.getByRole('button', { name: 'Duyệt' }).last().click();
  await expect(page.getByRole('dialog', { name: /Duyệt yêu cầu hoàn tiền/ })).toBeVisible();
  await page.getByLabel('Số buổi duyệt hoàn').fill('2');
  await page.getByLabel('Ghi chú admin').fill('Xác nhận theo chính sách.');
  await page.getByRole('button', { name: 'Duyệt' }).last().click();
  await expect(page.getByRole('dialog')).toHaveCount(0);
  expect(state.mutations).toContainEqual(expect.objectContaining({ method: 'POST', url: '/api/admin/refund-requests/refund-001-abcd/approve', body: { approvedSessions: 2, adminNote: 'Xác nhận theo chính sách.', version: 6 }, idempotencyKey: expect.any(String) }));
});

test('Admin rejects a refund with required reason and version', async ({ page }) => {
  await seedAdmin(page);
  const state = await mockRefunds(page);
  await page.goto('/admin/refunds', { waitUntil: 'domcontentloaded' });
  await page.getByRole('button', { name: 'Từ chối' }).click();
  await page.getByLabel('Lý do từ chối (bắt buộc)').fill('Yêu cầu ngoài thời hạn.');
  await page.getByRole('button', { name: 'Xác nhận từ chối' }).click();
  await expect(page.getByRole('dialog')).toHaveCount(0);
  expect(state.mutations).toContainEqual(expect.objectContaining({ method: 'POST', url: '/api/admin/refund-requests/refund-001-abcd/reject', body: { reason: 'Yêu cầu ngoài thời hạn.', version: 6 }, idempotencyKey: expect.any(String) }));
});

test('Admin records refund transfer metadata, proof, and idempotency key', async ({ page }) => {
  await seedAdmin(page);
  const state = await mockRefunds(page);
  await page.goto('/admin/refunds', { waitUntil: 'domcontentloaded' });
  await page.getByRole('tab', { name: 'Đã duyệt' }).click();
  await page.getByRole('button', { name: 'Xác nhận hoàn' }).click();
  await page.getByLabel('Mã giao dịch ngân hàng').fill('REFUND-TRANSFER-009');
  const transferTime = page.getByLabel('Thời điểm chuyển khoản');
  await transferTime.fill('16/10/2026 10:30');
  await transferTime.press('Enter');
  await page.locator('input[type="file"]').setInputFiles({ name: 'transfer-proof.pdf', mimeType: 'application/pdf', buffer: Buffer.from('%PDF-1.4 transfer fixture') });
  await page.getByRole('button', { name: 'Xác nhận hoàn tất' }).click();
  await expect(page.getByRole('dialog')).toHaveCount(0);
  const call = state.mutations.find(item => item.url.endsWith('/complete'));
  expect(call?.method).toBe('POST');
  expect(call?.idempotencyKey).toBeTruthy();
  expect(String(call?.body)).toContain('REFUND-TRANSFER-009');
  expect(String(call?.body)).toContain('"version":6');
  expect(String(call?.body)).toContain('"transferredAt":"2026-10-16T03:30:00.000Z"');
  expect(String(call?.body)).toContain('transfer-proof.pdf');
});

test('Admin refund query recovers from error and renders empty state', async ({ page }) => {
  await seedAdmin(page);
  await mockRefunds(page, { failListOnce: true });
  await page.goto('/admin/refunds', { waitUntil: 'domcontentloaded' });
  await expect(page.getByText('Không tải được yêu cầu hoàn tiền.')).toBeVisible();
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByText('420.000 ₫')).toBeVisible();
  await page.unrouteAll({ behavior: 'wait' });
  await mockRefunds(page, { empty: true });
  await page.reload({ waitUntil: 'domcontentloaded' });
  await expect(page.getByText('Chưa có yêu cầu hoàn tiền trong danh sách này.')).toBeVisible();
});
