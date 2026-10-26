import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const admin = { id: 'admin-audit', email: 'admin@example.test', fullName: 'Quản trị viên', role: 'ADMIN', status: 'ACTIVE' };
const auditLog = {
  id: 'audit-001-uuid', actorId: 'admin-operator-01', action: 'USER_LOCKED', targetType: 'USER', targetId: 'user-target-001',
  beforeData: { status: 'ACTIVE' }, afterData: { status: 'LOCKED', reason: 'Vi phạm điều khoản.' },
  ipAddress: '192.0.2.8', userAgent: 'TutorMatch admin fixture', createdAt: '2026-10-14T08:25:00Z',
};

async function seedAdmin(page: import('@playwright/test').Page) {
  await page.addInitScript(user => {
    localStorage.setItem('tutor-match.session', JSON.stringify({ user, remember: false, sessionId: 'admin-audit-session' }));
    document.cookie = 'accessToken=admin-audit-access; path=/; SameSite=Lax';
  }, admin);
}

async function mockAuditLogs(page: import('@playwright/test').Page, options: { empty?: boolean; failGet?: boolean } = {}) {
  const state = { queries: [] as Array<Record<string, string>>, reads: 0 };
  await page.route('**/api/admin/audit-logs**', async route => {
    const url = new URL(route.request().url());
    state.reads += 1;
    state.queries.push(Object.fromEntries(url.searchParams.entries()));
    if (options.failGet && state.reads <= 4) {
      await route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ message: 'Unavailable' }) });
      return;
    }
    const data = options.empty ? [] : [auditLog];
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, message: 'ok', data, errors: null, meta: { page: Number(url.searchParams.get('page') || 0), size: 20, totalElements: data.length, totalPages: data.length ? 1 : 0 } }) });
  });
  return state;
}

test('Admin audit log page is responsive and contains only audit records', async ({ page }) => {
  const baseline = process.env.BASELINE_ADMIN_AUDIT === '1';
  await seedAdmin(page);
  const state = await mockAuditLogs(page);
  await page.goto('/admin/audit-logs', { waitUntil: 'domcontentloaded' });
  await expect(page.getByRole('heading', { name: baseline ? 'Nhật ký Kiểm toán (Audit Logs)' : 'Nhật ký hoạt động' })).toBeVisible();
  await expect(page.getByText('USER_LOCKED')).toBeVisible();
  expect(state.queries[0]).toMatchObject({ page: '0', size: '20' });
  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    if (!baseline) {
      const dimensions = await page.evaluate(() => ({ body: document.body.scrollWidth, viewport: window.innerWidth }));
      expect(dimensions.body).toBeLessThanOrEqual(dimensions.viewport);
    }
    await page.screenshot({ path: resolve(evidence, `admin-audit-logs-${baseline ? 'before' : 'after'}-${width}.png`), fullPage: true });
  }
});

test('Admin audit deep link keeps target type and target ID in the API query', async ({ page }) => {
  await seedAdmin(page);
  const state = await mockAuditLogs(page);
  await page.goto('/admin/audit-logs?targetType=USER&targetId=user-target-001', { waitUntil: 'domcontentloaded' });
  await expect(page.getByText('Đang lọc theo người dùng · user-target-001')).toBeVisible();
  expect(state.queries[0]).toMatchObject({ targetType: 'USER', targetId: 'user-target-001', page: '0', size: '20' });
});

test('Admin can read before and after audit JSON without changing the record', async ({ page }) => {
  const baseline = process.env.BASELINE_ADMIN_AUDIT === '1';
  await seedAdmin(page);
  const state = await mockAuditLogs(page);
  await page.goto('/admin/audit-logs', { waitUntil: 'domcontentloaded' });
  await page.getByRole('button', { name: baseline ? 'Xem JSON' : 'Chi tiết' }).click();
  await expect(page.getByRole('dialog')).toBeVisible();
  await expect(page.getByText('Dữ liệu trước thay đổi')).toBeVisible();
  await expect(page.getByText('"status": "ACTIVE"')).toBeVisible();
  await expect(page.getByText('"status": "LOCKED"')).toBeVisible();
  expect(state.reads).toBe(1);
});

test('Admin audit query retries after failure and renders an empty result', async ({ page }) => {
  await seedAdmin(page);
  const state = await mockAuditLogs(page, { failGet: true });
  await page.goto('/admin/audit-logs', { waitUntil: 'domcontentloaded' });
  await expect(page.getByText('Không tải được nhật ký hoạt động.', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByText('USER_LOCKED')).toBeVisible();
  expect(state.reads).toBeGreaterThanOrEqual(5);
  await page.unrouteAll({ behavior: 'wait' });
  await mockAuditLogs(page, { empty: true });
  await page.reload({ waitUntil: 'domcontentloaded' });
  await expect(page.getByText('Chưa có nhật ký hoạt động.', { exact: true })).toBeVisible();
});
