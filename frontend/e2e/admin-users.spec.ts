import { expect, test } from '@playwright/test';

const admin = { id: 'admin-e2e', email: 'admin@example.test', fullName: 'Admin E2E', role: 'ADMIN', status: 'ACTIVE' };
const directoryUser = {
  id: '7f9a5cce-29de-4eb9-b07e-259d0b216eef', fullName: 'Nguyễn Minh An', email: 'an@example.test',
  role: 'STUDENT', status: 'ACTIVE', createdAt: '2026-09-01T00:00:00Z', lastLoginAt: null,
};

test('admin searches, filters, sorts, opens audit history, and locks a user', async ({ page }) => {
  const calls: string[] = [];
  let status: 'ACTIVE' | 'LOCKED' = 'ACTIVE';
  await page.route('**/api/auth/login', route => route.fulfill({ status: 200, contentType: 'application/json',
    body: JSON.stringify({ success: true, data: { accessToken: 'admin-access', refreshToken: 'admin-refresh', user: admin } }) }));
  await page.route('**/api/admin/**', async route => {
    const request = route.request();
    const url = new URL(request.url());
    calls.push(`${request.method()} ${url.pathname}${url.search}`);
    if (url.pathname === '/api/admin/users' && request.method() === 'GET') {
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true,
        data: [{ ...directoryUser, status }], errors: null, meta: { page: Number(url.searchParams.get('page') ?? 0),
          size: Number(url.searchParams.get('size') ?? 20), totalElements: 1, totalPages: 1, hasNext: false } }) });
    }
    if (url.pathname.startsWith('/api/admin/users/') && request.method() === 'PATCH') {
      status = request.postDataJSON().status;
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true,
        data: { id: directoryUser.id, email: directoryUser.email, fullName: directoryUser.fullName, roleName: 'STUDENT', statusName: status } }) });
    }
    if (url.pathname === '/api/admin/audit-logs') return route.fulfill({ status: 200, contentType: 'application/json',
      body: JSON.stringify({ success: true, data: [], errors: null, meta: { page: 0, size: 20, totalElements: 0, totalPages: 0, hasNext: false } }) });
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: [], errors: null, meta: null }) });
  });

  await page.goto('/auth/login');
  await page.getByPlaceholder('Nhập email của bạn').fill(admin.email);
  await page.getByPlaceholder('Nhập mật khẩu').fill('Password123!');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await page.goto('/admin/users');
  await expect(page.getByRole('heading', { name: 'Quản lý người dùng' })).toBeVisible();

  const search = page.getByRole('searchbox', { name: 'Tìm người dùng' });
  await search.fill('minh');
  await search.press('Enter');
  await expect.poll(() => calls.some(call => call.includes('keyword=minh'))).toBe(true);
  await page.locator('.tm-toolbar .ant-select').nth(0).click();
  const teacherOption = page.locator('.ant-select-dropdown .ant-select-item-option').filter({ hasText: 'Gia sư' });
  await expect(teacherOption).toBeVisible();
  await teacherOption.click();
  await expect.poll(() => calls.some(call => new URL(`http://localhost${call.split(' ')[1]}`).searchParams.get('role') === 'TEACHER')).toBe(true);
  await page.locator('.tm-toolbar .ant-select').nth(1).click();
  const lockedOption = page.locator('.ant-select-dropdown .ant-select-item-option').filter({ hasText: 'Đã khóa' });
  await expect(lockedOption).toBeVisible();
  await lockedOption.click();
  await expect.poll(() => calls.some(call => new URL(`http://localhost${call.split(' ')[1]}`).searchParams.get('status') === 'LOCKED')).toBe(true);
  await page.getByRole('columnheader', { name: 'Email' }).click();
  await expect.poll(() => calls.some(call => new URL(`http://localhost${call.split(' ')[1]}`).searchParams.get('sort') === 'email,asc')).toBe(true);
  await page.getByRole('combobox', { name: 'Page Size' }).click();
  await page.locator('.ant-select-dropdown .ant-select-item-option').filter({ hasText: '50 / page' }).click();
  await expect.poll(() => calls.some(call => new URL(`http://localhost${call.split(' ')[1]}`).searchParams.get('size') === '50')).toBe(true);

  await page.getByRole('link', { name: 'Lịch sử khóa/mở khóa' }).click();
  await expect(page).toHaveURL(new RegExp(`/admin/audit-logs\\?targetType=USER&targetId=${directoryUser.id}`));
  await expect(page.getByText(new RegExp(`Đang lọc theo người dùng ${directoryUser.id}`))).toBeVisible();

  await page.goto('/admin/users');
  await page.getByRole('button', { name: 'Khóa' }).click();
  await page.getByPlaceholder('Nhập lý do thực hiện thao tác...').fill('Vi phạm điều khoản');
  await page.getByRole('button', { name: 'Xác nhận khóa' }).click();
  await expect.poll(() => status).toBe('LOCKED');
  await expect(page.getByRole('button', { name: 'Mở khóa' })).toBeVisible();
});
