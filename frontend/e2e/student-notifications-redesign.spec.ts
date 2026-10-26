import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const student = { id: 'notification-student', email: 'notify@example.test', fullName: 'Nguyễn Minh An', role: 'STUDENT', status: 'ACTIVE' };
const teacher = { id: 'notification-teacher', email: 'teacher-notify@example.test', fullName: 'Trần Thu Hà', role: 'TEACHER', status: 'ACTIVE' };
const makeNotification = (id: string, title: string, referenceType: string, referenceUrl: string | null, isRead = false) => ({
  id, userId: student.id, type: referenceType, title, content: `Nội dung chi tiết cho ${title.toLowerCase()}.`, isRead,
  referenceId: `reference-${id}`, referenceType, referenceUrl, createdAt: '2026-10-16T08:30:00Z',
});
const notifications = [
  makeNotification('booking-1', 'Lịch học đã xác nhận', 'BOOKING', '/student/bookings'),
  makeNotification('assignment-1', 'Bài tập mới', 'ASSIGNMENT', null, true),
  makeNotification('external-1', 'Cập nhật bên ngoài', 'SYSTEM', 'https://outside.example.test/notice'),
];
const envelope = (data: unknown, meta: unknown = null) => ({ success: true, message: 'ok', data, errors: null, meta });

test('student notifications redesign preserves filters, read actions, safe links and pagination', async ({ page }) => {
  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  let listStatus = 200;
  let readStatus = 200;
  let readAllStatus = 200;
  const requests: URL[] = [];
  const readCalls: string[] = [];
  let readAllCalls = 0;
  await page.addInitScript(user => {
    localStorage.setItem('tutor-match.session', JSON.stringify({ user, remember: false, sessionId: 'student-notification-session' }));
    document.cookie = 'accessToken=student-notification-access; path=/; SameSite=Lax';
  }, student);
  await page.route('**/api/notifications**', async route => {
    const url = new URL(route.request().url());
    if (route.request().method() === 'GET') {
      requests.push(url);
      const currentPage = Number(url.searchParams.get('page') || 0);
      const referenceType = url.searchParams.get('referenceType');
      const isEmpty = referenceType === 'SYSTEM';
      const data = isEmpty ? [] : currentPage === 0 ? notifications : [makeNotification('older-1', 'Thông báo trước đó', 'PROFILE', null, true)];
      const totalPages = isEmpty ? 0 : 2;
      await route.fulfill({ status: listStatus, contentType: 'application/json', body: JSON.stringify(listStatus === 200 ? envelope(data, { page: currentPage, size: 20, totalElements: 21, totalPages, hasNext: currentPage + 1 < totalPages }) : { success: false, message: 'unavailable', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null }) });
      return;
    }
    if (route.request().method() === 'PATCH') {
      const id = url.pathname.split('/').at(-2) || '';
      readCalls.push(id);
      await route.fulfill({ status: readStatus, contentType: 'application/json', body: JSON.stringify(readStatus === 200 ? envelope({ ...notifications.find(item => item.id === id), isRead: true }) : { success: false, message: 'unavailable', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null }) });
      return;
    }
    readAllCalls += 1;
    await route.fulfill({ status: readAllStatus, contentType: 'application/json', body: JSON.stringify(readAllStatus === 200 ? envelope({ updatedCount: 2 }) : { success: false, message: 'unavailable', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null }) });
  });

  await page.goto('/student/notifications');
  await expect(page.getByRole('heading', { name: 'Thông báo' })).toBeVisible();
  if (variant === 'before') await expect(page.getByText('Lịch học đã xác nhận', { exact: true })).toBeVisible();
  else await expect(page.locator('.tm-notifications__title', { hasText: 'Lịch học đã xác nhận' })).toBeVisible();

  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    if (variant === 'after') {
      const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
      expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    }
    await page.screenshot({ path: resolve(evidence, `student-notifications-${variant}-${width}.png`), fullPage: true });
  }
  if (variant === 'before') return;

  await page.setViewportSize({ width: 375, height: 900 });
  await page.getByRole('tab', { name: 'Hệ thống' }).scrollIntoViewIfNeeded();
  await page.getByRole('tab', { name: 'Hệ thống' }).click();
  await expect(page.getByRole('heading', { name: 'Không có thông báo trong mục này' })).toBeVisible();
  await page.locator('.tm-notifications__tabs').evaluate(element => { element.scrollLeft = 0; });
  await page.getByRole('tab', { name: 'Tất cả' }).click();
  await page.setViewportSize({ width: 1280, height: 900 });

  await page.getByRole('tab', { name: 'Lịch học' }).click();
  await expect.poll(() => requests.at(-1)?.searchParams.get('referenceType')).toBe('BOOKING');
  await page.getByRole('tab', { name: 'Chưa đọc' }).click();
  await expect.poll(() => requests.at(-1)?.searchParams.get('isRead')).toBe('false');
  await expect.poll(() => requests.at(-1)?.searchParams.get('page')).toBe('0');
  await page.getByRole('tab', { name: 'Tất cả' }).click();

  await page.getByRole('listitem', { name: '2', exact: true }).click();
  await expect.poll(() => requests.at(-1)?.searchParams.get('page')).toBe('1');
  await expect(page.locator('.tm-notifications__title', { hasText: 'Thông báo trước đó' })).toBeVisible();
  await page.getByRole('listitem', { name: '1', exact: true }).click();
  await expect.poll(() => requests.at(-1)?.searchParams.get('page')).toBe('0');

  const external = page.locator('.tm-notifications__title', { hasText: 'Cập nhật bên ngoài' });
  await expect(external).toBeVisible();
  await expect(page.getByRole('link', { name: /Cập nhật bên ngoài/ })).toHaveCount(0);

  readStatus = 503;
  await page.getByRole('button', { name: 'Đánh dấu đã đọc: Lịch học đã xác nhận' }).click();
  await expect(page.locator('.tm-notifications__item').filter({ hasText: 'Lịch học đã xác nhận' }).getByText('Chưa đọc', { exact: true })).toBeVisible();
  readStatus = 200;
  await page.getByRole('button', { name: 'Đánh dấu đã đọc: Lịch học đã xác nhận' }).click();
  await expect(page.locator('.tm-notifications__item').filter({ hasText: 'Lịch học đã xác nhận' }).getByText('Đã đọc')).toBeVisible();
  expect(readCalls).toEqual(['booking-1', 'booking-1']);

  readAllStatus = 503;
  await page.getByRole('button', { name: 'Đánh dấu tất cả đã đọc' }).click();
  await expect(page.getByRole('button', { name: 'Đánh dấu tất cả đã đọc' })).toBeEnabled();
  readAllStatus = 200;
  await page.getByRole('button', { name: 'Đánh dấu tất cả đã đọc' }).click();
  await expect(page.getByRole('button', { name: 'Đánh dấu tất cả đã đọc' })).toBeDisabled();
  expect(readAllCalls).toBe(2);

  await page.getByRole('tab', { name: 'Hệ thống' }).click();
  await expect(page.getByRole('heading', { name: 'Không có thông báo trong mục này' })).toBeVisible();
  await page.getByRole('tab', { name: 'Tất cả' }).click();
  listStatus = 503;
  await page.getByRole('tab', { name: 'Bài tập' }).click();
  await expect(page.getByText('Chưa tải được thông báo')).toBeVisible();
  listStatus = 200;
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.locator('.tm-notifications__title', { hasText: 'Bài tập mới' })).toBeVisible();
  await page.keyboard.press('Tab');
  expect(await page.evaluate(() => document.activeElement?.matches(':focus-visible') ?? false)).toBe(true);

  await page.goto('/student/notifications');
  await expect(page.locator('.tm-notifications__title', { hasText: 'Lịch học đã xác nhận' })).toBeVisible();
  await page.getByRole('link', { name: /Lịch học đã xác nhận/ }).click();
  await expect(page).toHaveURL(/\/student\/bookings$/);
});

test('teacher notifications route keeps the shared list inside Teacher workspace', async ({ page }) => {
  await page.route('**/api/auth/login', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope({ accessToken: 'visual-teacher-access', refreshToken: 'visual-teacher-refresh', user: teacher })) }));
  await page.route('**/api/notifications**', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope([{ ...notifications[0], userId: teacher.id, referenceUrl: '/teacher/bookings' }], { page: 0, size: 20, totalElements: 1, totalPages: 1, hasNext: false })) }));
  await page.goto('/auth/login');
  await page.getByPlaceholder('Nhập email của bạn').fill(teacher.email);
  await page.getByPlaceholder('Nhập mật khẩu').fill('Password123!');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page).toHaveURL(/\/teacher$/);
  await page.goto('/teacher/notifications');
  await expect(page.getByRole('heading', { name: 'Thông báo' })).toBeVisible();
  await expect(page.locator('.tm-notifications__title', { hasText: 'Lịch học đã xác nhận' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Thông báo' }).first()).toHaveAttribute('href', '/teacher/notifications');
});
