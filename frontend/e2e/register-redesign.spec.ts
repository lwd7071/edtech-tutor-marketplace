import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';

test('registration form presents both account roles and responsive validation', async ({ page }) => {
  const evidenceDirectory = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidenceDirectory, { recursive: true });

  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.goto('/auth/register');
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    await expect(page.getByRole('heading', { name: 'Đăng ký tài khoản' })).toBeVisible();
    await expect(page.getByPlaceholder('Ví dụ: Nguyễn Văn A')).toBeVisible();
    await expect(page.getByPlaceholder('Nhập email của bạn')).toBeVisible();
    await expect(page.getByPlaceholder('Nhập mật khẩu')).toBeVisible();
    await expect(page.getByRole('link', { name: 'Đăng ký bằng Google' })).toHaveAttribute('href', /oauth2\/authorization\/google$/);
    if (variant === 'after') {
      await expect(page.locator('.tm-register-page')).toBeVisible();
      expect(await page.locator('.tm-register-role-option').count()).toBe(2);
      await expect(page.getByRole('link', { name: 'tutormatch' })).toBeVisible();
      await expect(page.getByText(/phụ huynh/i)).toHaveCount(1);
      await expect(page.getByLabel(/parent|phụ huynh/i)).toHaveCount(0);
    }
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidenceDirectory, `register-${variant}-${width}.png`), fullPage: true });
  }

  if (variant === 'before') return;

  await page.goto('/auth/register?role=TEACHER');
  await expect(page.locator('.tm-register-role-option.checked')).toContainText('Gia sư');
  await page.getByRole('button', { name: 'Đăng ký', exact: true }).click();
  await expect(page.getByText('Họ và tên không được để trống')).toBeVisible();
  await expect(page.getByText('Email không được để trống')).toBeVisible();
  await expect(page.getByText('Mật khẩu phải dài ít nhất 8 ký tự')).toBeVisible();
  await expect(page.getByText('Vui lòng đồng ý với điều khoản sử dụng')).toBeVisible();

  await page.route('**/api/auth/register', async (route) => {
    await new Promise((resolveRequest) => setTimeout(resolveRequest, 400));
    await route.fulfill({ status: 409, contentType: 'application/json', body: JSON.stringify({ message: 'Email đã được sử dụng' }) });
  });
  await page.goto('/auth/register');
  await page.getByPlaceholder('Ví dụ: Nguyễn Văn A').fill('Test User');
  await page.getByPlaceholder('Nhập email của bạn').fill('used@example.com');
  await page.getByPlaceholder('Nhập mật khẩu').fill('StrongPass123!');
  await page.getByRole('checkbox').check();
  await page.getByRole('button', { name: 'Đăng ký', exact: true }).click();
  await expect(page.locator('.tm-register-submit')).toHaveClass(/ant-btn-loading/);
  await expect(page.locator('.tm-register-error')).toContainText('Email đã được sử dụng');
});
