import { expect, test } from '@playwright/test';

test('guest can navigate from homepage to tutor marketplace', async ({ page }) => {
  await page.goto('/');
  await expect(page).toHaveTitle(/Tutor Match/i);
  await page.getByRole('link', { name: /Tìm gia sư/i }).first().click();
  await expect(page).toHaveURL(/\/teachers/);
  await expect(page.getByRole('heading', { name: /gia sư/i }).first()).toBeVisible();
});

test('login page exposes password and Google entry points', async ({ page }) => {
  await page.goto('/auth/login');
  await expect(page.getByRole('heading', { name: 'Đăng nhập' })).toBeVisible();
  await expect(page.getByRole('button', { name: /Đăng nhập$/i })).toBeVisible();
  await expect(page.getByRole('link', { name: /Google/i })).toHaveAttribute('href', /oauth2\/authorization\/google/);
});

test('registration keeps the student email for the verification screen', async ({ page }) => {
  await page.route('**/api/auth/register', route => route.fulfill({
    status: 201,
    contentType: 'application/json',
    body: JSON.stringify({ success: true, data: { email: 'learner@example.test', verificationRequired: true }, errors: null, meta: null }),
  }));
  await page.goto('/auth/register');
  await page.getByPlaceholder(/Nguyễn Văn A/i).fill('Learner Test');
  await page.getByPlaceholder(/Nhập email/i).fill('learner@example.test');
  await page.getByPlaceholder(/^Nhập mật khẩu$/i).fill('Password123!');
  await page.getByRole('checkbox').check();
  await page.getByRole('button', { name: /Đăng ký/i }).click();
  await expect(page).toHaveURL(/verify-email\?email=learner%40example\.test/);
});

test('public legal and support links resolve to real pages', async ({ page }) => {
  for (const path of ['/terms', '/privacy', '/support']) {
    await page.goto(path);
    await expect(page.locator('h1')).toBeVisible();
    await expect(page.locator('body')).not.toContainText('404');
  }
});
