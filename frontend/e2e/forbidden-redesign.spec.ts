import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';

test('forbidden access explains the 403 state and returns to the previous page', async ({ page }) => {
  const evidenceDirectory = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidenceDirectory, { recursive: true });

  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.goto('/forbidden');
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    if (variant === 'before') {
      await expect(page.getByText('403 - Truy cập bị từ chối')).toBeVisible();
    } else {
      await expect(page.getByRole('heading', { name: 'Truy cập bị từ chối', level: 1 })).toBeVisible();
      await expect(page.getByText('403', { exact: true })).toBeVisible();
      await expect(page.locator('.tm-forbidden-feedback-error')).toBeVisible();
    }
    await expect(page.getByText('Bạn không có quyền truy cập vào trang này.')).toBeVisible();
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidenceDirectory, `forbidden-${variant}-${width}.png`), fullPage: true });
  }

  if (variant === 'before') return;

  await page.goto('/how-it-works');
  await page.goto('/forbidden');
  const back = page.getByRole('button', { name: 'Quay lại' });
  await expect(back).toBeVisible();
  for (let tab = 0; tab < 40 && !(await back.evaluate(element => element === document.activeElement)); tab += 1) {
    await page.keyboard.press('Tab');
  }
  await expect(back).toBeFocused();
  expect(await back.evaluate(element => element.matches(':focus-visible'))).toBe(true);
  await back.click();
  await expect(page).toHaveURL(/\/how-it-works$/);
});
