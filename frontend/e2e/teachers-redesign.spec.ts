import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

test('teacher directory renders responsively and preserves search and preview behavior', async ({ page }) => {
  const evidenceDirectory = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidenceDirectory, { recursive: true });

  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.goto('/teachers');
    await expect(page.getByRole('heading', { name: 'Gia sư cho mục tiêu của bạn' })).toBeVisible();
    await expect(page.locator('.tm-search-result')).toHaveCount(3);
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    if (width === 375) {
      const avatar = await page.locator('.tm-result-profile .ant-avatar').first().boundingBox();
      const details = await page.locator('.tm-search-result-content').first().boundingBox();
      expect(avatar).not.toBeNull();
      expect(details).not.toBeNull();
      expect(avatar!.x + avatar!.width).toBeLessThanOrEqual(details!.x + 1);
    }
    await page.screenshot({ path: resolve(evidenceDirectory, `teachers-after-${width}.png`), fullPage: true });
  }

  await page.setViewportSize({ width: 375, height: 900 });
  await page.goto('/teachers');
  const teacherButton = page.getByRole('button', { name: 'Xem nhanh Nguyễn An' });
  await teacherButton.focus();
  await expect(page.getByRole('region', { name: 'Xem nhanh hồ sơ Nguyễn An' }).first()).toBeVisible();
  await page.getByRole('button', { name: 'Đóng xem nhanh' }).click();
  await expect(teacherButton).toBeFocused();
  await page.getByRole('searchbox', { name: 'Tìm gia sư theo từ khóa' }).fill('Nguyễn An');
  await page.getByRole('searchbox', { name: 'Tìm gia sư theo từ khóa' }).press('Enter');
  await expect(page).toHaveURL(/keyword=/);
  await expect(page.locator('.tm-search-result')).toHaveCount(1);

  await page.setViewportSize({ width: 1280, height: 900 });
  await page.goto('/teachers');
  await page.locator('.tm-filter-desktop').getByRole('combobox').first().click();
  await page.locator('.tm-filter-desktop').getByRole('combobox').first().fill('Toán');
  await page.keyboard.press('ArrowDown');
  await page.keyboard.press('Enter');
  await expect(page).toHaveURL(/subjectId=subject-math/);
  await page.getByRole('button', { name: 'Bỏ lọc: Toán học' }).click();
  await expect(page).not.toHaveURL(/subjectId=/);
  await page.getByRole('combobox', { name: 'Sắp xếp' }).click();
  await page.keyboard.press('ArrowDown');
  await page.keyboard.press('Enter');
  await expect(page).toHaveURL(/sort=price_asc/);
  await page.goto('/teachers?keyword=pagination-fixture');
  await expect(page.locator('.tm-search-result')).toHaveCount(12);
  await page.getByRole('navigation', { name: 'Trang kết quả gia sư' }).getByRole('listitem', { name: '2' }).click();
  await expect(page).toHaveURL(/page=2/);
  await expect(page.locator('.tm-search-result')).toHaveCount(1);
  await page.setViewportSize({ width: 375, height: 900 });
  await page.goto('/teachers');
  await page.getByRole('button', { name: /Bộ lọc/ }).click();
  await expect(page.getByRole('dialog', { name: 'Lọc gia sư' })).toBeVisible();
  await page.getByRole('button', { name: 'Xem kết quả' }).click();
  await expect(page.getByRole('dialog', { name: 'Lọc gia sư' })).toHaveCount(0);
});
