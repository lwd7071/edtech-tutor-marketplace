import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';

test('become-a-tutor explains the real onboarding and preserves both existing paths', async ({ page }) => {
  const evidenceDirectory = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidenceDirectory, { recursive: true });

  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.goto('/become-a-tutor');
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    await expect(page.locator('.tm-header .tm-brand')).toHaveText('tutormatch');
    await expect(page.getByRole('heading', { name: 'Kiến thức của bạn, bước tiến của học viên.', level: 1 })).toBeVisible();
    const stepHeadingLevel = variant === 'before' ? 2 : 3;
    await expect(page.getByRole('heading', { name: 'Chuẩn bị hồ sơ', level: stepHeadingLevel })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Gửi xét duyệt', level: stepHeadingLevel })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Bắt đầu giảng dạy', level: stepHeadingLevel })).toBeVisible();
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidenceDirectory, `become-a-tutor-${variant}-${width}.png`), fullPage: true });
  }

  if (variant === 'before') return;

  const actions = page.locator('.tm-page-actions');
  const register = actions.getByRole('link', { name: 'Đăng ký gia sư' });
  await expect(register).toHaveAttribute('href', '/auth/register?role=TEACHER');
  await expect(actions.getByRole('link', { name: 'Tiếp tục hồ sơ' })).toHaveAttribute('href', '/teacher/profile');
  for (let tab = 0; tab < 40 && !(await register.evaluate(element => element === document.activeElement)); tab += 1) {
    await page.keyboard.press('Tab');
  }
  await expect(register).toBeFocused();
  expect(await register.evaluate(element => element.matches(':focus-visible'))).toBe(true);
});
