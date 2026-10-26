import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const student = { id: 'student-booking-detail-visual', email: 'student.detail@example.test', fullName: 'Nguyễn Minh An', role: 'STUDENT', status: 'ACTIVE' };
const teacher = { id: 'teacher-booking-detail-visual', email: 'teacher.detail@example.test', fullName: 'Trần Thu Hà', role: 'TEACHER', status: 'ACTIVE' };
const baseBooking = {
  id: 'booking-detail-visual', teacher: { id: 'teacher-1', fullName: 'Trần Thu Hà' }, student: { id: 'student-1', fullName: 'Nguyễn Minh An' },
  studentPackageId: 'package-1', subject: { id: 'subject-1', name: 'Toán 11' }, startTime: '2026-10-20T10:00:00Z', endTime: '2026-10-20T11:00:00Z',
  deliveryMode: 'ONLINE', meetingLink: 'https://meet.example.test/session', status: 'SCHEDULED', trial: false, outsideAvailabilityWarning: true,
  version: 7, canConfirm: true, canReview: false, settlementStatus: 'AWAITING_CONFIRMATION',
  settlement: { status: 'AWAITING_CONFIRMATION', teacherConfirmedAt: null, studentConfirmedAt: null, confirmationDeadline: '2026-10-21T11:00:00Z', netAmountVnd: 450000, version: 2 },
  sessionReport: { content: 'Ôn phương trình bậc hai\nLuyện thêm dạng có tham số.', feedback: 'Nắm kiến thức tốt.', followUpNote: 'Làm bài tập trang 42.', teacherSelfRating: 5 },
};
const envelope = (data: unknown) => ({ success: true, message: 'ok', data, errors: null, meta: null });

async function login(page: import('@playwright/test').Page, user: typeof student | typeof teacher) {
  await page.route('**/api/auth/login', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope({ accessToken: 'visual-access', refreshToken: 'visual-refresh', user })) }));
  await page.goto('/auth/login');
  await page.getByPlaceholder('Nhập email của bạn').fill(user.email);
  await page.getByPlaceholder('Nhập mật khẩu').fill('Password123!');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
}

test('student booking detail matches spec at desktop/tablet/mobile and retains student actions', async ({ page }) => {
  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  let item: Record<string, any> = { ...baseBooking };
  let apiStatus = 200;
  let delay = 0;
  let reviewData: unknown = null;
  const requests: Array<{ url: string; method: string; body?: unknown }> = [];
  await page.route('**/api/student/bookings/booking-detail-visual/review', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(reviewData)) }));
  await page.route('**/api/student/bookings/booking-detail-visual/confirm', async route => {
    requests.push({ url: route.request().url(), method: route.request().method(), body: route.request().postDataJSON() });
    item = { ...item, canConfirm: false, settlement: { ...item.settlement, studentConfirmedAt: '2026-10-20T11:05:00Z' } };
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(item)) });
  });
  await page.route('**/api/student/bookings/booking-detail-visual', async route => {
    if (delay) await new Promise(resolveDelay => setTimeout(resolveDelay, delay));
    await route.fulfill({ status: apiStatus, contentType: 'application/json', body: JSON.stringify(apiStatus === 200 ? envelope(item) : { success: false, message: 'unavailable', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null }) });
  });
  await login(page, student);
  await expect(page).toHaveURL(/\/student$/);
  await page.goto('/student/bookings/booking-detail-visual');
  await expect(page.getByRole('heading', { name: 'Toán 11' })).toBeVisible();
  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    if (variant === 'after') {
      await expect(page.getByRole('heading', { name: 'Thông tin buổi học' })).toBeVisible();
      await expect(page.getByRole('link', { name: 'Mở phòng học' })).toHaveAttribute('href', item.meetingLink);
      await expect(page.getByRole('heading', { name: 'Báo cáo buổi học' })).toBeVisible();
      await expect(page.getByText('450.000 ₫')).toBeVisible();
      const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
      expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    }
    await page.screenshot({ path: resolve(evidence, `student-booking-detail-${variant}-${width}.png`), fullPage: true });
  }
  if (variant === 'before') return;

  await expect(page.getByRole('button', { name: 'Xác nhận buổi học' })).toBeVisible();
  for (const width of [375, 768]) {
    await page.setViewportSize({ width, height: 900 });
    await page.getByRole('button', { name: 'Xác nhận buổi học' }).scrollIntoViewIfNeeded();
    const actionClearance = await page.evaluate(() => ({
      actionBottom: document.querySelector('.tm-booking-actions button:last-of-type')?.getBoundingClientRect().bottom ?? 0,
      navTop: document.querySelector('.tm-workspace-bottom')?.getBoundingClientRect().top ?? Number.POSITIVE_INFINITY,
    }));
    expect(actionClearance.actionBottom).toBeLessThanOrEqual(actionClearance.navTop);
  }
  await page.getByRole('button', { name: 'Xác nhận buổi học' }).focus();
  await expect(page.getByRole('button', { name: 'Xác nhận buổi học' })).toBeFocused();
  await page.getByRole('button', { name: 'Xác nhận buổi học' }).click();
  await expect(page.getByText('Đã xác nhận buổi học.', { exact: true })).toBeVisible();
  expect(requests).toContainEqual(expect.objectContaining({ method: 'POST', body: { version: 7 } }));

  item = { ...baseBooking, status: 'CANCELLED', canConfirm: false, cancelReason: 'Gia sư báo bận.\nĐã thống nhất đổi lịch.' };
  await page.reload();
  await expect(page.getByText('Gia sư báo bận.\nĐã thống nhất đổi lịch.')).toBeVisible();
  expect(await page.getByRole('button', { name: 'Xác nhận buổi học' }).count()).toBe(0);
  item = { ...baseBooking, status: 'CANCELLED', cancelReason: null, canConfirm: false, meetingLink: 'javascript:alert(1)' };
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Toán 11' })).toBeVisible();
  expect(await page.getByRole('link', { name: 'Mở phòng học' }).count()).toBe(0);

  reviewData = null;
  item = { ...baseBooking, canConfirm: false, canReview: true };
  await page.reload();
  await expect(page.getByRole('button', { name: 'Đánh giá buổi học' })).toBeVisible();
  reviewData = { id: 'review-1', rating: 5, comment: 'Rất dễ hiểu', createdAt: '2026-10-21T00:00:00Z' };
  await page.reload();
  await expect(page.getByText('Bạn đã đánh giá')).toBeVisible();
  expect(await page.getByRole('button', { name: 'Đánh giá buổi học' }).count()).toBe(0);

  apiStatus = 503;
  await page.reload();
  await expect(page.getByText('Chưa đọc được buổi học')).toBeVisible();
  apiStatus = 200;
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByRole('heading', { name: 'Toán 11' })).toBeVisible();

  delay = 1200;
  const pendingNavigation = page.reload({ waitUntil: 'commit' });
  await expect(page.getByRole('status', { name: 'Đang tải chi tiết buổi học' })).toBeVisible();
  await pendingNavigation;
  await expect(page.getByRole('heading', { name: 'Toán 11' })).toBeVisible();
});

test('teacher booking detail retains its existing role-specific actions and layout', async ({ browser }) => {
  const context = await browser.newContext();
  const page = await context.newPage();
  await page.route('**/api/auth/login', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope({ accessToken: 'visual-access', refreshToken: 'visual-refresh', user: teacher })) }));
  await page.route('**/api/teacher/bookings/booking-detail-visual', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope({ ...baseBooking, canConfirm: false, canDispute: true })) }));
  await page.goto('/auth/login');
  await page.getByPlaceholder('Nhập email của bạn').fill(teacher.email);
  await page.getByPlaceholder('Nhập mật khẩu').fill('Password123!');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page).toHaveURL(/\/teacher$/);
  await page.goto('/teacher/bookings/booking-detail-visual');
  await expect(page.getByRole('heading', { name: 'Toán 11' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Hủy buổi học' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Khiếu nại khoản tiền đang giữ' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Nội dung buổi học' }).locator('..').getByText('Ôn phương trình bậc hai', { exact: false })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Nội dung buổi học' }).locator('..').getByText('Luyện thêm dạng có tham số.', { exact: false })).toBeVisible();
  expect(await page.locator('.tm-booking-detail').count()).toBe(0);
  expect(await page.getByRole('button', { name: 'Xác nhận buổi học' }).count()).toBe(0);
  await context.close();
});
