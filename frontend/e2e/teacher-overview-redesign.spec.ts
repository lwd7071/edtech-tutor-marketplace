import { expect, test, type Page } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const teacher = { id: 'teacher-overview-fixture', email: 'teacher-overview@example.test', fullName: 'Nguyễn Minh An', role: 'TEACHER', status: 'ACTIVE' };
const envelope = (data: unknown, meta: unknown = null) => ({ success: true, message: 'ok', data, errors: null, meta });
const failed = { success: false, message: 'Dữ liệu tạm thời chưa tải được', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null };
const booking = {
  id: 'booking-next-lesson', teacher: { id: teacher.id, fullName: teacher.fullName },
  student: { id: 'student-fixture', fullName: 'Lê Minh Khang' }, studentPackageId: 'package-fixture',
  subject: { id: 'subject-math', name: 'Toán' }, startTime: '2026-10-20T03:00:00Z', endTime: '2026-10-20T04:00:00Z',
  deliveryMode: 'ONLINE', meetingLink: null, locationAddress: null, status: 'SCHEDULED', trial: false,
  outsideAvailabilityWarning: false, version: 1,
};
const trials = [
  { id: 'trial-one', teacherId: teacher.id, studentId: 'student-one', subjectId: 'subject-math', preferredStartTime: '2026-10-21T04:00:00Z', note: 'Muốn ôn phương trình.', status: 'PENDING', createdAt: '2026-10-15T04:00:00Z', version: 0 },
  { id: 'trial-two', teacherId: teacher.id, studentId: 'student-two', subjectId: 'subject-english', preferredStartTime: '2026-10-22T05:00:00Z', note: 'Luyện giao tiếp.', status: 'PENDING', createdAt: '2026-10-15T05:00:00Z', version: 0 },
];
const assignments = [
  { id: 'assignment-one', title: 'Phương trình bậc hai', studentName: 'Lê Minh Khang', subjectName: 'Toán', dueAt: '2026-10-19T16:59:00Z', status: 'PUBLISHED', submittedCount: 2, totalStudents: 3 },
  { id: 'assignment-two', title: 'Hội thoại hằng ngày', studentName: 'Phạm Bảo An', subjectName: 'Tiếng Anh', dueAt: '2026-10-22T16:59:00Z', status: 'PUBLISHED', submittedCount: 1, totalStudents: 1 },
];
const wallet = { id: 'wallet-fixture', teacherId: teacher.id, pendingBalanceVnd: 125000, availableBalanceVnd: 1250000, reservedBalanceVnd: 0, heldBalanceVnd: 350000, version: 1 };

type ProfileStatus = 'DRAFT' | 'PENDING_APPROVAL' | 'REJECTED' | 'APPROVED';

async function mockDashboard(page: Page, options: { profileStatus?: ProfileStatus; empty?: boolean; failWalletUntilRetry?: boolean; holdWalletUntilRelease?: boolean } = {}) {
  const requests: URL[] = [];
  let walletError = options.failWalletUntilRetry === true;
  let resolveWalletGate: (() => void) | undefined;
  const walletGate = options.holdWalletUntilRelease ? new Promise<void>(resolve => { resolveWalletGate = resolve; }) : null;

  await page.route('**/api/auth/login', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope({ accessToken: 'teacher-overview-access', refreshToken: 'teacher-overview-refresh', user: teacher })) }));
  await page.route('**/api/teacher/profile', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope({ profileStatus: options.profileStatus ?? 'PENDING_APPROVAL', rejectionReason: options.profileStatus === 'REJECTED' ? 'Cần bổ sung nội dung giới thiệu.' : null })) }));
  await page.route('**/api/teacher/bookings**', route => {
    requests.push(new URL(route.request().url()));
    const data = options.empty ? [] : [booking];
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(data, { page: 0, size: 12, totalElements: data.length, totalPages: data.length ? 1 : 0, hasNext: false })) });
  });
  await page.route('**/api/teacher/trial-requests**', route => {
    requests.push(new URL(route.request().url()));
    const data = options.empty ? [] : trials;
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(data, { page: 0, size: 3, totalElements: data.length, totalPages: data.length ? 1 : 0, hasNext: false })) });
  });
  await page.route('**/api/teacher/assignments**', route => {
    requests.push(new URL(route.request().url()));
    const data = options.empty ? [] : assignments;
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(data, { page: 0, size: 8, totalElements: data.length, totalPages: data.length ? 1 : 0, hasNext: false })) });
  });
  await page.route('**/api/teacher/wallet', async route => {
    requests.push(new URL(route.request().url()));
    if (walletGate) await walletGate;
    if (walletError) {
      return route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify(failed) });
    }
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(wallet)) });
  });
  return { requests, recoverWallet: () => { walletError = false; }, releaseWallet: () => resolveWalletGate?.() };
}

async function signInAsTeacher(page: Page) {
  await page.goto('/auth/login');
  await page.getByPlaceholder('Nhập email của bạn').fill(teacher.email);
  await page.getByPlaceholder('Nhập mật khẩu').fill('Password123!');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page).toHaveURL(/\/teacher$/);
}

test('teacher overview preserves live dashboard data and links across responsive layouts', async ({ page }) => {
  const { requests } = await mockDashboard(page);
  await signInAsTeacher(page);
  await expect(page.getByRole('heading', { name: 'Hôm nay bạn cần làm gì?' })).toBeVisible();
  await page.keyboard.press('Tab');
  await expect(page.locator(':focus-visible').first()).toBeVisible();
  await expect(page.getByText('Hồ sơ đang chờ xét duyệt')).toBeVisible();
  await expect(page.getByText('Lê Minh Khang').first()).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Yêu cầu học thử' })).toBeVisible();
  await expect(page.getByText('Phương trình bậc hai')).toBeVisible();
  await expect(page.getByText('1.250.000 ₫')).toBeVisible();

  const byPath = (path: string) => requests.find(request => request.pathname === path);
  expect(byPath('/api/teacher/bookings')?.searchParams.toString()).toBe('status=SCHEDULED&page=0&size=12&sort=startTime%2Casc');
  expect(byPath('/api/teacher/trial-requests')?.searchParams.toString()).toBe('status=PENDING&page=0&size=3');
  expect(byPath('/api/teacher/assignments')?.searchParams.toString()).toBe('page=0&size=8');
  expect(requests.some(request => request.pathname === '/api/teacher/wallet')).toBe(true);

  await expect(page.getByRole('link', { name: 'Tiếp tục hồ sơ' })).toHaveAttribute('href', '/teacher/profile');
  await expect(page.getByRole('link', { name: 'Xem và phản hồi yêu cầu' })).toHaveAttribute('href', '/teacher/trial-requests');
  await expect(page.getByRole('link', { name: 'Xem ví thu nhập' })).toHaveAttribute('href', '/teacher/wallet');

  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidence, `teacher-overview-${variant}-${width}.png`), fullPage: true });
  }

  if (variant === 'after') {
    await page.setViewportSize({ width: 375, height: 900 });
    const finalAction = page.getByRole('link', { name: 'Xem ví thu nhập' });
    await finalAction.scrollIntoViewIfNeeded();
    const actionRect = await finalAction.boundingBox();
    const mobileNavRect = await page.getByRole('navigation', { name: 'Điều hướng nhanh' }).boundingBox();
    expect(actionRect).not.toBeNull();
    expect(mobileNavRect).not.toBeNull();
    expect(actionRect!.y + actionRect!.height).toBeLessThanOrEqual(mobileNavRect!.y + 1);
  }
});

for (const [status, label, action] of [
  ['DRAFT', 'Hồ sơ chưa gửi xét duyệt', 'Tiếp tục hồ sơ'],
  ['PENDING_APPROVAL', 'Hồ sơ đang chờ xét duyệt', 'Tiếp tục hồ sơ'],
  ['REJECTED', 'Hồ sơ cần chỉnh sửa', 'Tiếp tục hồ sơ'],
  ['APPROVED', 'Hồ sơ đã được duyệt', 'Xem hồ sơ'],
] as const) {
  test(`teacher profile status ${status} keeps its matching action`, async ({ page }) => {
    await mockDashboard(page, { profileStatus: status, empty: true });
    await signInAsTeacher(page);
    await expect(page.getByText(label)).toBeVisible();
    await expect(page.getByRole('link', { name: action })).toHaveAttribute('href', '/teacher/profile');
    if (status === 'REJECTED') await expect(page.getByText('Cần bổ sung nội dung giới thiệu.')).toBeVisible();
  });
}

test('teacher dashboard keeps API errors local and retries without showing a zero balance', async ({ page }) => {
  const dashboard = await mockDashboard(page, { failWalletUntilRetry: true });
  await signInAsTeacher(page);
  await expect(page.getByRole('heading', { name: 'Hôm nay bạn cần làm gì?' })).toBeVisible();
  await expect(page.getByText('Chưa tải được ví')).toBeVisible();
  await expect(page.getByText('Lê Minh Khang').first()).toBeVisible();
  dashboard.recoverWallet();
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByText('1.250.000 ₫')).toBeVisible();
});

test('teacher dashboard explains when task lists are empty', async ({ page }) => {
  await mockDashboard(page, { empty: true });
  await signInAsTeacher(page);
  await expect(page.getByText('Chưa có buổi học sắp tới.', { exact: false }).first()).toBeVisible();
  await expect(page.getByText('Không có yêu cầu học thử đang chờ.')).toBeVisible();
  await expect(page.getByText('Chưa có bài nộp trong các bài tập gần đây.')).toBeVisible();
});

test('teacher dashboard announces loading and then renders wallet data', async ({ page }) => {
  const dashboard = await mockDashboard(page, { holdWalletUntilRelease: true });
  await signInAsTeacher(page);
  await expect(page.getByRole('status').filter({ hasText: 'Đang tải số dư' })).toBeVisible();
  dashboard.releaseWallet();
  await expect(page.getByText('1.250.000 ₫')).toBeVisible();
});
