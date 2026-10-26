import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const admin = { id: 'admin-dashboard', email: 'admin-dashboard@example.test', fullName: 'Quản trị viên', role: 'ADMIN', status: 'ACTIVE' };
const dashboard = {
  totalGmvVnd: 138000000,
  totalCommissionVnd: 6900000,
  totalTeachers: 125,
  totalStudents: 860,
  totalBookings: 1120,
  completedBookings: 850,
  scheduledBookings: 230,
  cancelledBookings: 40,
  pendingPayoutsCount: 7,
  pendingPayoutsAmountVnd: 5500000,
  pendingRefundsCount: 3,
};

async function setupAdmin(page: import('@playwright/test').Page, failure = false) {
  const calls: string[] = [];
  await page.addInitScript(user => {
    localStorage.setItem('tutor-match.session', JSON.stringify({ user, remember: false, sessionId: 'admin-dashboard-session' }));
    document.cookie = 'accessToken=admin-dashboard-access; path=/; SameSite=Lax';
  }, admin);
  await page.route('**/api/admin/dashboard**', async route => {
    calls.push(`${route.request().method()} ${new URL(route.request().url()).pathname}`);
    if (failure) {
      await route.fulfill({ status: 500, contentType: 'application/json', body: JSON.stringify({ message: 'Unavailable' }) });
      return;
    }
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, message: 'ok', data: dashboard, errors: null, meta: null }) });
  });
  return calls;
}

test('redesigned Admin overview works at all required widths and both URLs', async ({ page }) => {
  const calls = await setupAdmin(page);
  await page.goto('/admin');
  await expect(page.getByRole('heading', { name: 'Tổng quan quản trị' })).toBeVisible();
  await expect(page.getByText('138.000.000 ₫')).toBeVisible();
  await expect(page.getByText('6.900.000 ₫')).toBeVisible();
  await expect(page.getByRole('link', { name: 'Lệnh rút tiền chờ duyệt' })).toHaveAttribute('href', '/admin/payouts');
  await expect(page.getByRole('link', { name: 'Quyết toán buổi học' })).toHaveAttribute('href', '/admin/booking-settlements');
  await page.getByRole('link', { name: 'Lệnh rút tiền chờ duyệt' }).focus();
  await page.keyboard.press('Tab');
  await expect(page.locator(':focus')).toHaveAttribute('href', '/admin/refunds');
  expect(await page.locator(':focus').evaluate(element => getComputedStyle(element).outlineStyle)).not.toBe('none');

  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    const dimensions = await page.evaluate(() => ({ body: document.body.scrollWidth, viewport: window.innerWidth }));
    expect(dimensions.body).toBeLessThanOrEqual(dimensions.viewport);
    await page.screenshot({ path: resolve(evidence, `admin-dashboard-after-${width}.png`), fullPage: true });
  }

  await page.goto('/admin/dashboard');
  await expect(page.getByRole('heading', { name: 'Tổng quan quản trị' })).toBeVisible();
  await expect(page.getByText('138.000.000 ₫')).toBeVisible();
  expect(calls).toEqual(['GET /api/admin/dashboard', 'GET /api/admin/dashboard']);
});

test('Admin overview shows a recoverable error and retries the existing API', async ({ page }) => {
  const calls: string[] = [];
  await page.addInitScript(user => {
    localStorage.setItem('tutor-match.session', JSON.stringify({ user, remember: false, sessionId: 'admin-dashboard-session' }));
    document.cookie = 'accessToken=admin-dashboard-access; path=/; SameSite=Lax';
  }, admin);
  await page.route('**/api/admin/dashboard**', async route => {
    calls.push(route.request().method());
    if (calls.length <= 4) {
      await route.fulfill({ status: 500, contentType: 'application/json', body: JSON.stringify({ message: 'Unavailable' }) });
      return;
    }
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, message: 'ok', data: dashboard, errors: null, meta: null }) });
  });

  await page.goto('/admin');
  await expect(page.getByText('Chưa tải được số liệu quản trị')).toBeVisible({ timeout: 15_000 });
  await page.getByRole('button', { name: 'Tải lại' }).click();
  await expect(page.getByText('138.000.000 ₫')).toBeVisible();
  expect(calls).toHaveLength(5);
  expect(calls.every(method => method === 'GET')).toBe(true);
});
