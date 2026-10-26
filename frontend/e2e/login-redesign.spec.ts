import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';

test('login layout matches approved auth direction across viewports', async ({ page }) => {
  const evidenceDirectory = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidenceDirectory, { recursive: true });

  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.goto('/auth/login');
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    await expect(page.getByRole('heading', { name: 'Đăng nhập' })).toBeVisible();
    await expect(page.getByPlaceholder('Nhập email của bạn')).toBeVisible();
    await expect(page.getByPlaceholder('Nhập mật khẩu')).toBeVisible();
    await expect(page.getByRole('link', { name: 'Đăng ký ngay' })).toHaveAttribute('href', '/auth/register');
    await expect(page.getByRole('link', { name: /Đăng nhập bằng Google/i })).toHaveAttribute('href', /oauth2\/authorization\/google$/);
    if (variant === 'after') {
      await expect(page.locator('.tm-login-page')).toBeVisible();
      await expect(page.getByRole('link', { name: 'tutormatch' })).toBeVisible();
    }
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidenceDirectory, `login-${variant}-${width}.png`), fullPage: true });
  }

  if (variant === 'before') return;

  const email = page.getByPlaceholder('Nhập email của bạn');
  await email.focus();
  await expect(email).toBeFocused();
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page.getByText('Email không được để trống')).toBeVisible();
  await expect(page.getByText('Mật khẩu không được để trống')).toBeVisible();

  for (const route of [
    '/auth/register',
    '/auth/verify-email',
    '/auth/forgot-password',
    '/auth/reset-password',
    '/auth/oauth/role?registrationToken=visual-check',
  ]) {
    await page.goto(route);
    await expect(page.getByRole('link', { name: 'tutormatch' })).toBeVisible();
  }
});
