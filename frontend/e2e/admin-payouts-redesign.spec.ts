import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const admin = { id: 'admin-payouts', email: 'admin@example.test', fullName: 'Quản trị viên', role: 'ADMIN', status: 'ACTIVE' };
const payout = {
  id: 'payout-001-abcd', teacherId: 'teacher-08', walletId: 'wallet-31', bankAccountId: 'bank-92',
  amountVnd: 1850000, status: 'PENDING', teacherNote: 'Rút tiền sau đợt dạy tháng này.', adminNote: null,
  bankReference: null, proofUrl: null, transferredAt: null, processedBy: null, processedAt: null,
  version: 6, createdAt: '2026-10-14T08:25:00Z',
};

async function seedAdmin(page: import('@playwright/test').Page) {
  await page.addInitScript(user => {
    localStorage.setItem('tutor-match.session', JSON.stringify({ user, remember: false, sessionId: 'admin-payouts-session' }));
    document.cookie = 'accessToken=admin-payouts-access; path=/; SameSite=Lax';
  }, admin);
}

async function mockPayouts(page: import('@playwright/test').Page, options: { empty?: boolean; failListOnce?: boolean; initialStatus?: 'PENDING' | 'PROCESSING' } = {}) {
  const state = { queries: [] as Array<Record<string, string>>, mutations: [] as Array<{ method: string; url: string; body: unknown; idempotencyKey: string | null }> };
  let record: Omit<typeof payout, 'status' | 'bankReference' | 'proofUrl' | 'transferredAt'> & { status: string; bankReference: string | null; proofUrl: string | null; transferredAt: string | null } = {
    ...payout, status: options.initialStatus ?? payout.status, bankReference: null, proofUrl: null, transferredAt: null,
  };
  let listFailed = false;
  await page.route('**/api/admin/payout-requests**', async route => {
    const request = route.request();
    const url = new URL(request.url());
    if (request.method() === 'GET') {
      state.queries.push(Object.fromEntries(url.searchParams.entries()));
      if (options.failListOnce && !listFailed) {
        listFailed = true;
        await route.fulfill({ status: 422, contentType: 'application/json', body: JSON.stringify({ message: 'Unavailable' }) });
        return;
      }
      const requestedStatus = url.searchParams.get('status');
      const visible = requestedStatus && requestedStatus !== record.status ? { ...record, status: requestedStatus as typeof record.status } : record;
      const data = options.empty ? [] : [visible];
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, message: 'ok', data, errors: null, meta: { page: 0, size: 20, totalElements: data.length, totalPages: data.length ? 1 : 0 } }) });
      return;
    }
    const raw = request.postDataBuffer()?.toString('utf8') ?? '';
    const multipart = request.headers()['content-type']?.includes('multipart/form-data') ?? false;
    const body = multipart ? raw : request.postDataJSON();
    state.mutations.push({ method: request.method(), url: url.pathname, body, idempotencyKey: request.headers()['idempotency-key'] ?? null });
    if (url.pathname.endsWith('/process')) record = { ...record, status: 'PROCESSING' };
    if (url.pathname.endsWith('/reject')) record = { ...record, status: 'REJECTED' };
    if (url.pathname.endsWith('/complete')) record = { ...record, status: 'SUCCEEDED', bankReference: raw.includes('PAYOUT-TRANSFER-004') ? 'PAYOUT-TRANSFER-004' : null, proofUrl: '/private/payout-proof' };
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, message: 'ok', data: record, errors: null, meta: null }) });
  });
  return state;
}

test('Admin payout queue is responsive and displays no invented bank details', async ({ page }) => {
  const baseline = process.env.BASELINE_ADMIN_PAYOUTS === '1';
  await seedAdmin(page);
  const state = await mockPayouts(page);
  await page.goto('/admin/payouts', { waitUntil: 'domcontentloaded' });
  await expect(page.getByRole('heading', { name: baseline ? 'Yêu cầu rút tiền của gia sư' : 'Yêu cầu rút tiền' })).toBeVisible();
  await expect(page.getByText('1.850.000 ₫')).toBeVisible();
  if (!baseline) await expect(page.getByText('teacher-08')).toBeVisible();
  expect(state.queries[0]).toMatchObject({ page: '0', size: '20' });
  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    if (!baseline) {
      const dimensions = await page.evaluate(() => ({ body: document.body.scrollWidth, viewport: window.innerWidth }));
      expect(dimensions.body).toBeLessThanOrEqual(dimensions.viewport);
      const mobileFilter = page.getByLabel('Lọc trạng thái rút tiền');
      if (width <= 680) await expect(mobileFilter).toBeVisible();
      else await expect(mobileFilter).toBeHidden();
    }
    await page.screenshot({ path: resolve(evidence, `admin-payouts-${baseline ? 'before' : 'after'}-${width}.png`), fullPage: true });
  }
});

test('Admin payout status tabs keep the query parameters', async ({ page }) => {
  await seedAdmin(page);
  const state = await mockPayouts(page);
  await page.goto('/admin/payouts', { waitUntil: 'domcontentloaded' });
  await page.getByRole('tab', { name: 'Đang xử lý' }).click();
  await expect.poll(() => state.queries.some(query => query.status === 'PROCESSING' && query.page === '0' && query.size === '20')).toBeTruthy();
});

test('Admin moves a pending payout to processing with its version', async ({ page }) => {
  await seedAdmin(page);
  const state = await mockPayouts(page);
  await page.goto('/admin/payouts', { waitUntil: 'domcontentloaded' });
  await page.getByRole('button', { name: 'Xử lý' }).first().click();
  await page.getByRole('button', { name: 'Xử lý' }).last().click();
  await expect.poll(() => state.mutations.some(call => call.url.endsWith('/process'))).toBeTruthy();
  expect(state.mutations).toContainEqual(expect.objectContaining({ method: 'POST', url: '/api/admin/payout-requests/payout-001-abcd/process', body: { version: 6 }, idempotencyKey: expect.any(String) }));
});

test('Admin completes a processing payout with transfer metadata, proof, and version', async ({ page }) => {
  await seedAdmin(page);
  const state = await mockPayouts(page, { initialStatus: 'PROCESSING' });
  await page.goto('/admin/payouts', { waitUntil: 'domcontentloaded' });
  await page.getByRole('button', { name: 'Xác nhận chuyển' }).click();
  await page.getByLabel('Mã giao dịch ngân hàng').fill('PAYOUT-TRANSFER-004');
  const transferTime = page.getByLabel('Thời điểm chuyển khoản');
  await transferTime.fill('16/10/2026 10:30');
  await transferTime.press('Enter');
  await page.locator('input[type="file"]').setInputFiles({ name: 'payout-proof.pdf', mimeType: 'application/pdf', buffer: Buffer.from('%PDF-1.4 transfer fixture') });
  await page.getByRole('button', { name: 'Xác nhận thành công' }).click();
  await expect(page.getByRole('dialog')).toHaveCount(0);
  const call = state.mutations.find(item => item.url.endsWith('/complete'));
  expect(call?.method).toBe('POST');
  expect(call?.idempotencyKey).toBeTruthy();
  expect(String(call?.body)).toContain('PAYOUT-TRANSFER-004');
  expect(String(call?.body)).toContain('"transferredAt":"2026-10-16T03:30:00.000Z"');
  expect(String(call?.body)).toContain('"version":6');
  expect(String(call?.body)).toContain('payout-proof.pdf');
});

test('Admin payout reject requires a reason and sends the current version', async ({ page }) => {
  await seedAdmin(page);
  const state = await mockPayouts(page, { initialStatus: 'PROCESSING' });
  await page.goto('/admin/payouts', { waitUntil: 'domcontentloaded' });
  await page.getByRole('button', { name: 'Từ chối' }).click();
  await page.getByLabel('Lý do từ chối (bắt buộc)').fill('Thông tin nhận tiền không xác minh được.');
  await page.getByRole('button', { name: 'Xác nhận từ chối' }).click();
  await expect(page.getByRole('dialog')).toHaveCount(0);
  expect(state.mutations).toContainEqual(expect.objectContaining({ method: 'POST', url: '/api/admin/payout-requests/payout-001-abcd/reject', body: { reason: 'Thông tin nhận tiền không xác minh được.', version: 6 }, idempotencyKey: expect.any(String) }));
});

test('Admin payout query retries after failure and shows empty state', async ({ page }) => {
  await seedAdmin(page);
  await mockPayouts(page, { failListOnce: true });
  await page.goto('/admin/payouts', { waitUntil: 'domcontentloaded' });
  await expect(page.getByText('Không tải được yêu cầu rút tiền.', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByText('1.850.000 ₫')).toBeVisible();
  await page.unrouteAll({ behavior: 'wait' });
  await mockPayouts(page, { empty: true });
  await page.reload({ waitUntil: 'domcontentloaded' });
  await expect(page.getByText('Chưa có yêu cầu rút tiền trong danh sách này.')).toBeVisible();
});
