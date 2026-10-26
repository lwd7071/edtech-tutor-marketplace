import { expect, test, type Page } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const teacher = { id: 'teacher-profile-fixture', email: 'teacher-profile@example.test', fullName: 'Nguyễn Minh An', role: 'TEACHER', status: 'ACTIVE' };
const envelope = (data: unknown) => ({ success: true, message: 'ok', data, errors: null, meta: null });
const failed = { success: false, message: 'Không lưu được thông tin', data: null, errors: [{ code: 'CONCURRENT_MODIFICATION' }], meta: null };

type Status = 'DRAFT' | 'PENDING_APPROVAL' | 'REJECTED' | 'APPROVED';

async function mockProfile(page: Page, options: { status?: Status; profileError?: boolean; profileUpdateStatus?: number; holdProfileUntilRelease?: boolean } = {}) {
  const requests: Array<{ method: string; path: string; body?: unknown }> = [];
  let profileError = options.profileError ?? false;
  let releaseProfileGate: (() => void) | undefined;
  const profileGate = options.holdProfileUntilRelease ? new Promise<void>(resolve => { releaseProfileGate = resolve; }) : null;
  await page.route('**/api/auth/login', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope({ accessToken: 'teacher-profile-access', refreshToken: 'teacher-profile-refresh', user: teacher })) }));
  await page.route('**/api/teacher/profile', async route => {
    requests.push({ method: route.request().method(), path: new URL(route.request().url()).pathname, body: route.request().postDataJSON() });
    if (route.request().method() === 'GET') {
      if (profileGate) await profileGate;
      if (profileError) return route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify(failed) });
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope({ id: 'profile-fixture', bio: 'Dạy Toán theo mục tiêu từng buổi.', yearsOfExperience: 5, languages: ['Tiếng Việt'], supportsOnline: true, supportsOffline: false, provinceCode: '01', wardCode: '01001', introductionVideoUrl: '', profileStatus: options.status ?? 'DRAFT', rejectionReason: options.status === 'REJECTED' ? 'Bổ sung kinh nghiệm giảng dạy.' : null })) });
    }
    return route.fulfill({ status: options.profileUpdateStatus ?? 200, contentType: 'application/json', body: JSON.stringify(options.profileUpdateStatus ? failed : envelope({})) });
  });
  await page.route('**/api/teacher/profile/residence', route => {
    requests.push({ method: route.request().method(), path: new URL(route.request().url()).pathname, body: route.request().postDataJSON() });
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope({})) });
  });
  await page.route('**/api/teacher/profile/submit', route => {
    requests.push({ method: route.request().method(), path: new URL(route.request().url()).pathname });
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope({})) });
  });
  await page.route('**/api/public/locations/provinces', route => {
    requests.push({ method: route.request().method(), path: new URL(route.request().url()).pathname });
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope([{ code: '01', name: 'Hà Nội' }, { code: '79', name: 'Hồ Chí Minh' }])) });
  });
  await page.route('**/api/public/locations/provinces/*/wards', route => {
    requests.push({ method: route.request().method(), path: new URL(route.request().url()).pathname });
    const data = new URL(route.request().url()).pathname.includes('/79/')
      ? [{ code: '79001', name: 'Quận 1' }, { code: '79002', name: 'Bình Thạnh' }]
      : [{ code: '01001', name: 'Ba Đình' }, { code: '01002', name: 'Hoàn Kiếm' }];
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(data)) });
  });
  return { requests, recoverProfile: () => { profileError = false; }, releaseProfile: () => releaseProfileGate?.() };
}

async function openProfile(page: Page) {
  await page.goto('/auth/login');
  await page.getByPlaceholder('Nhập email của bạn').fill(teacher.email);
  await page.getByPlaceholder('Nhập mật khẩu').fill('Password123!');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page).toHaveURL(/\/teacher$/);
  await page.goto('/teacher/profile');
}

test('teacher profile lays out source profile, API-backed residence and actions at supported widths', async ({ page }) => {
  const { requests } = await mockProfile(page);
  await openProfile(page);
  await expect(page.getByRole('heading', { name: 'Hồ sơ gia sư' })).toBeVisible();
  await page.keyboard.press('Tab');
  await expect(page.locator(':focus-visible').first()).toBeVisible();
  await expect(page.getByText('Hồ sơ bản nháp')).toBeVisible();
  await expect(page.getByLabel('Giới thiệu và phương pháp giảng dạy')).toHaveValue('Dạy Toán theo mục tiêu từng buổi.');
  await expect(page.getByRole('heading', { name: 'Nơi ở' })).toBeVisible();
  await expect(page.getByText('Dạy trực tiếp', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Gửi hồ sơ xét duyệt' })).toBeVisible();
  expect(requests.some(request => request.method === 'GET' && request.path === '/api/teacher/profile')).toBe(true);
  expect(requests.some(request => request.path === '/api/public/locations/provinces')).toBe(true);
  expect(requests.some(request => request.path === '/api/public/locations/provinces/01/wards')).toBe(true);

  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidence, `teacher-profile-${variant}-${width}.png`), fullPage: true });
  }
  for (const width of [375, 768]) {
    await page.setViewportSize({ width, height: 900 });
    await page.evaluate(() => window.scrollTo(0, document.documentElement.scrollHeight));
    const submit = await page.getByRole('button', { name: 'Gửi hồ sơ xét duyệt' }).boundingBox();
    const mobileNav = await page.getByRole('navigation', { name: 'Điều hướng nhanh' }).boundingBox();
    expect(submit).not.toBeNull();
    expect(mobileNav).not.toBeNull();
    expect(submit!.y + submit!.height).toBeLessThanOrEqual(mobileNav!.y + 1);
  }
});

for (const [status, label, submitVisible] of [
  ['DRAFT', 'Hồ sơ bản nháp', true],
  ['PENDING_APPROVAL', 'Hồ sơ đang chờ duyệt', false],
  ['REJECTED', 'Hồ sơ cần bổ sung', true],
  ['APPROVED', 'Hồ sơ đã được duyệt', false],
] as const) {
  test(`teacher profile status ${status} keeps edit and submit rules`, async ({ page }) => {
    await mockProfile(page, { status });
    await openProfile(page);
    await expect(page.getByText(label)).toBeVisible();
    const bio = page.getByLabel('Giới thiệu và phương pháp giảng dạy');
    if (status === 'PENDING_APPROVAL') await expect(bio).toBeDisabled();
    else await expect(bio).toBeEnabled();
    if (submitVisible) await expect(page.getByRole('button', { name: 'Gửi hồ sơ xét duyệt' })).toBeVisible();
    else await expect(page.getByRole('button', { name: 'Gửi hồ sơ xét duyệt' })).toHaveCount(0);
    if (status === 'REJECTED') await expect(page.getByText('Bổ sung kinh nghiệm giảng dạy.')).toBeVisible();
  });
}

test('teacher profile preserves profile and residence API payloads and submits approval', async ({ page }) => {
  const { requests } = await mockProfile(page);
  await openProfile(page);
  await page.getByLabel('Giới thiệu và phương pháp giảng dạy').fill('Dạy Toán bám sát mục tiêu từng buổi học.');
  await page.getByRole('button', { name: 'Lưu hồ sơ' }).click();
  await expect(page.getByText('Đã lưu hồ sơ.')).toBeVisible();
  const profileUpdate = requests.find(request => request.method === 'PUT' && request.path === '/api/teacher/profile');
  expect(profileUpdate?.body).toMatchObject({ bio: 'Dạy Toán bám sát mục tiêu từng buổi học.', yearsOfExperience: 5, languages: ['Tiếng Việt'], supportsOnline: true, supportsOffline: false });

  await page.getByLabel('Tỉnh/thành').click();
  await page.getByText('Hồ Chí Minh', { exact: true }).click();
  await page.getByLabel('Xã/phường').click();
  await page.getByText('Quận 1', { exact: true }).click();
  await page.getByRole('button', { name: 'Lưu nơi ở' }).click();
  await expect(page.getByText('Đã cập nhật nơi ở.')).toBeVisible();
  expect(requests.find(request => request.method === 'PUT' && request.path === '/api/teacher/profile/residence')?.body).toEqual({ provinceCode: '79', wardCode: '79001' });

  await page.getByRole('button', { name: 'Gửi hồ sơ xét duyệt' }).click();
  await expect(page.getByText('Đã gửi hồ sơ xét duyệt')).toBeVisible();
  expect(requests.some(request => request.method === 'POST' && request.path === '/api/teacher/profile/submit')).toBe(true);
});

test('teacher profile retains edited values after an API conflict', async ({ page }) => {
  await mockProfile(page, { profileUpdateStatus: 409 });
  await openProfile(page);
  const bio = page.getByLabel('Giới thiệu và phương pháp giảng dạy');
  await bio.fill('Nội dung vẫn giữ lại sau lỗi lưu.');
  await page.getByRole('button', { name: 'Lưu hồ sơ' }).click();
  await expect(page.getByText('Chưa lưu được hồ sơ. Kiểm tra thông tin và thử lại.')).toBeVisible();
  await expect(bio).toHaveValue('Nội dung vẫn giữ lại sau lỗi lưu.');

});

test('approved teacher can update residence without sending a profile approval mutation', async ({ page }) => {
  const { requests } = await mockProfile(page, { status: 'APPROVED' });
  await openProfile(page);
  await page.getByLabel('Tỉnh/thành').click();
  await page.getByText('Hồ Chí Minh', { exact: true }).click();
  await page.getByLabel('Xã/phường').click();
  await page.getByText('Quận 1', { exact: true }).click();
  await page.getByRole('button', { name: 'Lưu nơi ở' }).click();
  await expect(page.getByText('Đã cập nhật nơi ở.')).toBeVisible();
  expect(requests.find(request => request.method === 'PUT' && request.path === '/api/teacher/profile/residence')?.body).toEqual({ provinceCode: '79', wardCode: '79001' });
  expect(requests.some(request => request.method === 'PUT' && request.path === '/api/teacher/profile')).toBe(false);
  await expect(page.getByText('Hồ sơ đã được duyệt')).toBeVisible();
});

test('teacher profile explains load failure and retries without losing the route', async ({ page }) => {
  const fixture = await mockProfile(page, { profileError: true });
  await openProfile(page);
  await expect(page.getByText('Chưa tải được hồ sơ')).toBeVisible();
  fixture.recoverProfile();
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByRole('heading', { name: 'Hồ sơ gia sư' })).toBeVisible();
  await expect(page.getByLabel('Giới thiệu và phương pháp giảng dạy')).toHaveValue('Dạy Toán theo mục tiêu từng buổi.');
});

test('teacher profile exposes a loading state while profile data is pending', async ({ page }) => {
  const fixture = await mockProfile(page, { holdProfileUntilRelease: true });
  await openProfile(page);
  await expect(page.getByRole('status', { name: 'Đang tải hồ sơ gia sư' })).toBeVisible();
  fixture.releaseProfile();
  await expect(page.getByRole('heading', { name: 'Hồ sơ gia sư' })).toBeVisible();
});
