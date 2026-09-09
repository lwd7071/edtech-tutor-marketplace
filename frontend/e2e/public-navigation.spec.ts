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
