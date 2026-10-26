import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const admin = { id: 'admin-teachers', email: 'admin@example.test', fullName: 'Quản trị viên', role: 'ADMIN', status: 'ACTIVE' };
const teacher = {
  teacherProfileId: 'teacher-001', userId: 'user-001', fullName: 'Nguyễn Mai Lan', email: 'mailan@example.test',
  bio: 'Gia sư tiếng Việt với kinh nghiệm hướng dẫn học viên theo mục tiêu cá nhân.', yearsOfExperience: 6,
  languages: ['Tiếng Việt', 'English'], supportsOnline: true, supportsOffline: false,
  locationAddress: 'Quận 3, Thành phố Hồ Chí Minh', introductionVideoUrl: 'https://video.example.test/intro',
  status: 'PENDING_APPROVAL', rejectionReason: null,
  documents: [{ id: 'doc-1', type: 'DEGREE', title: 'Bằng cử nhân', secureUrl: 'https://files.example.test/degree.pdf', mimeType: 'application/pdf', fileSize: 1048576, verificationStatus: 'PENDING' }],
};

async function seedAdmin(page: import('@playwright/test').Page) {
  await page.addInitScript(user => {
    localStorage.setItem('tutor-match.session', JSON.stringify({ user, remember: false, sessionId: 'admin-teachers-session' }));
    document.cookie = 'accessToken=admin-teachers-access; path=/; SameSite=Lax';
  }, admin);
}

async function mockQueue(page: import('@playwright/test').Page, options: { empty?: boolean; failFirst?: number } = {}) {
  const requests: Array<{ method: string; status: string; page: string; size: string; sort: string }> = [];
  await page.route('**/api/admin/teachers/approvals**', async route => {
    const url = new URL(route.request().url());
    const status = url.searchParams.get('status') ?? '';
    requests.push({ method: route.request().method(), status, page: url.searchParams.get('page') ?? '', size: url.searchParams.get('size') ?? '', sort: url.searchParams.get('sort') ?? '' });
    if (requests.length <= (options.failFirst ?? 0)) {
      await route.fulfill({ status: 500, contentType: 'application/json', body: JSON.stringify({ message: 'Unavailable' }) });
      return;
    }
    const row = { ...teacher, status };
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, message: 'ok', data: options.empty ? [] : [row], errors: null, meta: { page: Number(url.searchParams.get('page') ?? 0), size: Number(url.searchParams.get('size') ?? 10), totalElements: options.empty ? 0 : 1, totalPages: options.empty ? 0 : 1 } }) });
  });
  return requests;
}

test('Admin teacher approvals match the queue spec at all required widths', async ({ page }) => {
  await seedAdmin(page);
  const requests = await mockQueue(page);
  await page.goto('/admin/teachers');
  await expect(page.getByRole('heading', { name: 'Xét duyệt hồ sơ gia sư' })).toBeVisible();
  await expect(page.getByText('Nguyễn Mai Lan')).toBeVisible();
  expect(requests[0]).toEqual({ method: 'GET', status: 'PENDING_APPROVAL', page: '0', size: '10', sort: 'createdAt,asc' });

  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    const dimensions = await page.evaluate(() => ({ body: document.body.scrollWidth, viewport: window.innerWidth }));
    expect(dimensions.body).toBeLessThanOrEqual(dimensions.viewport);
    await page.screenshot({ path: resolve(evidence, `admin-teachers-after-${width}.png`), fullPage: true });
  }

  await page.setViewportSize({ width: 375, height: 900 });
  await page.getByRole('button', { name: 'Xem hồ sơ' }).click();
  await expect(page.getByRole('heading', { name: 'Thông tin gia sư' })).toBeVisible();
  await expect(page.getByText('Quận 3, Thành phố Hồ Chí Minh')).toBeVisible();
  await expect(page.getByRole('link', { name: 'Xem video giới thiệu' })).toHaveAttribute('href', teacher.introductionVideoUrl);
  await expect(page.getByRole('link', { name: 'Xem tài liệu' })).toHaveAttribute('href', teacher.documents[0].secureUrl);
  const drawer = page.locator('.ant-drawer-content-wrapper');
  expect(await drawer.evaluate(element => Math.round(element.getBoundingClientRect().width))).toBe(375);
  const detailDimensions = await page.evaluate(() => ({ body: document.body.scrollWidth, viewport: window.innerWidth }));
  expect(detailDimensions.body).toBeLessThanOrEqual(detailDimensions.viewport);

  await page.getByRole('button', { name: 'Đóng' }).click();
  await page.getByRole('tab', { name: 'Đã duyệt' }).click();
  await expect(page.getByRole('button', { name: 'Xem hồ sơ' })).toBeVisible();
  expect(requests.some(request => request.status === 'APPROVED')).toBe(true);
});

test('Admin can approve with the existing note payload', async ({ page }) => {
  await seedAdmin(page);
  await mockQueue(page);
  let mutation: { url: string; method: string; body: unknown } | undefined;
  await page.route('**/api/admin/teachers/teacher-001/approve', async route => {
    mutation = { url: new URL(route.request().url()).pathname, method: route.request().method(), body: route.request().postDataJSON() };
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, message: 'ok', data: { ...teacher, status: 'APPROVED' }, errors: null, meta: null }) });
  });
  await page.goto('/admin/teachers');
  await page.getByRole('button', { name: 'Xem hồ sơ' }).click();
  await page.getByLabel('Ghi chú phê duyệt (tùy chọn)').fill('Đã đối chiếu giấy tờ.');
  await page.getByRole('button', { name: 'Phê duyệt' }).last().click();
  await expect(page.getByText('Đã phê duyệt hồ sơ gia sư thành công')).toBeVisible();
  expect(mutation).toEqual({ url: '/api/admin/teachers/teacher-001/approve', method: 'POST', body: { note: 'Đã đối chiếu giấy tờ.' } });
});

test('Admin rejection requires a reason and sends the existing payload', async ({ page }) => {
  await seedAdmin(page);
  await mockQueue(page);
  let body: unknown;
  await page.route('**/api/admin/teachers/teacher-001/reject', async route => {
    body = route.request().postDataJSON();
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, message: 'ok', data: { ...teacher, status: 'REJECTED' }, errors: null, meta: null }) });
  });
  await page.goto('/admin/teachers');
  await page.getByRole('button', { name: 'Xem hồ sơ' }).click();
  await page.getByRole('button', { name: 'Từ chối' }).click();
  await page.getByRole('button', { name: 'Xác nhận từ chối' }).click();
  await expect(page.getByText('Vui lòng nhập lý do từ chối hồ sơ')).toBeVisible();
  await page.getByLabel('Lý do từ chối (bắt buộc)').fill('Vui lòng bổ sung minh chứng học vấn.');
  await page.getByRole('button', { name: 'Xác nhận từ chối' }).click();
  await expect(page.getByText('Đã từ chối hồ sơ gia sư')).toBeVisible();
  expect(body).toEqual({ reason: 'Vui lòng bổ sung minh chứng học vấn.' });
});

test('Admin teacher queue renders recoverable API error and empty state', async ({ page }) => {
  await seedAdmin(page);
  await mockQueue(page, { failFirst: 4 });
  await page.goto('/admin/teachers');
  await expect(page.getByText('Chưa tải được danh sách hồ sơ.')).toBeVisible({ timeout: 15_000 });
  await page.getByRole('button', { name: 'Tải lại' }).click();
  await expect(page.getByText('Nguyễn Mai Lan')).toBeVisible();
});

test('Admin teacher queue shows an explicit empty state', async ({ page }) => {
  await seedAdmin(page);
  await mockQueue(page, { empty: true });
  await page.goto('/admin/teachers');
  await expect(page.getByText('Không có hồ sơ chờ duyệt ở trang này.')).toBeVisible();
});
