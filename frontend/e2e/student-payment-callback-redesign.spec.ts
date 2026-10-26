import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const student = { id: 'student-payment-callback-visual', email: 'student@example.test', fullName: 'An Nguyen', role: 'STUDENT', status: 'ACTIVE' };

test('student payment callback displays invoice status from the server and keeps polling', async ({ page }) => {
  const evidenceDirectory = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidenceDirectory, { recursive: true });
  let invoiceStatus: 'PENDING' | 'PAID' | 'CANCELLED' | 'EXPIRED' = 'PENDING';
  let apiStatus = 200;
  let apiDelay = 0;
  let invoiceCalls = 0;

  await page.route('**/api/auth/login', route => route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({ success: true, message: 'ok', data: { accessToken: 'visual-access', refreshToken: 'visual-refresh', user: student }, errors: null, meta: null }),
  }));
  await page.route('**/api/student/invoices/inv-visual', async route => {
    invoiceCalls += 1;
    if (apiDelay) await new Promise(resolveRequest => setTimeout(resolveRequest, apiDelay));
    const data = { id: 'inv-visual', invoiceNumber: 'INV-20261016-000123', pricingPackageId: 'pkg-visual', packageName: 'Toán 11 — 10 buổi', amountVnd: 1000000, status: invoiceStatus, checkoutUrl: 'https://payos.example.test/checkout/123', qrCode: '00020101021253037045802VN5913TutorMatch6005HANOI6304ABCD', paymentExpiredAt: '2026-10-16T16:00:00Z', paidAt: invoiceStatus === 'PAID' ? '2026-10-16T15:00:00Z' : null };
    await route.fulfill({
      status: apiStatus,
      contentType: 'application/json',
      body: JSON.stringify(apiStatus === 200
        ? { success: true, message: 'ok', data, errors: null, meta: null }
        : { success: false, message: 'invoice unavailable', data: null, errors: [{ code: 'NOT_FOUND' }], meta: null }),
    });
  });

  await page.setViewportSize({ width: 1280, height: 900 });
  await page.goto('/auth/login');
  await page.getByPlaceholder('Nhập email của bạn').fill(student.email);
  await page.getByPlaceholder('Nhập mật khẩu').fill('Password123!');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page).toHaveURL(/\/student$/);
  await page.goto('/student/payments/callback?invoiceId=inv-visual');

  if (variant === 'before') {
    await expect(page.getByText('Đang đối chiếu thanh toán')).toBeVisible();
  } else {
    await expect(page.getByRole('heading', { name: 'Đang đối chiếu thanh toán' })).toBeVisible();
  }

  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    if (variant === 'after') {
      await expect(page.locator('.tm-brand')).toHaveText('tutormatch');
      await expect(page.getByRole('link', { name: 'Xem gói học' })).toHaveAttribute('href', '/student/packages');
      await expect(page.getByRole('button', { name: 'Kiểm tra lại' })).toBeVisible();
    }
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidenceDirectory, `student-payment-callback-${variant}-${width}.png`), fullPage: true });
  }
  if (variant === 'before') return;

  const retryButton = page.getByRole('button', { name: 'Kiểm tra lại' });
  await retryButton.focus();
  await expect(retryButton).toBeFocused();

  const paidResponse = page.waitForResponse(async response => {
    if (!response.url().includes('/api/student/invoices/inv-visual') || response.status() !== 200) return false;
    return (await response.json()).data?.status === 'PAID';
  }, { timeout: 7000 });
  invoiceStatus = 'PAID';
  await paidResponse;
  await expect(page.getByRole('heading', { name: 'Thanh toán đã được xác nhận' })).toBeVisible();

  invoiceStatus = 'EXPIRED';
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Hóa đơn đã hết hạn' })).toBeVisible();
  invoiceStatus = 'CANCELLED';
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Hóa đơn đã hủy' })).toBeVisible();

  const callsBeforeMissingId = invoiceCalls;
  await page.goto('/student/payments/callback?status=PAID');
  await expect(page.getByRole('heading', { name: 'Chưa có mã hóa đơn để đối chiếu' })).toBeVisible();
  await expect(page.getByText('Thanh toán đã được xác nhận')).toHaveCount(0);
  expect(invoiceCalls).toBe(callsBeforeMissingId);

  await page.goto('/student/payments/callback?invoiceId=inv-visual');
  invoiceStatus = 'PENDING';
  apiStatus = 503;
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Chưa đọc được trạng thái thanh toán' })).toBeVisible();
  apiStatus = 200;
  await retryButton.click();
  await expect(page.getByRole('heading', { name: 'Đang đối chiếu thanh toán' })).toBeVisible();

  apiDelay = 1200;
  const loadingReload = page.reload();
  await expect(page.getByRole('status')).toContainText('Đang kiểm tra hóa đơn');
  await loadingReload;
  expect(invoiceCalls).toBeGreaterThan(callsBeforeMissingId);
});
