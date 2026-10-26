import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';

test('terms page remains readable and links to the existing privacy page', async ({ page }) => {
  const evidenceDirectory = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidenceDirectory, { recursive: true });

  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.goto('/terms');
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    await expect(page.locator('.tm-header .tm-brand')).toHaveText('tutormatch');
    await expect(page.getByRole('heading', { name: 'Điều khoản sử dụng', level: 1 })).toBeVisible();
    await expect(page.getByText('Khi sử dụng Tutor Match, bạn đồng ý cung cấp thông tin chính xác, bảo vệ tài khoản và sử dụng nền tảng cho mục đích học tập hợp pháp.')).toBeVisible();
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidenceDirectory, `terms-${variant}-${width}.png`), fullPage: true });
  }

  if (variant === 'before') return;

  const relatedLink = page.getByRole('navigation', { name: 'Tài liệu liên quan' }).getByRole('link', { name: 'Chính sách riêng tư' });
  await expect(relatedLink).toHaveAttribute('href', '/privacy');
  for (let tab = 0; tab < 40 && !(await relatedLink.evaluate(element => element === document.activeElement)); tab += 1) {
    await page.keyboard.press('Tab');
  }
  await expect(relatedLink).toBeFocused();
  expect(await relatedLink.evaluate(element => element.matches(':focus-visible'))).toBe(true);
});
