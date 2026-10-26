import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';

test('OAuth callback keeps provider, exchange, and registration outcomes intact', async ({ page }) => {
  const evidenceDirectory = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidenceDirectory, { recursive: true });

  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.goto('/oauth2/callback?error=access_denied');
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    await expect(page.getByText('Google không thể xác thực tài khoản. Vui lòng thử lại.')).toBeVisible();
    await expect(page.locator('a[href="/auth/login"]')).toBeVisible();
    if (variant === 'after') await expect(page.getByRole('heading', { name: 'Xác thực Google chưa hoàn tất' })).toBeVisible();
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidenceDirectory, `oauth-callback-${variant}-${width}.png`), fullPage: true });
  }

  if (variant === 'before') return;

  await page.route('**/api/auth/oauth2/exchange', async (route) => {
    expect(await route.request().postDataJSON()).toEqual({ exchangeCode: 'fixture-code' });
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
      success: true,
      message: 'ok',
      data: { accessToken: 'fixture-access', refreshToken: 'fixture-refresh', tokenType: 'Bearer', accessTokenExpiresIn: 900, user: { id: 'fixture-user', email: 'student@example.test', fullName: 'Fixture Student', role: 'STUDENT', status: 'ACTIVE', avatarUrl: null } },
      errors: null,
      meta: null,
    }) });
  });
  await page.goto('/oauth2/callback?exchangeCode=fixture-code');
  await expect(page).toHaveURL(/\/student$/);

  await page.goto('/oauth2/callback?registrationToken=fixture-registration');
  await expect(page).toHaveURL(/\/auth\/oauth\/role\?registrationToken=fixture-registration$/);

  await page.route('**/api/auth/oauth2/exchange', (route) => route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ success: false, message: 'Unavailable', data: null, errors: null, meta: null }) }));
  await page.goto('/oauth2/callback?exchangeCode=failed-code');
  await expect(page.getByText('Unavailable')).toBeVisible();
  await expect(page.locator('a[href="/auth/login"]')).toBeVisible();
  await page.keyboard.press('Tab');
  await expect(page.locator('a[href="/auth/login"]')).toBeFocused();
});
