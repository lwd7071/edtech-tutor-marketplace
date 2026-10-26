import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const student = { id: 'session-report-student', email: 'reports@example.test', fullName: 'Nguyễn Minh An', role: 'STUDENT', status: 'ACTIVE' };
const envelope = (data: unknown, meta: unknown = null) => ({ success: true, message: 'ok', data, errors: null, meta });
const rejected = { success: false, message: 'unavailable', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null };
const reports = [
  { id: 'report-1', bookingId: 'booking-1', teacherId: 'teacher-1', subjectId: 'subject-1', startTime: '2026-10-20T03:30:00Z', endTime: '2026-10-20T04:20:00Z', deliveryMode: 'ONLINE', recordLink: 'https://recordings.example.test/lesson-1', content: 'Luyện cách lập và giải phương trình bậc hai.\nĐã hoàn thành ba dạng bài.', feedback: 'Em trình bày từng bước rõ ràng và đã tự kiểm tra kết quả.', followUpNote: 'Hoàn thành bài tập số 4 trước buổi tiếp theo.', teacherSelfRating: 5, submittedAt: '2026-10-20T05:00:00Z' },
  { id: 'report-2', bookingId: 'booking-2', teacherId: 'teacher-1', subjectId: 'subject-1', startTime: '2026-10-18T03:30:00Z', endTime: '2026-10-18T04:20:00Z', deliveryMode: 'ONLINE', recordLink: 'javascript:alert(1)', content: 'Ôn tập đồ thị hàm số.', feedback: 'Tiếp tục luyện tập cách xác định giao điểm.', followUpNote: '', teacherSelfRating: 5, submittedAt: '2026-10-18T05:00:00Z' },
];

test('student session reports redesign preserves report data, links, pagination and states', async ({ page }) => {
  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  let getStatus = 200;
  let forceEmpty = false;
  let holdInitialGet = true;
  let releaseInitialGet!: () => void;
  const requests: URL[] = [];
  await page.route('**/api/auth/login', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope({ accessToken: 'visual-report-access', refreshToken: 'visual-report-refresh', user: student })) }));
  await page.route('**/api/student/session-reports**', async route => {
    const url = new URL(route.request().url());
    requests.push(url);
    if (holdInitialGet) {
      holdInitialGet = false;
      await new Promise<void>(resolveGet => { releaseInitialGet = resolveGet; });
    }
    if (getStatus !== 200) return route.fulfill({ status: getStatus, contentType: 'application/json', body: JSON.stringify(rejected) });
    const currentPage = Number(url.searchParams.get('page') || 0);
    const data = forceEmpty ? [] : [reports[currentPage] ?? reports[1]];
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(data, { page: currentPage, size: 10, totalElements: forceEmpty ? 0 : 11, totalPages: forceEmpty ? 0 : 2, hasNext: !forceEmpty && currentPage === 0 })) });
  });

  await page.goto('/auth/login');
  await page.getByPlaceholder('Nhập email của bạn').fill(student.email);
  await page.getByPlaceholder('Nhập mật khẩu').fill('Password123!');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page).toHaveURL(/\/student$/);
  await page.goto('/student/session-reports');
  if (variant === 'after') await expect(page.getByRole('status', { name: 'Đang tải báo cáo buổi học' })).toBeVisible();
  expect(requests.at(-1)?.searchParams.get('size')).toBe('10');
  releaseInitialGet();
  await expect(page.getByRole('heading', { name: 'Báo cáo buổi học' })).toBeVisible();
  await expect(page.getByText('Luyện cách lập và giải phương trình bậc hai.', { exact: false })).toBeVisible();

  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    if (variant === 'after') {
      const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
      expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    }
    await page.screenshot({ path: resolve(evidence, `student-session-reports-${variant}-${width}.png`), fullPage: true });
  }
  if (variant === 'before') return;

  await page.setViewportSize({ width: 1280, height: 900 });
  const recordLink = page.getByRole('link', { name: 'Xem bản ghi' });
  await expect(recordLink).toHaveAttribute('href', 'https://recordings.example.test/lesson-1');
  await expect(recordLink).toHaveAttribute('target', '_blank');
  await expect(recordLink).toHaveAttribute('rel', 'noopener noreferrer');
  await expect(page.getByRole('link', { name: 'Mở chi tiết buổi học' })).toHaveAttribute('href', '/student/bookings/booking-1');
  await expect(page.getByRole('link', { name: /javascript:/ })).toHaveCount(0);
  await expect(page.getByRole('heading', { name: 'Việc cần làm tiếp' })).toBeVisible();
  await page.setViewportSize({ width: 375, height: 812 });
  const bookingLink = page.getByRole('link', { name: 'Mở chi tiết buổi học' });
  await bookingLink.scrollIntoViewIfNeeded();
  const bookingLinkBottom = await bookingLink.evaluate(element => element.getBoundingClientRect().bottom);
  const bottomNavigationTop = await page.getByRole('navigation', { name: 'Điều hướng nhanh' }).evaluate(element => element.getBoundingClientRect().top);
  expect(bookingLinkBottom).toBeLessThanOrEqual(bottomNavigationTop);
  await page.setViewportSize({ width: 1280, height: 900 });
  await page.evaluate(() => window.scrollTo(0, 0));

  await page.getByRole('listitem', { name: '2', exact: true }).click();
  await expect.poll(() => requests.at(-1)?.searchParams.get('page')).toBe('1');
  await expect(page.getByText('Ôn tập đồ thị hàm số.')).toBeVisible();
  await expect(page.getByRole('link', { name: 'Xem bản ghi' })).toHaveCount(0);

  getStatus = 503;
  await page.reload();
  await expect(page.getByText('Chưa tải được báo cáo buổi học')).toBeVisible();
  getStatus = 200;
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByText('Luyện cách lập và giải phương trình bậc hai.', { exact: false })).toBeVisible();

  forceEmpty = true;
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Chưa có báo cáo buổi học' })).toBeVisible();
  const detailLink = page.getByRole('link', { name: 'Mở chi tiết buổi học' });
  await expect(detailLink).toHaveCount(0);
  await page.setViewportSize({ width: 375, height: 812 });
  await page.keyboard.press('Tab');
  expect(await page.evaluate(() => document.activeElement?.matches(':focus-visible') ?? false)).toBe(true);
});
