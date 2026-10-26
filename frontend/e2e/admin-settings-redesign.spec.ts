import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const admin = { id: 'admin-settings', email: 'admin@example.test', fullName: 'Quản trị viên', role: 'ADMIN', status: 'ACTIVE' };
const initialSettings = { id: 'settings-1', commissionRate: 0.05, bayesianMinimumReviews: 5, bookingReminderHours: 2, bookingExpirationHours: 12, updatedAt: '2026-10-14T08:25:00Z' };

async function seedAdmin(page: import('@playwright/test').Page) {
  await page.addInitScript(user => {
    localStorage.setItem('tutor-match.session', JSON.stringify({ user, remember: false, sessionId: 'admin-settings-session' }));
    document.cookie = 'accessToken=admin-settings-access; path=/; SameSite=Lax';
  }, admin);
}

async function mockSettings(page: import('@playwright/test').Page, options: { failGetOnce?: boolean; failPut?: boolean } = {}) {
  const state = { reads: 0, updates: [] as Array<Record<string, number>> };
  let settings = { ...initialSettings };
  await page.route('**/api/admin/settings', async route => {
    if (route.request().method() === 'GET') {
      state.reads += 1;
      if (options.failGetOnce && state.reads <= 4) {
        await route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ message: 'Unavailable' }) });
        return;
      }
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, message: 'ok', data: settings, errors: null, meta: null }) });
      return;
    }
    const body = route.request().postDataJSON() as Record<string, number>;
    state.updates.push(body);
    if (options.failPut) {
      await route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ message: 'Update unavailable' }) });
      return;
    }
    settings = { ...settings, ...body, updatedAt: '2026-10-16T02:30:00Z' };
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, message: 'ok', data: settings, errors: null, meta: null }) });
  });
  return state;
}

test('Admin settings page is responsive and exposes only current settings', async ({ page }) => {
  const baseline = process.env.BASELINE_ADMIN_SETTINGS === '1';
  await seedAdmin(page);
  const state = await mockSettings(page);
  await page.goto('/admin/settings', { waitUntil: 'domcontentloaded' });
  await expect(page.getByRole('heading', { name: baseline ? 'Cài đặt Sàn & Tham số Hệ thống' : 'Cài đặt nền tảng' })).toBeVisible();
  await expect(page.getByLabel('Tỉ lệ hoa hồng sàn (%)')).toHaveAttribute('aria-valuenow', '5');
  expect(state.reads).toBeGreaterThan(0);
  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    if (!baseline) {
      const dimensions = await page.evaluate(() => ({ body: document.body.scrollWidth, viewport: window.innerWidth }));
      expect(dimensions.body).toBeLessThanOrEqual(dimensions.viewport);
    }
    await page.screenshot({ path: resolve(evidence, `admin-settings-${baseline ? 'before' : 'after'}-${width}.png`), fullPage: true });
  }
});

test('Admin settings update sends the exact platform contract and refreshes timestamp', async ({ page }) => {
  await seedAdmin(page);
  const state = await mockSettings(page);
  await page.goto('/admin/settings', { waitUntil: 'domcontentloaded' });
  await page.getByLabel('Tỉ lệ hoa hồng sàn (%)').fill('6.5');
  await page.getByLabel('Số đánh giá tối thiểu tính Bayesian Average').fill('8');
  await page.getByLabel('Thời gian gửi nhắc lịch học trước giờ bắt đầu').fill('3');
  await page.getByLabel('Thời gian tối đa để gia sư xác nhận hoàn thành buổi học').fill('18');
  await page.getByRole('button', { name: 'Lưu cấu hình' }).click();
  await expect(page.getByText('09:30, 16/10/2026')).toBeVisible();
  expect(state.updates).toEqual([{ commissionRate: 0.065, bayesianMinimumReviews: 8, bookingReminderHours: 3, bookingExpirationHours: 18 }]);
});

test('Admin settings validation blocks a missing required value', async ({ page }) => {
  await seedAdmin(page);
  const state = await mockSettings(page);
  await page.goto('/admin/settings', { waitUntil: 'domcontentloaded' });
  await page.getByLabel('Số đánh giá tối thiểu tính Bayesian Average').fill('');
  await page.getByRole('button', { name: 'Lưu cấu hình' }).click();
  await expect(page.getByText('Vui lòng nhập số đánh giá tối thiểu')).toBeVisible();
  expect(state.updates).toHaveLength(0);
});

test('Admin settings preserves edits after save failure', async ({ page }) => {
  await seedAdmin(page);
  const state = await mockSettings(page, { failPut: true });
  await page.goto('/admin/settings', { waitUntil: 'domcontentloaded' });
  await page.getByLabel('Số đánh giá tối thiểu tính Bayesian Average').fill('9');
  await page.getByRole('button', { name: 'Lưu cấu hình' }).click();
  await expect(page.getByText('Update unavailable')).toBeVisible();
  await expect(page.getByLabel('Số đánh giá tối thiểu tính Bayesian Average')).toHaveValue('9');
  expect(state.updates).toHaveLength(1);
});

test('Admin settings query retries after load failure', async ({ page }) => {
  await seedAdmin(page);
  const state = await mockSettings(page, { failGetOnce: true });
  await page.goto('/admin/settings', { waitUntil: 'domcontentloaded' });
  await expect(page.getByText('Không tải được cài đặt nền tảng.', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByLabel('Tỉ lệ hoa hồng sàn (%)')).toHaveAttribute('aria-valuenow', '5');
  expect(state.reads).toBeGreaterThanOrEqual(2);
});
