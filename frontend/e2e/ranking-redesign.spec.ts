import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const rankedLinks = 'a[href^="/teachers/"][href*="3333"]';

test('ranking keeps real ranking values, subject filter, profile links, and responsive behavior', async ({ page }) => {
  const evidenceDirectory = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidenceDirectory, { recursive: true });

  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.goto('/ranking');
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    await expect(page.locator('.tm-header .tm-brand')).toHaveText('tutormatch');
    await expect(page.getByRole('heading', { name: /Bảng xếp hạng gia sư/i, level: 1 })).toBeVisible();
    await expect(page.getByText('Gia sư 4')).toBeVisible();
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidenceDirectory, `ranking-${variant}-${width}.png`), fullPage: true });
  }

  if (variant === 'before') return;

  await page.setViewportSize({ width: 1280, height: 900 });
  await page.goto('/ranking');
  await page.evaluate(() => window.scrollTo(0, 0));
  const subjectFilter = page.getByRole('combobox', { name: 'Lọc theo môn học' });
  await subjectFilter.click();
  await page.keyboard.press('ArrowDown');
  await page.keyboard.press('Enter');
  await expect(page).toHaveURL(/subjectId=subject-[a-z]+/);
  const selectedSubjectId = new URL(page.url()).searchParams.get('subjectId');
  const expectedCount = selectedSubjectId === 'subject-math' ? 7 : selectedSubjectId === 'subject-english' ? 6 : 0;
  await expect(page.locator(rankedLinks)).toHaveCount(expectedCount);

  await page.goto('/ranking?subjectId=subject-math');
  await expect(page.locator(rankedLinks)).toHaveCount(7);
  await expect(page.locator(rankedLinks).first()).toHaveAttribute('href', /\/teachers\/00000001-3333-4333-8333-000000000001$/);

  await page.goto('/ranking');
  const firstProfile = page.locator(rankedLinks).first();
  for (let tab = 0; tab < 32 && !(await firstProfile.evaluate(element => element === document.activeElement)); tab += 1) {
    await page.keyboard.press('Tab');
  }
  await expect(firstProfile).toBeFocused();
  expect(await firstProfile.evaluate(element => element.matches(':focus-visible'))).toBe(true);

  await page.goto('/ranking?subjectId=fixture-slow');
  await expect(page.getByTestId('ranking-loading')).toBeVisible();
  await expect(page.getByText('Chưa có dữ liệu xếp hạng')).toBeVisible();
  await page.goto('/ranking?subjectId=fixture-error');
  await expect(page.getByText('Chưa tải được bảng xếp hạng.', { exact: true })).toBeVisible();
});
