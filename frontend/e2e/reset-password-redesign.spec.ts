import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';

test('reset password flow is responsive and keeps token recovery behavior', async ({ page }) => {
  const evidenceDirectory = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidenceDirectory, { recursive: true });

  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.goto('/auth/reset-password?token=visual-fixture');
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    await expect(page.getByRole('heading', { name: 'Đặt lại mật khẩu' })).toBeVisible();
    await expect(page.getByPlaceholder('Nhập mật khẩu mới')).toBeVisible();
    await expect(page.getByPlaceholder('Nhập lại mật khẩu mới')).toBeVisible();
    if (variant === 'after') {
      await expect(page.locator('.tm-reset-page')).toBeVisible();
      await expect(page.getByRole('link', { name: 'tutormatch' })).toBeVisible();
    }
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidenceDirectory, `reset-password-${variant}-${width}.png`), fullPage: true });
  }

  if (variant === 'before') return;

  await page.getByPlaceholder('Nhập mật khẩu mới').fill('StrongPass123!');
  await page.getByPlaceholder('Nhập lại mật khẩu mới').fill('DifferentPass123!');
  await page.getByRole('button', { name: 'Xác nhận đặt lại mật khẩu' }).click();
  await expect(page.getByText('Mật khẩu xác nhận không khớp')).toBeVisible();

  await page.route('**/api/auth/reset-password', async (route) => {
    await new Promise((resolveRequest) => setTimeout(resolveRequest, 350));
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, message: 'ok', data: null, errors: null, meta: null }) });
  });
  await page.getByPlaceholder('Nhập lại mật khẩu mới').fill('StrongPass123!');
  await page.getByRole('button', { name: 'Xác nhận đặt lại mật khẩu' }).click();
  await expect(page.locator('.tm-reset-submit')).toHaveClass(/ant-btn-loading/);
  await expect(page).toHaveURL(/\/auth\/login\?reset=success$/);

  await page.goto('/auth/reset-password');
  await expect(page.getByText('Liên kết không hợp lệ hoặc đã hết hạn')).toBeVisible();
  await page.getByRole('button', { name: 'Gửi lại liên kết khôi phục' }).click();
  await expect(page).toHaveURL(/\/auth\/forgot-password$/);

  await page.route('**/api/auth/reset-password', (route) => route.fulfill({ status: 400, contentType: 'application/json', body: JSON.stringify({ message: 'Token đã hết hạn' }) }));
  await page.goto('/auth/reset-password?token=expired-fixture');
  await page.getByPlaceholder('Nhập mật khẩu mới').fill('StrongPass123!');
  await page.getByPlaceholder('Nhập lại mật khẩu mới').fill('StrongPass123!');
  await page.getByRole('button', { name: 'Xác nhận đặt lại mật khẩu' }).click();
  await expect(page.getByText('Liên kết không hợp lệ hoặc đã hết hạn')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Gửi lại liên kết khôi phục' })).toBeVisible();

  await page.route('**/api/auth/reset-password', (route) => route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ message: 'Không thể hoàn tất lúc này' }) }));
  await page.goto('/auth/reset-password?token=retry-fixture');
  await page.getByPlaceholder('Nhập mật khẩu mới').fill('StrongPass123!');
  await page.getByPlaceholder('Nhập lại mật khẩu mới').fill('StrongPass123!');
  await page.getByRole('button', { name: 'Xác nhận đặt lại mật khẩu' }).click();
  await expect(page.locator('.tm-reset-error')).toContainText('Không thể hoàn tất lúc này');
  await expect(page.getByPlaceholder('Nhập mật khẩu mới')).toBeVisible();
});
