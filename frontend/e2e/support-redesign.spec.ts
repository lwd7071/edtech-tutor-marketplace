import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';

test('support page lists only the existing in-account help destinations', async ({ page }) => {
  const evidenceDirectory = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidenceDirectory, { recursive: true });

  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.goto('/support');
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    await expect(page.locator('.tm-header .tm-brand')).toHaveText('tutormatch');
    await expect(page.getByRole('heading', { name: 'Bạn cần hỗ trợ?', level: 1 })).toBeVisible();
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidenceDirectory, `support-${variant}-${width}.png`), fullPage: true });
  }

  if (variant === 'before') return;

  const links = page.locator('.tm-support-topic-list');
  await expect(links.getByRole('link', { name: 'Yêu cầu học thử, hoàn tiền và gia hạn' })).toHaveAttribute('href', '/student/requests');
  await expect(links.getByRole('link', { name: 'Thông báo và kết quả xử lý' })).toHaveAttribute('href', '/student/notifications');
  await expect(links.getByRole('link', { name: 'Trao đổi với gia sư' })).toHaveAttribute('href', '/student/messages');
  const firstLink = links.getByRole('link').first();
  for (let tab = 0; tab < 40 && !(await firstLink.evaluate(element => element === document.activeElement)); tab += 1) {
    await page.keyboard.press('Tab');
  }
  await expect(firstLink).toBeFocused();
  expect(await firstLink.evaluate(element => element.matches(':focus-visible'))).toBe(true);
});
