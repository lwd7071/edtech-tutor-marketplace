import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';

test('forgot password remains private and readable across viewports', async ({ page }) => {
  const evidenceDirectory = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidenceDirectory, { recursive: true });

  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.goto('/auth/forgot-password');
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    await expect(page.getByText('Quên mật khẩu?')).toBeVisible();
    await expect(page.getByPlaceholder('Nhập email của bạn')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Gửi liên kết khôi phục' })).toBeVisible();
    if (variant === 'after') {
      await expect(page.locator('.tm-forgot-page')).toBeVisible();
      await expect(page.getByRole('link', { name: 'tutormatch' })).toBeVisible();
    }
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidenceDirectory, `forgot-password-${variant}-${width}.png`), fullPage: true });
  }

  if (variant === 'before') return;

  await page.getByRole('button', { name: 'Gửi liên kết khôi phục' }).click();
  await expect(page.getByText('Email không được để trống')).toBeVisible();
  await page.getByPlaceholder('Nhập email của bạn').fill('private@example.com');
  await page.route('**/api/auth/forgot-password', async (route) => {
    await new Promise((resolveRequest) => setTimeout(resolveRequest, 350));
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, message: 'ok', data: null, errors: null, meta: null }) });
  });
  await page.getByRole('button', { name: 'Gửi liên kết khôi phục' }).click();
  await expect(page.locator('.tm-forgot-submit')).toHaveClass(/ant-btn-loading/);
  await expect(page.getByText('Đã gửi email khôi phục')).toBeVisible();
  await expect(page.getByText(/Nếu email này có trong hệ thống/)).toBeVisible();
  await expect(page.getByText('private@example.com')).toHaveCount(0);
  await expect(page.getByRole('link', { name: 'Quay lại đăng nhập' })).toHaveAttribute('href', '/auth/login');

  await page.route('**/api/auth/forgot-password', (route) => route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ message: 'Dịch vụ tạm thời gián đoạn' }) }));
  await page.goto('/auth/forgot-password');
  await page.getByPlaceholder('Nhập email của bạn').fill('retry@example.com');
  await page.getByRole('button', { name: 'Gửi liên kết khôi phục' }).click();
  await expect(page.locator('.tm-forgot-error')).toContainText('Dịch vụ tạm thời gián đoạn');
  await expect(page.getByPlaceholder('Nhập email của bạn')).toBeVisible();
});
