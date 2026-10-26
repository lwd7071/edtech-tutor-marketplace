import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';

test('email verification states use a responsive, readable auth layout', async ({ page }) => {
  const evidenceDirectory = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidenceDirectory, { recursive: true });

  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.goto('/auth/verify-email?email=visual-check%40example.com');
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    await expect(page.getByText('Vui lòng xác minh email')).toBeVisible();
    if (variant === 'after') {
      await expect(page.locator('.tm-verify-page')).toBeVisible();
      await expect(page.getByRole('link', { name: 'tutormatch' })).toBeVisible();
      await expect(page.getByText('visual-check@example.com')).toBeVisible();
    }
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidenceDirectory, `verify-email-${variant}-${width}.png`), fullPage: true });
  }

  if (variant === 'before') return;

  await page.route('**/api/auth/resend-verification', async (route) => {
    await new Promise((resolveRequest) => setTimeout(resolveRequest, 350));
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, message: 'ok', data: null, errors: null, meta: null }) });
  });
  await page.goto('/auth/verify-email?email=visual-check%40example.com');
  await page.getByRole('button', { name: 'Gửi lại email xác minh' }).click();
  await expect(page.getByRole('button', { name: 'Gửi lại email xác minh' })).toHaveClass(/ant-btn-loading/);
  await expect(page.getByText('Đã gửi lại email xác minh thành công.')).toBeVisible();

  await page.route('**/api/auth/verify-email', (route) => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, message: 'ok', data: null, errors: null, meta: null }) }));
  await page.goto('/auth/verify-email?token=visual-check');
  await expect(page.getByText('Xác minh thành công')).toBeVisible();
  await expect(page.getByRole('link', { name: 'Tới trang Đăng nhập' })).toHaveAttribute('href', '/auth/login');

  await page.unroute('**/api/auth/verify-email');
  await page.route('**/api/auth/verify-email', (route) => route.fulfill({ status: 400, contentType: 'application/json', body: JSON.stringify({ message: 'Liên kết đã hết hạn' }) }));
  await page.goto('/auth/verify-email?token=expired-fixture');
  await expect(page.getByText('Xác minh thất bại')).toBeVisible();
  await expect(page.getByText('Liên kết đã hết hạn')).toBeVisible();

  await page.goto('/auth/verify-email');
  page.once('dialog', async (dialog) => {
    expect(dialog.message()).toContain('Vui lòng cung cấp email trong URL');
    await dialog.accept();
  });
  await page.getByRole('button', { name: 'Gửi lại email xác minh' }).click();
});
