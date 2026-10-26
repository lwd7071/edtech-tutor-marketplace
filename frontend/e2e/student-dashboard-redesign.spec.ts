import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const student = { id: 'student-dashboard-visual', email: 'student@example.test', fullName: 'An Nguyen', role: 'STUDENT', status: 'ACTIVE' };
const summary: { nextBookingStartTime: string | null; remainingSessions: number; todoAssignments: number; unreadNotifications: number; pendingRequests: number } = {
  nextBookingStartTime: '2026-10-20T03:00:00Z', remainingSessions: 12, todoAssignments: 3, unreadNotifications: 4, pendingRequests: 2,
};

test('student dashboard follows the approved task-first composition without changing its read model', async ({ page }) => {
  const evidenceDirectory = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidenceDirectory, { recursive: true });
  let apiSummary = summary;
  let apiStatus = 200;
  let apiDelay = 0;
  await page.route('**/api/auth/login', route => route.fulfill({
    status: 200, contentType: 'application/json',
    body: JSON.stringify({ success: true, message: 'ok', data: { accessToken: 'visual-access', refreshToken: 'visual-refresh', user: student }, errors: null, meta: null }),
  }));
  await page.route('**/api/student/dashboard', async route => {
    if (apiDelay) await new Promise(resolveRequest => setTimeout(resolveRequest, apiDelay));
    await route.fulfill({
      status: apiStatus, contentType: 'application/json',
      body: JSON.stringify(apiStatus === 200
        ? { success: true, message: 'ok', data: apiSummary, errors: null, meta: null }
        : { success: false, message: 'Dashboard unavailable', data: null, errors: null, meta: null }),
    });
  });

  await page.setViewportSize({ width: 1280, height: 900 });
  await page.goto('/auth/login');
  await page.getByPlaceholder('Nhập email của bạn').fill(student.email);
  await page.getByPlaceholder('Nhập mật khẩu').fill('Password123!');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page).toHaveURL(/\/student$/);
  await expect(page.getByText('12 buổi')).toBeVisible();

  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    if (variant === 'after') {
      await expect(page.getByRole('heading', { name: 'Chào An Nguyen!' })).toBeVisible();
      await expect(page.getByRole('heading', { name: 'Buổi học tiếp theo' })).toBeVisible();
      await expect(page.getByRole('link', { name: 'Xem lịch học' })).toHaveAttribute('href', '/student/bookings');
      await expect(page.getByRole('link', { name: /Lượt học còn lại.*12 buổi/ })).toHaveAttribute('href', '/student/packages');
      await expect(page.getByRole('link', { name: /Bài cần làm.*3 bài/ })).toHaveAttribute('href', '/student/assignments');
      await expect(page.locator('.tm-brand')).toHaveText('tutormatch');
      await expect(page.getByRole('link', { name: 'Báo cáo buổi học' })).toHaveAttribute('href', '/student/session-reports');
      await expect(page.getByRole('link', { name: 'Tin nhắn' }).last()).toHaveAttribute('href', '/student/messages');
    }
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidenceDirectory, `student-dashboard-${variant}-${width}.png`), fullPage: true });
  }

  if (variant === 'before') return;

  const nextLesson = page.getByRole('link', { name: 'Xem lịch học' });
  await nextLesson.focus();
  await page.keyboard.press('Tab');
  await page.keyboard.press('Shift+Tab');
  await expect(nextLesson).toBeFocused();

  apiDelay = 1500;
  const loadingReload = page.reload();
  await expect(page.locator('.tm-student-dashboard-loading')).toBeVisible();
  await loadingReload;

  apiDelay = 0;
  apiStatus = 503;
  await page.reload();
  await expect(page.getByText('Chưa tải được tổng quan')).toBeVisible();
  await expect(page.locator('.tm-student-next')).toHaveCount(0);
  apiStatus = 200;
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByRole('link', { name: 'Xem lịch học' })).toBeVisible();

  apiSummary = { ...summary, nextBookingStartTime: null, remainingSessions: 0, todoAssignments: 0, unreadNotifications: 0, pendingRequests: 0 };
  await page.reload();
  await expect(page.getByRole('link', { name: 'Tìm gia sư' })).toHaveAttribute('href', '/subjects');
  await expect(page.getByRole('link', { name: /0 buổi/ })).toBeVisible();
  await page.setViewportSize({ width: 375, height: 900 });
  await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
  const bottomClearance = await page.evaluate(() => {
    const lastShortcut = document.querySelector('.tm-student-shortcuts a:last-child')?.getBoundingClientRect();
    const bottomNavigation = document.querySelector('.tm-workspace-bottom')?.getBoundingClientRect();
    return { shortcutBottom: lastShortcut?.bottom ?? 0, navigationTop: bottomNavigation?.top ?? 0 };
  });
  expect(bottomClearance.shortcutBottom).toBeLessThanOrEqual(bottomClearance.navigationTop);
});
