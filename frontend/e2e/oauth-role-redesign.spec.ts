import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';

test('OAuth role selection preserves the one-time registration flow', async ({ page }) => {
  const evidenceDirectory = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidenceDirectory, { recursive: true });

  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.goto('/auth/oauth/role?registrationToken=visual-fixture');
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    await expect(page.getByRole('heading', { name: 'Hoàn tất đăng ký' })).toBeVisible();
    await expect(page.getByText('Vai trò này không thể thay đổi sau khi bạn hoàn tất đăng ký.')).toBeVisible();
    await expect(page.getByText('Học viên / Phụ huynh')).toBeVisible();
    await expect(page.getByText('Gia sư', { exact: true })).toBeVisible();
    if (variant === 'after') {
      await expect(page.locator('.tm-oauth-role-page')).toBeVisible();
      await expect(page.getByRole('link', { name: 'tutormatch' })).toBeVisible();
    }
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidenceDirectory, `oauth-role-${variant}-${width}.png`), fullPage: true });
  }

  if (variant === 'before') return;

  await page.route('**/api/auth/oauth2/complete-registration', async (route) => {
    await new Promise((resolveRequest) => setTimeout(resolveRequest, 350));
    await route.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify({
      success: true,
      message: 'ok',
      data: { accessToken: 'test-access', refreshToken: 'test-refresh', tokenType: 'Bearer', accessTokenExpiresIn: 900, user: { id: 'visual-user', email: 'teacher@example.test', fullName: 'Visual Teacher', role: 'TEACHER', status: 'ACTIVE', avatarUrl: null } },
      errors: null,
      meta: null,
    }) });
  });
  await page.getByText('Gia sư', { exact: true }).click();
  await page.getByRole('button', { name: 'Hoàn tất đăng ký' }).click();
  await expect(page.locator('.tm-oauth-role-submit')).toHaveClass(/ant-btn-loading/);
  await expect(page).toHaveURL(/\/teacher$/);

  await page.goto('/auth/oauth/role');
  await page.getByRole('button', { name: 'Hoàn tất đăng ký' }).click();
  await expect(page.getByText('Token không hợp lệ. Vui lòng đăng nhập lại.')).toBeVisible();
});
