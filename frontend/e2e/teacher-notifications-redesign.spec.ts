import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const teacher = { id: 'notification-teacher', email: 'teacher-notify@example.test', fullName: 'Trần Thu Hà', role: 'TEACHER', status: 'ACTIVE' };
const envelope = (data: unknown, meta: unknown = null) => ({ success: true, message: 'ok', data, errors: null, meta });

test('teacher notifications use Teacher language, preserve read actions, and open only Teacher references', async ({ page }) => {
  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  let listStatus = 200;
  let readStatus = 200;
  let readAllStatus = 200;
  const requests: URL[] = [];
  const readCalls: string[] = [];
  let readAllCalls = 0;
  const notifications = [
    { id: 'teacher-booking', userId: teacher.id, type: 'BOOKING', title: 'Buổi dạy đã xác nhận', content: 'Lịch dạy với học viên đã được xác nhận.', isRead: false, referenceId: 'booking-1', referenceType: 'BOOKING', referenceUrl: '/teacher/bookings/booking-1', createdAt: '2026-10-16T08:30:00Z' },
    { id: 'wrong-role', userId: teacher.id, type: 'ASSIGNMENT', title: 'Liên kết sai vai trò', content: 'Thông báo không được dẫn sang khu vực học viên.', isRead: true, referenceId: 'assignment-1', referenceType: 'ASSIGNMENT', referenceUrl: '/student/assignments/assignment-1', createdAt: '2026-10-15T08:30:00Z' },
    { id: 'unsafe', userId: teacher.id, type: 'SYSTEM', title: 'Đích ngoài hệ thống', content: 'Liên kết bên ngoài không được tự mở.', isRead: true, referenceId: null, referenceType: 'SYSTEM', referenceUrl: 'https://outside.example.test/notice', createdAt: '2026-10-14T08:30:00Z' },
  ];

  await page.addInitScript(user => {
    localStorage.setItem('tutor-match.session', JSON.stringify({ user, remember: false, sessionId: 'teacher-notification-session' }));
    document.cookie = 'accessToken=teacher-notification-access; path=/; SameSite=Lax';
  }, teacher);
  await page.route('**/api/notifications**', async route => {
    const url = new URL(route.request().url());
    if (route.request().method() === 'GET') {
      requests.push(url);
      const pageIndex = Number(url.searchParams.get('page') || 0);
      const type = url.searchParams.get('referenceType');
      const isEmpty = type === 'SYSTEM';
      const data = isEmpty ? [] : pageIndex === 0 ? notifications : [{ ...notifications[1], id: 'older', title: 'Thông báo cũ', isRead: true }];
      const totalPages = isEmpty ? 0 : 2;
      await route.fulfill({ status: listStatus, contentType: 'application/json', body: JSON.stringify(listStatus === 200 ? envelope(data, { page: pageIndex, size: 20, totalElements: 21, totalPages, hasNext: pageIndex + 1 < totalPages }) : { success: false, message: 'unavailable', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null }) });
      return;
    }
    if (route.request().method() === 'PATCH') {
      const id = url.pathname.split('/').at(-2) || '';
      readCalls.push(id);
      const notification = notifications.find(item => item.id === id);
      await route.fulfill({ status: readStatus, contentType: 'application/json', body: JSON.stringify(readStatus === 200 ? envelope({ ...notification, isRead: true }) : { success: false, message: 'unavailable', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null }) });
      return;
    }
    readAllCalls += 1;
    await route.fulfill({ status: readAllStatus, contentType: 'application/json', body: JSON.stringify(readAllStatus === 200 ? envelope({ updatedCount: 2 }) : { success: false, message: 'unavailable', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null }) });
  });

  await page.goto('/teacher/notifications');
  await expect(page.getByRole('heading', { name: 'Thông báo' })).toBeVisible();
  await expect(page.locator('.tm-notifications__title', { hasText: 'Buổi dạy đã xác nhận' })).toBeVisible();
  if (variant === 'after') {
    await expect(page.getByRole('tab', { name: 'Lịch dạy' })).toBeVisible();
    await expect(page.getByText('Theo dõi lịch dạy, bài tập, thu nhập và trạng thái tài khoản.')).toBeVisible();
  }
  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    if (variant === 'after') {
      const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
      expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
      if (width <= 768) {
        const pagination = await page.locator('.tm-notifications__pagination').boundingBox();
        const bottomNavigation = await page.locator('.tm-workspace-bottom').boundingBox();
        expect(pagination).not.toBeNull();
        expect(bottomNavigation).not.toBeNull();
        expect(pagination!.y + pagination!.height).toBeLessThanOrEqual(bottomNavigation!.y + 1);
      }
    }
    await page.screenshot({ path: resolve(evidence, `teacher-notifications-${variant}-${width}.png`), fullPage: true });
  }
  if (variant === 'before') return;

  await page.setViewportSize({ width: 1280, height: 900 });
  const allTab = page.getByRole('tab', { name: 'Tất cả' });
  await allTab.focus();
  await page.keyboard.press('ArrowRight');
  await expect(page.getByRole('tab', { name: 'Chưa đọc' })).toHaveAttribute('aria-selected', 'true');
  expect(await page.evaluate(() => document.activeElement?.matches(':focus-visible') ?? false)).toBe(true);
  await page.keyboard.press('Home');
  await expect(allTab).toHaveAttribute('aria-selected', 'true');
  await page.getByRole('tab', { name: 'Lịch dạy' }).click();
  await expect.poll(() => requests.at(-1)?.searchParams.get('referenceType')).toBe('BOOKING');
  await page.getByRole('tab', { name: 'Chưa đọc' }).click();
  await expect.poll(() => requests.at(-1)?.searchParams.get('isRead')).toBe('false');
  await page.getByRole('tab', { name: 'Tất cả' }).click();
  await page.getByRole('listitem', { name: '2', exact: true }).click();
  await expect.poll(() => requests.at(-1)?.searchParams.get('page')).toBe('1');
  await page.getByRole('listitem', { name: '1', exact: true }).click();

  await expect(page.getByRole('link', { name: /Liên kết sai vai trò/ })).toHaveCount(0);
  await expect(page.getByRole('link', { name: /Đích ngoài hệ thống/ })).toHaveCount(0);
  readStatus = 503;
  await page.getByRole('button', { name: 'Đánh dấu đã đọc: Buổi dạy đã xác nhận' }).click();
  await expect(page.locator('.tm-notifications__item').filter({ hasText: 'Buổi dạy đã xác nhận' }).getByText('Chưa đọc', { exact: true })).toBeVisible();
  expect(readCalls).toEqual(['teacher-booking']);
  readAllStatus = 503;
  await page.getByRole('button', { name: 'Đánh dấu tất cả đã đọc' }).click();
  await expect(page.getByRole('button', { name: 'Đánh dấu tất cả đã đọc' })).toBeEnabled();
  readAllStatus = 200;
  await page.getByRole('button', { name: 'Đánh dấu tất cả đã đọc' }).click();
  await expect(page.getByRole('button', { name: 'Đánh dấu tất cả đã đọc' })).toBeDisabled();
  expect(readAllCalls).toBe(2);

  await page.getByRole('tab', { name: 'Hệ thống' }).click();
  await expect(page.getByRole('heading', { name: 'Không có thông báo trong mục này' })).toBeVisible();
  await expect(page.getByText('Cập nhật về lịch dạy, học viên và tài khoản sẽ xuất hiện tại đây.')).toBeVisible();
  await page.getByRole('tab', { name: 'Tất cả' }).click();
  listStatus = 503;
  await page.getByRole('tab', { name: 'Bài tập' }).click();
  await expect(page.getByText('Chưa tải được thông báo')).toBeVisible();
  listStatus = 200;
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.locator('.tm-notifications__title', { hasText: 'Buổi dạy đã xác nhận' })).toBeVisible();
  await page.goto('/teacher/notifications');
  await expect(page.locator('.tm-notifications__title', { hasText: 'Buổi dạy đã xác nhận' })).toBeVisible();
  readStatus = 503;
  await page.getByRole('button', { name: 'Đánh dấu đã đọc: Buổi dạy đã xác nhận' }).click();
  await expect(page.locator('.tm-notifications__item').filter({ hasText: 'Buổi dạy đã xác nhận' }).getByText('Chưa đọc', { exact: true })).toBeVisible();
  readStatus = 200;
  await page.getByRole('button', { name: 'Đánh dấu đã đọc: Buổi dạy đã xác nhận' }).click();
  await expect(page.locator('.tm-notifications__item').filter({ hasText: 'Buổi dạy đã xác nhận' }).getByText('Đã đọc')).toBeVisible();
  expect(readCalls).toEqual(['teacher-booking', 'teacher-booking', 'teacher-booking']);
  await page.getByRole('link', { name: /Buổi dạy đã xác nhận/ }).click();
  await expect(page).toHaveURL(/\/teacher\/bookings\/booking-1$/);
});
