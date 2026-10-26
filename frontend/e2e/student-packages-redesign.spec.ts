import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const student = { id: 'student-packages-visual', email: 'student@example.test', fullName: 'An Nguyen', role: 'STUDENT', status: 'ACTIVE' };
const packages = [
  { id: 'pkg-1', teacher: { id: 'teacher-1', fullName: 'Trần Thu Hà' }, subject: { id: 'subject-1', name: 'Toán 11' }, packageName: 'Toán 11 — 10 buổi', totalSessions: 10, remainingSessions: 7, reservedSessions: 1, completedSessions: 2, refundedSessions: 0, purchasePriceVnd: 1000000, startsAt: '2026-08-19T02:00:00Z', expiresAt: '2026-11-17T02:00:00Z', status: 'ACTIVE', version: 1 },
  { id: 'pkg-2', teacher: { id: 'teacher-2', fullName: 'Nguyễn Minh Châu' }, subject: { id: 'subject-2', name: 'Tiếng Anh giao tiếp' }, packageName: 'Giao tiếp căn bản — 12 buổi', totalSessions: 12, remainingSessions: 9, reservedSessions: 0, completedSessions: 3, refundedSessions: 0, purchasePriceVnd: 1440000, startsAt: '2026-09-01T02:00:00Z', expiresAt: '2027-01-01T02:00:00Z', status: 'ACTIVE', version: 1 },
  { id: 'pkg-3', teacher: { id: 'teacher-3', fullName: 'Lê Quang Huy' }, subject: { id: 'subject-3', name: 'Vật lý 12' }, packageName: 'Vật lý 12 — 8 buổi', totalSessions: 8, remainingSessions: 2, reservedSessions: 1, completedSessions: 5, refundedSessions: 0, purchasePriceVnd: 960000, startsAt: '2026-09-04T02:00:00Z', expiresAt: '2026-12-20T02:00:00Z', status: 'ACTIVE', version: 1 },
];

test('student packages keep API filters and terms while presenting the responsive approved list', async ({ page }) => {
  const evidenceDirectory = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidenceDirectory, { recursive: true });
  let responseStatus = 200;
  let responseDelay = 0;
  let responseRows = packages;
  const requested: string[] = [];

  await page.route('**/api/auth/login', route => route.fulfill({
    status: 200, contentType: 'application/json',
    body: JSON.stringify({ success: true, message: 'ok', data: { accessToken: 'visual-access', refreshToken: 'visual-refresh', user: student }, errors: null, meta: null }),
  }));
  await page.route('**/api/student/packages**', async route => {
    const url = new URL(route.request().url());
    requested.push(url.search);
    if (responseDelay) await new Promise(resolveRequest => setTimeout(resolveRequest, responseDelay));
    await route.fulfill({
      status: responseStatus,
      contentType: 'application/json',
      body: JSON.stringify(responseStatus === 200
        ? { success: true, message: 'ok', data: responseRows, errors: null, meta: { page: Number(url.searchParams.get('page') || 0), size: Number(url.searchParams.get('size') || 9), totalElements: responseRows.length === 0 ? 0 : 20, totalPages: responseRows.length === 0 ? 0 : 3 } }
        : { success: false, message: 'unavailable', data: null, errors: null, meta: null }),
    });
  });

  await page.setViewportSize({ width: 1280, height: 900 });
  await page.goto('/auth/login');
  await page.getByPlaceholder('Nhập email của bạn').fill(student.email);
  await page.getByPlaceholder('Nhập mật khẩu').fill('Password123!');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page).toHaveURL(/\/student$/);
  await page.goto('/student/packages');
  await expect(page.getByRole('heading', { name: 'Gói học của tôi' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Toán 11 — 10 buổi' })).toBeVisible();

  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    if (variant === 'after') {
      await expect(page.locator('.tm-brand')).toHaveText('tutormatch');
      await expect(page.getByText('Đã thanh toán').first()).toBeVisible();
      await expect(page.getByRole('link', { name: 'Xem chi tiết' }).first()).toHaveAttribute('href', '/student/packages/pkg-1');
      await expect(page.getByText('7', { exact: true })).toBeVisible();
    }
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidenceDirectory, `student-packages-${variant}-${width}.png`), fullPage: true });
  }

  if (variant === 'before') return;

  const packagesAction = page.getByRole('link', { name: 'Xem chi tiết' }).first();
  await packagesAction.focus();
  await expect(packagesAction).toBeFocused();
  await page.getByRole('tab', { name: 'Đang hoạt động' }).click();
  await expect.poll(() => requested.at(-1)).toContain('status=ACTIVE');
  await expect.poll(() => requested.at(-1)).toContain('page=0');
  await page.locator('.ant-pagination-next').click();
  await expect.poll(() => requested.at(-1)).toContain('page=1');
  await expect.poll(() => requested.at(-1)).toContain('size=9');

  responseDelay = 1200;
  const reloadLoading = page.reload();
  await expect(page.getByRole('status')).toContainText('Đang tải danh sách gói học');
  await reloadLoading;

  responseDelay = 0;
  responseStatus = 503;
  await page.reload();
  await expect(page.getByText('Chưa tải được danh sách gói học')).toBeVisible();
  responseStatus = 200;
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByRole('heading', { name: 'Toán 11 — 10 buổi' })).toBeVisible();

  responseRows = [];
  await page.reload();
  await expect(page.getByText('Bạn chưa có gói học nào.')).toBeVisible();
  await expect(page.getByRole('link', { name: 'Tìm gia sư' })).toHaveAttribute('href', '/subjects');
  await page.setViewportSize({ width: 375, height: 900 });
  await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
  const finalActionClearance = await page.evaluate(() => {
    const action = document.querySelector('.tm-student-package-empty a')?.getBoundingClientRect();
    const navigation = document.querySelector('.tm-workspace-bottom')?.getBoundingClientRect();
    return { actionBottom: action?.bottom ?? 0, navigationTop: navigation?.top ?? 0 };
  });
  expect(finalActionClearance.actionBottom).toBeLessThanOrEqual(finalActionClearance.navigationTop);
});
