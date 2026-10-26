import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const admin = { id: 'admin-users', email: 'admin@example.test', fullName: 'Quản trị viên', role: 'ADMIN', status: 'ACTIVE' };
const users = [
  { id: 'user-student-02', fullName: 'Trần Minh Anh', email: 'minhanh@example.test', role: 'STUDENT', status: 'ACTIVE', createdAt: '2026-09-02T05:15:00Z', lastLoginAt: '2026-10-15T12:00:00Z' },
  { id: 'user-teacher-08', fullName: 'Lê Quốc Bảo', email: 'quocbao@example.test', role: 'TEACHER', status: 'LOCKED', createdAt: '2026-08-11T02:30:00Z', lastLoginAt: null },
];

async function seedAdmin(page: import('@playwright/test').Page) {
  await page.addInitScript(user => {
    localStorage.setItem('tutor-match.session', JSON.stringify({ user, remember: false, sessionId: 'admin-users-session' }));
    document.cookie = 'accessToken=admin-users-access; path=/; SameSite=Lax';
  }, admin);
}

async function mockUsers(page: import('@playwright/test').Page, options: { empty?: boolean; failListOnce?: boolean; failMutation?: boolean } = {}) {
  const state = { queries: [] as Array<Record<string, string>>, mutations: [] as Array<{ method: string; url: string; body: unknown }> };
  let records = users.map(user => ({ ...user }));
  let listFailed = false;
  await page.route('**/api/admin/users**', async route => {
    const request = route.request();
    const url = new URL(request.url());
    if (request.method() === 'GET') {
      state.queries.push(Object.fromEntries(url.searchParams.entries()));
      if (options.failListOnce && !listFailed) {
        listFailed = true;
        await route.fulfill({ status: 422, contentType: 'application/json', body: JSON.stringify({ message: 'Unavailable' }) });
        return;
      }
      const data = options.empty ? [] : records;
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, message: 'ok', data, errors: null, meta: { page: 0, size: 20, totalElements: data.length, totalPages: data.length ? 1 : 0 } }) });
      return;
    }
    const body = request.postDataJSON();
    state.mutations.push({ method: request.method(), url: url.pathname, body });
    if (options.failMutation) {
      await route.fulfill({ status: 500, contentType: 'application/json', body: JSON.stringify({ message: 'Unavailable' }) });
      return;
    }
    const existing = records.find(user => user.id === url.pathname.split('/').at(-2));
    if (!existing) {
      await route.fulfill({ status: 404, contentType: 'application/json', body: JSON.stringify({ message: 'Not found' }) });
      return;
    }
    const updated = { ...existing, status: (body as { status: string }).status };
    records = records.map(user => user.id === updated.id ? updated : user);
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, message: 'ok', data: updated, errors: null, meta: null }) });
  });
  return state;
}

test('Admin user directory renders at the approved responsive reference sizes', async ({ page }) => {
  const baseline = process.env.BASELINE_ADMIN_USERS === '1';
  await seedAdmin(page);
  const state = await mockUsers(page);
  await page.goto('/admin/users', { waitUntil: 'domcontentloaded' });
  await expect(page.getByRole('heading', { name: 'Quản lý người dùng' })).toBeVisible();
  await expect(page.getByText('minhanh@example.test')).toBeVisible();
  await expect(page.getByText('quocbao@example.test')).toBeVisible();
  expect(state.queries.length).toBeGreaterThan(0);
  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    if (!baseline) {
      const dimensions = await page.evaluate(() => ({ body: document.body.scrollWidth, viewport: window.innerWidth }));
      expect(dimensions.body).toBeLessThanOrEqual(dimensions.viewport);
    }
    await page.screenshot({ path: resolve(evidence, `admin-users-${baseline ? 'before' : 'after'}-${width}.png`), fullPage: true });
  }
  if (!baseline) {
    await page.getByRole('button', { name: 'Khóa', exact: true }).focus();
    expect(await page.locator(':focus').evaluate(element => getComputedStyle(element).outlineStyle)).not.toBe('none');
  }
});

test('Admin user filters and sort retain API query parameters', async ({ page }) => {
  await seedAdmin(page);
  const state = await mockUsers(page);
  await page.goto('/admin/users', { waitUntil: 'domcontentloaded' });
  await page.getByRole('searchbox', { name: 'Tìm người dùng' }).fill('minh');
  await page.keyboard.press('Enter');
  await expect.poll(() => state.queries.some(query => query.keyword === 'minh')).toBeTruthy();
  await page.getByLabel('Lọc vai trò').click();
  await page.getByText('Gia sư', { exact: true }).last().click();
  await expect.poll(() => state.queries.some(query => query.role === 'TEACHER')).toBeTruthy();
  await page.getByLabel('Sắp xếp theo').click();
  await page.getByText('Email', { exact: true }).last().click();
  await expect.poll(() => state.queries.some(query => query.sort === 'email,desc')).toBeTruthy();
  await page.getByRole('button', { name: 'Đổi chiều sắp xếp' }).click();
  await expect.poll(() => state.queries.some(query => query.sort === 'email,asc')).toBeTruthy();
  await page.getByRole('searchbox', { name: 'Tìm người dùng' }).fill('');
  await expect.poll(() => state.queries.some(query => !query.keyword)).toBeTruthy();
  expect(state.queries.some(query => query.size === '20' && query.sort === 'createdAt,desc')).toBeTruthy();
});

test('Admin locks a user with a required reason and preserves audit target link', async ({ page }) => {
  await seedAdmin(page);
  const state = await mockUsers(page);
  await page.goto('/admin/users', { waitUntil: 'domcontentloaded' });
  const studentRow = page.locator('li').filter({ hasText: 'minhanh@example.test' });
  await page.setViewportSize({ width: 375, height: 900 });
  await studentRow.getByRole('button', { name: 'Khóa' }).click();
  await expect(page.getByRole('dialog', { name: 'Khóa tài khoản người dùng' })).toBeVisible();
  const modalWidth = await page.getByRole('dialog').evaluate(element => element.getBoundingClientRect().width);
  expect(modalWidth).toBeLessThanOrEqual(375);
  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  await page.screenshot({ path: resolve(evidence, 'admin-users-modal-after-375.png'), fullPage: true });
  await page.getByRole('button', { name: 'Xác nhận khóa' }).click();
  await expect(page.getByText('Vui lòng nhập lý do điều chỉnh trạng thái tài khoản', { exact: true })).toBeVisible();
  await page.getByRole('textbox', { name: 'Lý do điều chỉnh trạng thái tài khoản' }).fill('Tài khoản cần xác minh.');
  await page.getByRole('button', { name: 'Xác nhận khóa' }).click();
  await expect(page.getByRole('dialog')).toHaveCount(0);
  await expect(studentRow.getByText('Đã khóa')).toBeVisible();
  expect(state.mutations).toContainEqual({ method: 'PATCH', url: '/api/admin/users/user-student-02/status', body: { status: 'LOCKED', reason: 'Tài khoản cần xác minh.' } });
  await expect(page.getByRole('link', { name: 'Lịch sử khóa/mở khóa' }).first()).toHaveAttribute('href', '/admin/audit-logs?targetType=USER&targetId=user-student-02');
});

test('Admin reactivates a locked user with a reason; failed mutation keeps its context', async ({ page }) => {
  await seedAdmin(page);
  const state = await mockUsers(page);
  await page.goto('/admin/users', { waitUntil: 'domcontentloaded' });
  const teacherRow = page.locator('li').filter({ hasText: 'quocbao@example.test' });
  await teacherRow.getByRole('button', { name: 'Mở khóa' }).click();
  await expect(page.getByRole('dialog', { name: 'Mở khóa tài khoản người dùng' })).toBeVisible();
  await page.getByRole('textbox', { name: 'Lý do điều chỉnh trạng thái tài khoản' }).fill('Đã xác minh danh tính.');
  await page.getByRole('button', { name: 'Xác nhận mở khóa' }).click();
  await expect(page.getByRole('dialog')).toHaveCount(0);
  await expect(teacherRow.getByText('Đang hoạt động')).toBeVisible();
  expect(state.mutations).toContainEqual({ method: 'PATCH', url: '/api/admin/users/user-teacher-08/status', body: { status: 'ACTIVE', reason: 'Đã xác minh danh tính.' } });

  await page.unrouteAll({ behavior: 'wait' });
  await mockUsers(page, { failMutation: true });
  await page.reload({ waitUntil: 'domcontentloaded' });
  const activeRow = page.locator('li').filter({ hasText: 'minhanh@example.test' });
  await activeRow.getByRole('button', { name: 'Khóa' }).click();
  await page.getByRole('textbox', { name: 'Lý do điều chỉnh trạng thái tài khoản' }).fill('Kiểm tra lỗi máy chủ.');
  await page.getByRole('button', { name: 'Xác nhận khóa' }).click();
  await expect(page.getByRole('dialog', { name: 'Khóa tài khoản người dùng' })).toBeVisible();
});

test('Admin user list shows retry and empty states', async ({ page }) => {
  await seedAdmin(page);
  await mockUsers(page, { failListOnce: true });
  await page.goto('/admin/users', { waitUntil: 'domcontentloaded' });
  await expect(page.getByText('Chưa tải được danh sách người dùng')).toBeVisible();
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByText('minhanh@example.test')).toBeVisible();

  await page.unrouteAll({ behavior: 'wait' });
  await mockUsers(page, { empty: true });
  await page.reload({ waitUntil: 'domcontentloaded' });
  await expect(page.getByText('Không tìm thấy người dùng phù hợp.')).toBeVisible();
});
