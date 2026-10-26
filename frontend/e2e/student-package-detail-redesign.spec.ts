import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const student = { id: 'student-package-detail-visual', email: 'student@example.test', fullName: 'An Nguyen', role: 'STUDENT', status: 'ACTIVE' };
const activePackage = {
  id: 'pkg-visual', teacher: { id: 'teacher-visual', fullName: 'Trần Thu Hà' }, subject: { id: 'subject-visual', name: 'Toán 11' }, packageName: 'Toán 11 — 10 buổi', totalSessions: 10, remainingSessions: 7, reservedSessions: 1, completedSessions: 2, refundedSessions: 0, purchasePriceVnd: 1000000, startsAt: '2026-08-19T02:00:00Z', expiresAt: '2026-11-17T02:00:00Z', status: 'ACTIVE', lockedReason: null, version: 4, description: 'Ôn tập đại số, hàm số và các dạng bài trọng tâm của chương trình Toán 11.', sessionDurationMinutes: 60,
};

test('student package detail preserves owned snapshot and request flows in the approved responsive layout', async ({ page }) => {
  const evidenceDirectory = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidenceDirectory, { recursive: true });
  let packageData = { ...activePackage };
  let apiStatus = 200;
  let apiDelay = 0;
  let refundPayload: Record<string, unknown> | undefined;
  let extensionPayload: Record<string, unknown> | undefined;

  await page.route('**/api/auth/login', route => route.fulfill({
    status: 200, contentType: 'application/json',
    body: JSON.stringify({ success: true, message: 'ok', data: { accessToken: 'visual-access', refreshToken: 'visual-refresh', user: student }, errors: null, meta: null }),
  }));
  await page.route('**/api/student/packages/pkg-visual', async route => {
    if (apiDelay) await new Promise(resolveRequest => setTimeout(resolveRequest, apiDelay));
    await route.fulfill({
      status: apiStatus,
      contentType: 'application/json',
      body: JSON.stringify(apiStatus === 200
        ? { success: true, message: 'ok', data: packageData, errors: null, meta: null }
        : { success: false, message: 'not available', data: null, errors: [{ code: 'NOT_FOUND', message: 'unavailable' }], meta: null }),
    });
  });
  await page.route('**/api/student/refund-requests', async route => {
    if (route.request().method() === 'POST') {
      refundPayload = route.request().postDataJSON();
      await route.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify({ success: true, message: 'ok', data: { id: 'refund-1' }, errors: null, meta: null }) });
      return;
    }
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, message: 'ok', data: [], errors: null, meta: { page: 0, size: 10, totalElements: 0, totalPages: 0 } }) });
  });
  await page.route('**/api/student/extension-requests', async route => {
    if (route.request().method() === 'POST') {
      extensionPayload = route.request().postDataJSON();
      await route.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify({ success: true, message: 'ok', data: { id: 'extension-1' }, errors: null, meta: null }) });
      return;
    }
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, message: 'ok', data: [], errors: null, meta: { page: 0, size: 10, totalElements: 0, totalPages: 0 } }) });
  });
  await page.route('**/api/student/trials/requests**', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, message: 'ok', data: [], errors: null, meta: { page: 0, size: 10, totalElements: 0, totalPages: 0 } }) }));
  await page.route('**/api/student/conversations/teachers/teacher-visual', route => route.fulfill({ status: 409, contentType: 'application/json', body: JSON.stringify({ success: false, errors: [{ code: 'CONVERSATION_NOT_ALLOWED' }] }) }));

  await page.setViewportSize({ width: 1280, height: 900 });
  await page.goto('/auth/login');
  await page.getByPlaceholder('Nhập email của bạn').fill(student.email);
  await page.getByPlaceholder('Nhập mật khẩu').fill('Password123!');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page).toHaveURL(/\/student$/);
  await page.goto('/student/packages/pkg-visual');
  await expect(page.getByRole('heading', { name: 'Toán 11 — 10 buổi' })).toBeVisible();

  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    if (variant === 'after') {
      await expect(page.locator('.tm-brand')).toHaveText('tutormatch');
      await expect(page.getByText('Trần Thu Hà').first()).toBeVisible();
      await expect(page.getByRole('link', { name: 'Danh sách gói học' })).toHaveAttribute('href', '/student/packages');
      await expect(page.getByRole('button', { name: 'Yêu cầu hoàn tiền phần chưa học' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Nhắn tin' })).toBeVisible();
    }
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidenceDirectory, `student-package-detail-${variant}-${width}.png`), fullPage: true });
  }

  if (variant === 'before') return;

  await page.setViewportSize({ width: 375, height: 900 });
  await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
  const bottomClearance = await page.evaluate(() => {
    const description = document.querySelector('.tm-student-package-description')?.getBoundingClientRect();
    const navigation = document.querySelector('.tm-workspace-bottom')?.getBoundingClientRect();
    return { contentBottom: description?.bottom ?? 0, navigationTop: navigation?.top ?? 0 };
  });
  expect(bottomClearance.contentBottom).toBeLessThanOrEqual(bottomClearance.navigationTop);
  await page.evaluate(() => window.scrollTo(0, 0));

  const backLink = page.getByRole('link', { name: 'Danh sách gói học' });
  await backLink.focus();
  await expect(backLink).toBeFocused();

  await page.getByRole('button', { name: 'Nhắn tin' }).click();
  await expect(page.getByText(/Bạn có thể nhắn tin sau khi gửi học thử hoặc mua gói học/)).toBeVisible();
  await expect(page).toHaveURL(/\/student\/packages\/pkg-visual$/);

  await page.getByRole('button', { name: 'Yêu cầu hoàn tiền phần chưa học' }).click();
  await expect(page.getByRole('dialog', { name: 'Yêu cầu hoàn tiền gói học' })).toBeVisible();
  await page.getByPlaceholder('Ví dụ: Lịch học bận không sắp xếp được tiếp tục...').fill('Lịch học không thể tiếp tục trong học kỳ này.');
  await page.getByRole('combobox', { name: 'Ngân hàng' }).click();
  await page.getByText('MB Bank (Ngân hàng Quân đội)', { exact: true }).click();
  await page.getByPlaceholder('Ví dụ: 0123456789').fill('0123456789');
  await page.getByPlaceholder('NGUYEN VAN A').fill('nguyen minh an');
  await page.getByRole('button', { name: 'Gửi yêu cầu hoàn tiền' }).click();
  await expect(page).toHaveURL(/\/student\/requests$/);
  await expect.poll(() => refundPayload).toMatchObject({ studentPackageId: 'pkg-visual', packageVersion: 4, requestedSessions: 1, bankBin: '970422', bankName: 'MB Bank (Ngân hàng Quân đội)', accountNumber: '0123456789', accountHolderName: 'NGUYEN MINH AN' });
  expect(refundPayload?.reason).toBe('Lịch học không thể tiếp tục trong học kỳ này.');

  packageData = { ...activePackage, status: 'LOCKED_EXPIRED' };
  await page.goto('/student/packages/pkg-visual');
  await expect(page.getByRole('button', { name: 'Yêu cầu gia hạn' })).toBeVisible();
  await page.getByRole('button', { name: 'Yêu cầu gia hạn' }).click();
  await expect(page.getByRole('dialog', { name: 'Yêu cầu gia hạn gói học' })).toBeVisible();
  await page.getByPlaceholder('Ví dụ: Đợt vừa rồi em thi học kỳ bận, mong ban quản trị gia hạn thêm 30 ngày để học nốt...').fill('Em cần thêm thời gian để hoàn thành các buổi học còn lại.');
  await page.getByPlaceholder('Chọn ngày kết thúc mới').click();
  await page.locator('.ant-picker-cell:not(.ant-picker-cell-disabled) .ant-picker-cell-inner').first().click();
  await page.getByRole('button', { name: 'Gửi yêu cầu gia hạn' }).click();
  await expect(page).toHaveURL(/\/student\/requests$/);
  await expect.poll(() => extensionPayload).toMatchObject({ studentPackageId: 'pkg-visual', packageVersion: 4, reason: 'Em cần thêm thời gian để hoàn thành các buổi học còn lại.' });
  expect(extensionPayload?.requestedExpiryDate).toEqual(expect.any(String));

  apiStatus = 503;
  await page.goto('/student/packages/pkg-visual');
  await expect(page.getByText('Không tìm thấy thông tin gói học hoặc bạn không có quyền xem')).toBeVisible();
  await page.getByRole('button', { name: 'Thử lại' }).click();
  apiStatus = 200;
  await expect(page.getByRole('heading', { name: 'Toán 11 — 10 buổi' })).toBeVisible();

  apiDelay = 1200;
  const loadingReload = page.reload();
  await expect(page.getByRole('status')).toContainText('Đang tải thông tin gói học');
  await loadingReload;
});
