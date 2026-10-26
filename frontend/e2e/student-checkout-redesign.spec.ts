import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const student = { id: 'student-checkout-visual', email: 'student@example.test', fullName: 'An Nguyen', role: 'STUDENT', status: 'ACTIVE' };

test('student checkout waits for server invoice status and keeps the existing payOS route', async ({ page }) => {
  const evidenceDirectory = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidenceDirectory, { recursive: true });
  let invoiceStatus: 'PENDING' | 'PAID' | 'CANCELLED' | 'EXPIRED' = 'PENDING';
  let apiStatus = 200;
  let apiDelay = 0;
  let invoiceCalls = 0;

  await page.route('**/api/auth/login', route => route.fulfill({
    status: 200, contentType: 'application/json',
    body: JSON.stringify({ success: true, message: 'ok', data: { accessToken: 'visual-access', refreshToken: 'visual-refresh', user: student }, errors: null, meta: null }),
  }));
  await page.route('**/api/student/invoices/inv-visual', async route => {
    invoiceCalls += 1;
    if (apiDelay) await new Promise(resolveRequest => setTimeout(resolveRequest, apiDelay));
    await route.fulfill({
      status: apiStatus,
      contentType: 'application/json',
      body: JSON.stringify(apiStatus === 200
        ? { success: true, message: 'ok', data: { id: 'inv-visual', invoiceNumber: 'INV-20261016-000123', pricingPackageId: 'pkg-visual', packageName: 'Toán 11 — 10 buổi', amountVnd: 1000000, status: invoiceStatus, checkoutUrl: 'https://payos.example.test/checkout/123', qrCode: '00020101021253037045802VN5913TutorMatch6005HANOI6304ABCD', paymentExpiredAt: '2026-10-16T16:00:00Z', paidAt: invoiceStatus === 'PAID' ? '2026-10-16T15:00:00Z' : null }, errors: null, meta: null }
        : { success: false, message: 'invoice unavailable', data: null, errors: [{ code: 'NOT_FOUND' }], meta: null }),
    });
  });

  await page.setViewportSize({ width: 1280, height: 900 });
  await page.goto('/auth/login');
  await page.getByPlaceholder('Nhập email của bạn').fill(student.email);
  await page.getByPlaceholder('Nhập mật khẩu').fill('Password123!');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page).toHaveURL(/\/student$/);
  await page.goto('/student/checkout/inv-visual');
  await expect(page.getByText('INV-20261016-000123')).toBeVisible();

  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    if (variant === 'after') {
      await expect(page.locator('.tm-brand')).toHaveText('tutormatch');
      await expect(page.getByText('1.000.000 ₫')).toBeVisible();
      await expect(page.getByRole('link', { name: 'Mở trang thanh toán' })).toHaveAttribute('href', 'https://payos.example.test/checkout/123');
      await expect(page.getByRole('link', { name: 'Xem gói học của tôi' })).toHaveAttribute('href', '/student/packages');
    }
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidenceDirectory, `student-checkout-${variant}-${width}.png`), fullPage: true });
  }

  if (variant === 'before') return;

  const packageLink = page.getByRole('link', { name: 'Xem gói học của tôi' });
  await packageLink.focus();
  await expect(packageLink).toBeFocused();

  invoiceStatus = 'CANCELLED';
  await page.reload();
  await expect(page.getByText('Hóa đơn đã hủy')).toBeVisible();
  await expect(page.locator('.tm-checkout-qr, .tm-checkout-pending')).toHaveCount(0);

  invoiceStatus = 'EXPIRED';
  await page.reload();
  await expect(page.getByText('Hóa đơn hết hạn')).toBeVisible();

  invoiceStatus = 'PENDING';
  apiStatus = 503;
  await page.reload();
  await expect(page.getByText('Không tìm thấy thông tin đơn hàng thanh toán')).toBeVisible();
  apiStatus = 200;
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByRole('link', { name: 'Mở trang thanh toán' })).toBeVisible();

  apiDelay = 1300;
  const loadingReload = page.reload();
  await expect(page.getByRole('status')).toContainText('Đang tải thông tin đơn hàng');
  await loadingReload;
  apiDelay = 0;
  await expect(page).toHaveURL(/\/student\/checkout\/inv-visual$/);
  const callCountBeforePaid = invoiceCalls;
  invoiceStatus = 'PAID';
  const paidInvoiceRequest = page.waitForRequest(request =>
    request.url().includes('/api/student/invoices/inv-visual'),
    { timeout: 8000 },
  );
  await page.reload();
  await paidInvoiceRequest;
  expect(invoiceCalls).toBeGreaterThan(callCountBeforePaid);
  await expect(page).toHaveURL(/\/student\/payment-result\/inv-visual$/, { timeout: 8000 });
  await expect(page.getByText('Thanh toán thành công!')).toBeVisible();
});
