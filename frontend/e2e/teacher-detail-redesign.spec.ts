import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const teacherId = '22222222-2222-4222-8222-222222222222';
const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';

test('teacher profile hierarchy, responsive layout, and guest actions', async ({ page }) => {
  const evidenceDirectory = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidenceDirectory, { recursive: true });

  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.goto(`/teachers/${teacherId}`);
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    await expect(page.locator('.tm-header .tm-brand')).toHaveText('tutormatch');
    await expect(page.getByRole('heading', { name: 'Nguyễn An', level: 1 })).toBeVisible();
    await expect(page.locator('#packages')).toBeVisible();
    await expect(page.locator('#availability')).toBeVisible();
    await expect(page.locator('#reviews')).toBeVisible();
    await expect(page.getByText('Bằng cử nhân Sư phạm')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Chọn gói học' }).first()).toBeVisible();
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidenceDirectory, `teacher-detail-${variant}-${width}.png`), fullPage: true });
  }

  await page.setViewportSize({ width: 1280, height: 900 });
  await page.goto(`/teachers/${teacherId}`);
  await page.getByRole('navigation', { name: 'Các phần hồ sơ' }).getByRole('link', { name: 'Đánh giá' }).click();
  await expect(page).toHaveURL(/#reviews$/);
  await page.goto(`/teachers/${teacherId}`);
  const choosePackage = page.getByRole('button', { name: 'Chọn gói học' }).first();
  for (let tab = 0; tab < 40 && !(await choosePackage.evaluate(element => element === document.activeElement)); tab += 1) {
    await page.keyboard.press('Tab');
  }
  await expect(choosePackage).toBeFocused();
  expect(await choosePackage.evaluate(element => element.matches(':focus-visible'))).toBe(true);
  await page.keyboard.press('Enter');
  await expect(page).toHaveURL(/\/auth\/login\?/);
  expect(new URL(page.url()).searchParams.get('redirect')).toBe(`/teachers/${teacherId}?packageId=package-fixture-1#packages`);
});
