import { expect, test } from '@playwright/test';

const teacher = { id: 'teacher-e2e', email: 'teacher@example.test', fullName: 'Gia sư E2E', role: 'TEACHER', status: 'APPROVED' };

test.beforeEach(async ({ page }) => {
  await page.route('**/api/auth/login', route => route.fulfill({
    status: 200, contentType: 'application/json',
    body: JSON.stringify({ success: true, data: { accessToken: 'teacher-access', refreshToken: 'teacher-refresh', user: teacher } }),
  }));
  await page.route('**/api/teacher/**', route => {
    const path = new URL(route.request().url()).pathname;
    const data = path === '/api/teacher/profile' ? { profileStatus: 'APPROVED' }
      : path === '/api/teacher/wallet' ? { availableBalanceVnd: 0, heldBalanceVnd: 0 }
      : [];
    return route.fulfill({ status: 200, contentType: 'application/json',
      body: JSON.stringify({ success: true, data, meta: { page: 0, size: 20, totalPages: 0, totalElements: 0 } }) });
  });
});

test('teacher navigation is grouped by workflow and keeps the current destination', async ({ page }, testInfo) => {
  await page.goto('/auth/login');
  await page.getByPlaceholder('Nhập email của bạn').fill(teacher.email);
  await page.getByPlaceholder('Nhập mật khẩu').fill('Password123!');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page).toHaveURL(/\/teacher$/);
  await expect(page.getByText('Chưa có buổi học sắp tới.')).toBeVisible();
  await page.screenshot({ path: testInfo.outputPath('teacher-overview-desktop.png'), fullPage: true });
  const nav = page.getByRole('navigation', { name: 'Điều hướng không gian làm việc' });
  for (const group of ['Hồ sơ', 'Giảng dạy', 'Học tập', 'Tài chính', 'Giao tiếp']) {
    await expect(nav.getByText(group, { exact: true })).toBeVisible();
  }
  await nav.getByRole('link', { name: 'Lịch dạy' }).click();
  await expect(page).toHaveURL(/\/teacher\/bookings$/);
  await expect(nav.getByRole('link', { name: 'Lịch dạy' })).toHaveAttribute('aria-current', 'page');
});

test('mobile workspace menu opens, navigates, and restores focus', async ({ page }, testInfo) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/auth/login');
  await page.getByPlaceholder('Nhập email của bạn').fill(teacher.email);
  await page.getByPlaceholder('Nhập mật khẩu').fill('Password123!');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page).toHaveURL(/\/teacher$/);
  await expect(page.getByText('Chưa có buổi học sắp tới.')).toBeVisible();
  await page.screenshot({ path: testInfo.outputPath('teacher-overview-mobile.png'), fullPage: true });
  const menu = page.getByRole('button', { name: 'Mở menu' });
  await menu.click();
  const drawer = page.getByRole('dialog', { name: 'Điều hướng' });
  await expect(drawer).toBeVisible();
  await drawer.getByRole('link', { name: 'Lịch dạy' }).click();
  await expect(page).toHaveURL(/\/teacher\/bookings$/);
  await expect(drawer).toBeHidden();
  await expect(menu).toBeFocused();
});
