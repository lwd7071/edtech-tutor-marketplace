import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

test('guest can navigate from homepage to subject selection', async ({ page }) => {
  await page.goto('/');
  await expect(page).toHaveTitle(/Tutor Match/i);
  await expect(page.locator('.tm-header .tm-brand')).toHaveText('tutormatch');
  await page.getByRole('link', { name: /Tìm gia sư/i }).first().click();
  await expect(page).toHaveURL(/\/subjects/);
  await expect(page.getByRole('heading', { name: /Bạn muốn học môn nào/i })).toBeVisible();
});

test('public shell remains consistent across real public routes', async ({ page }) => {
  for (const path of ['/subjects', '/teachers', '/ranking', '/how-it-works', '/become-a-tutor', '/support', '/terms', '/privacy']) {
    const response = await page.goto(path);
    expect(response?.status()).toBe(200);
    await expect(page.locator('.tm-header .tm-brand')).toHaveText('tutormatch');
    await expect(page.locator('footer.tm-footer')).toBeVisible();
  }
});

test('login page exposes password and Google entry points', async ({ page }) => {
  await page.goto('/auth/login');
  await expect(page.getByRole('heading', { name: 'Đăng nhập' })).toBeVisible();
  await expect(page.getByRole('button', { name: /Đăng nhập$/i })).toBeVisible();
  await expect(page.getByRole('link', { name: /Google/i })).toHaveAttribute('href', /oauth2\/authorization\/google/);
});

test('registration keeps the student email for the verification screen', async ({ page }) => {
  await page.route('**/api/auth/register', route => route.fulfill({
    status: 201,
    contentType: 'application/json',
    body: JSON.stringify({ success: true, data: { email: 'learner@example.test', verificationRequired: true }, errors: null, meta: null }),
  }));
  await page.goto('/auth/register');
  await page.getByPlaceholder(/Nguyễn Văn A/i).fill('Learner Test');
  await page.getByPlaceholder(/Nhập email/i).fill('learner@example.test');
  await page.getByPlaceholder(/^Nhập mật khẩu$/i).fill('Password123!');
  await page.getByRole('checkbox').check();
  await page.getByRole('button', { name: /Đăng ký/i }).click();
  await expect(page).toHaveURL(/verify-email\?email=learner%40example\.test/);
});

test('public legal and support links resolve to real pages', async ({ page }) => {
  for (const path of ['/terms', '/privacy', '/support']) {
    await page.goto(path);
    await expect(page.locator('h1')).toBeVisible();
    await expect(page.locator('body')).not.toContainText('404');
  }
});

test('homepage hierarchy and actions fit mobile, tablet, and desktop viewports', async ({ page }) => {
  const evidenceDirectory = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidenceDirectory, { recursive: true });

  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.goto('/');
    await expect(page.locator('.tm-header .tm-brand')).toHaveText('tutormatch');
    await expect(page.getByRole('heading', { name: /Học đúng người\. Tiến đúng mục tiêu\./i })).toBeVisible();
    await expect(page.getByRole('img', { name: 'Học viên viết bài trong buổi học trực tuyến cùng gia sư' })).toBeVisible();
    const tutorCta = page.locator('.tm-hero-cta');
    await expect(tutorCta).toBeVisible();
    await expect(tutorCta).toHaveAttribute('href', '/subjects');
    await expect(page.locator('.tm-search-form')).toHaveCount(0);
    await expect(page.locator('.tm-hero-cta-arrow')).toHaveCount(0);
    await expect(page.getByRole('link', { name: 'Đăng ký làm gia sư' })).toHaveAttribute('href', '/become-a-tutor');
    const ctaBounds = await tutorCta.boundingBox();
    expect(ctaBounds).not.toBeNull();
    expect(ctaBounds!.x + ctaBounds!.width).toBeLessThanOrEqual(width);
    await page.keyboard.press('Tab');
    for (let tab = 0; tab < 12 && !(await tutorCta.evaluate(element => element === document.activeElement)); tab += 1) {
      await page.keyboard.press('Tab');
    }
    await expect(tutorCta).toBeFocused();
    await expect.poll(() => tutorCta.evaluate(element => getComputedStyle(element).outlineStyle)).toBe('solid');
    await page.keyboard.press('Tab');
    await expect(page.getByRole('link', { name: 'Tất cả môn học' })).toBeFocused();
    await page.evaluate(() => (document.activeElement as HTMLElement | null)?.blur());

    for (const image of [
      page.locator('.tm-home-tutor-art'),
      page.getByRole('img', { name: 'Học viên trao đổi bài cùng gia sư trong lớp học trực tuyến' }),
    ]) {
      await image.scrollIntoViewIfNeeded();
      await expect.poll(() => image.evaluate(element => (element as HTMLImageElement).naturalWidth)).toBeGreaterThan(0);
    }
    await page.evaluate(() => window.scrollTo(0, 0));

    const homeWidths = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    expect(homeWidths.content).toBeLessThanOrEqual(homeWidths.viewport + 1);
    await page.screenshot({ path: resolve(evidenceDirectory, `home-after-${width}.png`), fullPage: true });

    const howItWorks = page.getByRole('region', { name: 'Cách hoạt động' });
    await howItWorks.scrollIntoViewIfNeeded();
    await expect(howItWorks).toBeVisible();

    await page.goto('/subjects');
    await expect(page.getByRole('heading', { name: /Bạn muốn học môn nào/i })).toBeVisible();
    const widths = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    expect(widths.content).toBeLessThanOrEqual(widths.viewport + 1);
  }
});

test('subject picker matches responsive references and preserves filters through tutor search', async ({ page }) => {
  const evidenceDirectory = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidenceDirectory, { recursive: true });

  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.goto('/subjects');
    await expect(page.getByRole('heading', { name: 'Bạn muốn học môn nào?' })).toBeVisible();
    await expect(page.getByRole('group', { name: 'Danh sách môn học' })).toBeVisible();
    await expect(page.getByRole('radio')).toHaveCount(11);
    await expect(page.getByRole('button', { name: 'Tìm gia sư' })).toBeDisabled();
    await expect(page.locator('.tm-header .tm-brand')).toHaveText('tutormatch');

    const dimensions = await page.evaluate(() => ({
      viewport: document.documentElement.clientWidth,
      content: document.documentElement.scrollWidth,
    }));
    expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidenceDirectory, `subjects-after-${width}.png`), fullPage: true });
  }

  await page.setViewportSize({ width: 375, height: 900 });
  await page.goto('/subjects');
  const filters = page.getByRole('button', { name: /Tìm kiếm hoặc lọc môn học/ });
  await expect(filters).toHaveAttribute('aria-expanded', 'false');
  await filters.click();
  await expect(page.getByRole('searchbox', { name: 'Tìm môn học' })).toBeVisible();
  await page.getByRole('searchbox', { name: 'Tìm môn học' }).fill(`No match ${Date.now()}`);
  await page.getByRole('button', { name: 'Tìm môn' }).click();
  await expect(page.getByRole('heading', { name: 'Chưa tìm thấy môn học phù hợp' })).toBeVisible();
  await page.getByRole('button', { name: 'Xem tất cả môn học' }).click();
  await expect(page.getByRole('radio')).toHaveCount(11);
  const keyboardSubject = page.getByRole('radio', { name: /Toán học/ });
  await keyboardSubject.focus();
  await page.keyboard.press('Space');
  await expect(keyboardSubject).toBeChecked();
  await expect(page).toHaveURL(/subjectId=subject-math/);
  const levelChange = page.getByLabel('Lọc theo cấp học').selectOption('UNIVERSITY');
  await levelChange;
  await expect(page).toHaveURL(/educationLevel=UNIVERSITY/);
  await page.getByRole('searchbox', { name: 'Tìm môn học' }).fill('Lập trình');
  await page.getByRole('button', { name: 'Tìm môn' }).click();
  await expect(page).toHaveURL(/keyword=L%E1%BA%ADp\+tr%C3%ACnh/);
  const programming = page.getByRole('radio', { name: /Lập trình/ });
  await programming.check();
  await expect(programming).toBeChecked();
  await page.getByRole('button', { name: /Tìm gia sư môn Lập trình/ }).click();
  await expect(page).toHaveURL(/\/teachers\?subjectId=subject-programming/);
});
