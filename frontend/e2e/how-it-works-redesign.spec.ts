import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';

test('how-it-works page preserves its steps and existing routes responsively', async ({ page }) => {
  const evidenceDirectory = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidenceDirectory, { recursive: true });

  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.goto('/how-it-works');
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    await expect(page.locator('.tm-header .tm-brand')).toHaveText('tutormatch');
    await expect(page.getByRole('heading', { name: 'Từ tìm gia sư đến buổi học đầu tiên', level: 1 })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Tìm người phù hợp', level: 2 })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Chọn cách bắt đầu', level: 2 })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Theo dõi việc học', level: 2 })).toBeVisible();
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidenceDirectory, `how-it-works-${variant}-${width}.png`), fullPage: true });
  }

  if (variant === 'before') return;

  const actions = page.locator('.tm-page-actions');
  await expect(actions.getByRole('link', { name: 'Tìm gia sư' })).toHaveAttribute('href', '/teachers');
  await expect(actions.getByRole('link', { name: 'Trở thành gia sư' })).toHaveAttribute('href', '/become-a-tutor');
  const tutorLink = actions.getByRole('link', { name: 'Tìm gia sư' });
  for (let tab = 0; tab < 40 && !(await tutorLink.evaluate(element => element === document.activeElement)); tab += 1) {
    await page.keyboard.press('Tab');
  }
  await expect(tutorLink).toBeFocused();
  expect(await tutorLink.evaluate(element => element.matches(':focus-visible'))).toBe(true);
});
