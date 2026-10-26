import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const student = { id: 'request-student', email: 'requests@example.test', fullName: 'Nguyễn Minh An', role: 'STUDENT', status: 'ACTIVE' };
const envelope = (data: unknown, meta: unknown = null) => ({ success: true, message: 'ok', data, errors: null, meta });
const rejected = { success: false, message: 'unavailable', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null };

test('student requests redesign preserves three lists, paging and retry states', async ({ page }) => {
  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  let trialStatus = 200;
  const listRequests: URL[] = [];
  const fixture = {
    trial: [{ id: 'trial-1', teacherId: 'teacher-1', studentId: student.id, subjectId: 'subject-1', preferredStartTime: '2026-10-20T03:30:00Z', note: 'Ôn tập phương trình', status: 'REJECTED', rejectionReason: 'Không còn khung giờ phù hợp', createdAt: '2026-10-16T08:30:00Z', version: 0 }],
    refund: [{ id: 'refund-1', studentPackageId: 'package-1', studentId: student.id, reason: 'Chuyển nơi ở', requestedSessions: 3, refundAmountVnd: null, status: 'PENDING', adminNote: null, version: 0, createdAt: '2026-10-16T08:30:00Z' }],
    extension: [{ id: 'extension-1', studentPackageId: 'package-1', studentId: student.id, reason: 'Nghỉ điều trị', requestedExpiryDate: '2026-11-20', status: 'APPROVED', adminNote: 'Đã gia hạn theo yêu cầu', createdAt: '2026-10-16T08:30:00Z' }],
  };
  await page.route('**/api/auth/login', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope({ accessToken: 'visual-requests-access', refreshToken: 'visual-requests-refresh', user: student })) }));
  await page.route('**/api/student/**', async route => {
    const url = new URL(route.request().url());
    listRequests.push(url);
    const pageIndex = Number(url.searchParams.get('page') || 0);
    const isTrial = url.pathname.endsWith('/trial-requests');
    if (isTrial && trialStatus !== 200) return route.fulfill({ status: trialStatus, contentType: 'application/json', body: JSON.stringify(rejected) });
    const data = isTrial ? (pageIndex === 0 ? fixture.trial : []) : url.pathname.endsWith('/refund-requests') ? fixture.refund : fixture.extension;
    const totalElements = isTrial ? 11 : 1;
    const totalPages = isTrial ? 2 : 1;
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(data, { page: pageIndex, size: 10, totalElements, totalPages, hasNext: pageIndex + 1 < totalPages })) });
  });

  await page.goto('/auth/login');
  await page.getByPlaceholder('Nhập email của bạn').fill(student.email);
  await page.getByPlaceholder('Nhập mật khẩu').fill('Password123!');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page).toHaveURL(/\/student$/);
  await page.goto('/student/requests');
  await expect(page.getByRole('heading', { name: 'Yêu cầu của tôi' })).toBeVisible();
  if (variant === 'before') await expect(page.getByText('Ôn tập phương trình')).toBeVisible();
  else await expect(page.getByText('Ôn tập phương trình')).toBeVisible();

  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    if (variant === 'after') {
      const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
      expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    }
    await page.screenshot({ path: resolve(evidence, `student-requests-${variant}-${width}.png`), fullPage: true });
  }
  if (variant === 'before') return;

  await page.setViewportSize({ width: 1280, height: 900 });
  await page.getByRole('tab', { name: 'Học thử' }).focus();
  await page.keyboard.press('ArrowRight');
  await expect(page.getByRole('tab', { name: 'Hoàn tiền' })).toHaveAttribute('aria-selected', 'true');
  await expect(page.getByText('Số tiền đang được ước tính')).toBeVisible();
  await expect(page.getByText('Chuyển nơi ở')).toBeVisible();
  await page.getByRole('tab', { name: 'Gia hạn' }).click();
  await expect(page.getByText('Đã gia hạn theo yêu cầu')).toBeVisible();
  await expect(page.getByText(/20\/11\/2026/)).toBeVisible();
  await page.getByRole('tab', { name: 'Học thử' }).click();
  await page.getByRole('listitem', { name: '2', exact: true }).click();
  await expect.poll(() => listRequests.filter(request => request.pathname.endsWith('/trial-requests')).at(-1)?.searchParams.get('page')).toBe('1');
  await expect(page.getByText('Chưa có yêu cầu học thử')).toBeVisible();
  await page.getByRole('listitem', { name: '1', exact: true }).click();

  trialStatus = 503;
  await page.reload();
  await expect(page.getByRole('tab', { name: 'Học thử' })).toBeVisible();
  await expect(page.getByText('Chưa tải được danh sách yêu cầu')).toBeVisible();
  expect(await page.getByText('Chưa có yêu cầu học thử').count()).toBe(0);
  trialStatus = 200;
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByText('Ôn tập phương trình')).toBeVisible();
  await page.keyboard.press('Tab');
  expect(await page.evaluate(() => document.activeElement?.matches(':focus-visible') ?? false)).toBe(true);
});
