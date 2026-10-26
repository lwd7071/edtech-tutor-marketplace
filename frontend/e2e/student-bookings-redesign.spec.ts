import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const student = { id: 'student-bookings-visual', email: 'student@example.test', fullName: 'An Nguyen', role: 'STUDENT', status: 'ACTIVE' };
const teacher = { id: 'teacher-bookings-visual', email: 'teacher@example.test', fullName: 'Ha Tran', role: 'TEACHER', status: 'ACTIVE' };

const bookings = [
  { id: 'booking-upcoming', teacher: { id: 'teacher-1', fullName: 'Trần Thu Hà' }, student: { id: 'student-1', fullName: 'Nguyễn Minh An' }, studentPackageId: 'package-1', subject: { id: 'subject-1', name: 'Toán 11' }, startTime: '2026-10-20T10:00:00Z', endTime: '2026-10-20T11:00:00Z', deliveryMode: 'ONLINE', meetingLink: 'https://meet.example.test/one', status: 'SCHEDULED', trial: false, outsideAvailabilityWarning: false, version: 1, settlementStatus: 'AWAITING_CONFIRMATION' },
  { id: 'booking-trial', teacher: { id: 'teacher-2', fullName: 'Lê Minh Khoa' }, student: { id: 'student-1', fullName: 'Nguyễn Minh An' }, studentPackageId: 'package-2', subject: { id: 'subject-2', name: 'Vật lý 12' }, startTime: '2026-10-22T09:00:00Z', endTime: '2026-10-22T10:00:00Z', deliveryMode: 'OFFLINE', locationAddress: 'Quận 3', status: 'SCHEDULED', trial: true, outsideAvailabilityWarning: false, version: 1, settlementStatus: null },
];

test('student bookings redesign preserves filters, pagination, links and teacher view', async ({ page }) => {
  const evidenceDirectory = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidenceDirectory, { recursive: true });
  let currentUser = student;
  let responseMode: 'data' | 'empty' = 'data';
  let apiStatus = 200;
  let apiDelay = 0;
  const requests: URL[] = [];

  await page.route('**/api/auth/login', route => route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({ success: true, message: 'ok', data: { accessToken: 'visual-access', refreshToken: 'visual-refresh', user: currentUser }, errors: null, meta: null }),
  }));
  await page.route('**/api/student/bookings**', async route => {
    requests.push(new URL(route.request().url()));
    if (apiDelay) await new Promise(resolveRequest => setTimeout(resolveRequest, apiDelay));
    await route.fulfill({
      status: apiStatus,
      contentType: 'application/json',
      body: JSON.stringify(apiStatus === 200
        ? { success: true, message: 'ok', data: responseMode === 'empty' ? [] : bookings, errors: null, meta: { page: Number(new URL(route.request().url()).searchParams.get('page') || 0), size: 12, totalElements: responseMode === 'empty' ? 0 : 13, totalPages: responseMode === 'empty' ? 0 : 2 } }
        : { success: false, message: 'bookings unavailable', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null }),
    });
  });
  await page.route('**/api/teacher/bookings**', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, message: 'ok', data: bookings, errors: null, meta: { page: 0, size: 12, totalElements: 2, totalPages: 1 } }),
    });
  });

  await page.setViewportSize({ width: 1280, height: 900 });
  await page.goto('/auth/login');
  await page.getByPlaceholder('Nhập email của bạn').fill(student.email);
  await page.getByPlaceholder('Nhập mật khẩu').fill('Password123!');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page).toHaveURL(/\/student$/);
  await page.goto('/student/bookings');
  await expect(page.getByText('Toán 11')).toBeVisible();

  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    if (variant === 'after') {
      await expect(page.locator('.tm-brand')).toHaveText('tutormatch');
      await expect(page.getByRole('heading', { name: 'Lịch học' })).toBeVisible();
      await expect(page.getByText('Trần Thu Hà')).toBeVisible();
      await expect(page.getByText('17:00, 20/10/2026')).toBeVisible();
      await expect(page.getByRole('link', { name: 'Xem chi tiết' }).first()).toHaveAttribute('href', '/student/bookings/booking-upcoming');
      await expect(page.getByText('Chờ xác nhận')).toBeVisible();
    }
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    if (variant === 'after') expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidenceDirectory, `student-bookings-${variant}-${width}.png`), fullPage: true });
  }
  if (variant === 'before') return;

  await page.setViewportSize({ width: 375, height: 900 });
  const finalDetailLink = page.getByRole('link', { name: 'Xem chi tiết' }).last();
  await finalDetailLink.scrollIntoViewIfNeeded();
  const mobileClearance = await page.evaluate(() => {
    const action = document.querySelector('.tm-student-bookings-results li:last-child .tm-student-booking-detail-link');
    const navigation = document.querySelector('.tm-workspace-bottom');
    if (!action || !navigation) return null;
    return { actionBottom: action.getBoundingClientRect().bottom, navigationTop: navigation.getBoundingClientRect().top };
  });
  expect(mobileClearance).not.toBeNull();
  expect(mobileClearance!.actionBottom).toBeLessThanOrEqual(mobileClearance!.navigationTop);
  const pagination = page.locator('.tm-student-bookings-pagination');
  await pagination.scrollIntoViewIfNeeded();
  await page.evaluate(() => window.scrollBy(0, 100));
  const paginationClearance = await page.evaluate(() => {
    const pageControl = document.querySelector('.tm-student-bookings-pagination');
    const navigation = document.querySelector('.tm-workspace-bottom');
    if (!pageControl || !navigation) return null;
    return { controlBottom: pageControl.getBoundingClientRect().bottom, navigationTop: navigation.getBoundingClientRect().top };
  });
  expect(paginationClearance).not.toBeNull();
  expect(paginationClearance!.controlBottom).toBeLessThanOrEqual(paginationClearance!.navigationTop);
  await page.setViewportSize({ width: 1280, height: 900 });
  await page.evaluate(() => window.scrollTo(0, 0));

  const detailLink = page.getByRole('link', { name: 'Xem chi tiết' }).first();
  await detailLink.focus();
  await expect(detailLink).toBeFocused();
  const statusFilter = page.getByLabel('Trạng thái buổi học');
  await statusFilter.focus();
  await statusFilter.press('ArrowDown');
  await statusFilter.press('Enter');
  await expect.poll(() => requests.at(-1)?.searchParams.get('status')).toBe('SCHEDULED');
  await page.getByLabel('Ngày học theo giờ Việt Nam').fill('2026-10-20');
  await expect.poll(() => requests.at(-1)?.searchParams.has('from')).toBe(true);
  await expect.poll(() => requests.at(-1)?.searchParams.has('to')).toBe(true);

  await page.locator('.ant-pagination-item-2').click();
  await expect.poll(() => requests.at(-1)?.searchParams.get('page')).toBe('1');
  await statusFilter.focus();
  await statusFilter.press('ArrowDown');
  await statusFilter.press('ArrowDown');
  await statusFilter.press('Enter');
  await expect.poll(() => requests.at(-1)?.searchParams.get('page')).toBe('0');
  await expect.poll(() => requests.at(-1)?.searchParams.get('status')).toBe('COMPLETED');

  responseMode = 'empty';
  await page.goto('/student/bookings');
  await expect(page.getByRole('heading', { name: 'Chưa có buổi học với điều kiện đã chọn.' })).toBeVisible();
  responseMode = 'data';
  apiStatus = 503;
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Chưa tải được lịch' })).toBeVisible();
  apiStatus = 200;
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByText('Toán 11')).toBeVisible();

  apiDelay = 1100;
  const loadingReload = page.reload();
  await expect(page.getByRole('status')).toContainText('Đang tải lịch học');
  await loadingReload;

  await page.getByRole('button', { name: 'Đăng xuất' }).click();
  currentUser = teacher;
  await page.goto('/auth/login');
  await page.getByPlaceholder('Nhập email của bạn').fill(teacher.email);
  await page.getByPlaceholder('Nhập mật khẩu').fill('Password123!');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page).toHaveURL(/\/teacher$/);
  await page.goto('/teacher/bookings');
  await expect(page.getByRole('heading', { name: 'Lịch dạy' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Chọn học viên để tạo buổi' })).toHaveAttribute('href', '/teacher/students');
  await expect(page.locator('.tm-package-grid .tm-panel')).toHaveCount(2);
});
