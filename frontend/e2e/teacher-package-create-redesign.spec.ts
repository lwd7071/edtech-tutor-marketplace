import { expect, test, type Page } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const teacher = { id: 'teacher-create-package-fixture', email: 'teacher-create@example.test', fullName: 'Nguyễn Minh An', role: 'TEACHER', status: 'ACTIVE' };
const envelope = (data: unknown, meta: unknown = null) => ({ success: true, message: 'ok', data, errors: null, meta });
const failed = { success: false, message: 'temporary error', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null };

async function setup(page: Page, options: { failSubjects?: boolean; holdSubjects?: boolean; failPost?: boolean } = {}) {
  const requests: Array<{ method: string; path: string; query: string; body?: string }> = [];
  let failSubjects = options.failSubjects ?? false;
  let failPost = options.failPost ?? false;
  let release: (() => void) | undefined;
  const gate = options.holdSubjects ? new Promise<void>(resolve => { release = resolve; }) : null;
  await page.route('**/api/teacher/subjects', async route => {
    const request = route.request();
    const url = new URL(request.url());
    requests.push({ method: request.method(), path: url.pathname, query: url.search, body: request.postData() ?? undefined });
    if (gate) await gate;
    if (failSubjects) return route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify(failed) });
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope([{ id: 'link-math', subject: { id: 'subject-math', name: 'Toán học', educationLevel: 'HIGH_SCHOOL' } }])) });
  });
  await page.route('**/api/teacher/profile', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope({ profileStatus: 'APPROVED' })) }));
  await page.route('**/api/teacher/packages', async route => {
    const request = route.request();
    const url = new URL(request.url());
    requests.push({ method: request.method(), path: url.pathname, query: url.search, body: request.postData() ?? undefined });
    if (request.method() === 'GET') return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope([], { page: 0, size: 12, totalElements: 0, totalPages: 0 })) });
    if (failPost) return route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify(failed) });
    return route.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify(envelope({ id: 'new-package', name: 'Toán 10 buổi', subjectId: 'subject-math', priceVnd: 500000, totalSessions: 10, sessionDurationMinutes: 60, durationDays: 60, status: 'DRAFT', version: 1 })) });
  });
  return { requests, retrySubjects: () => { failSubjects = false; }, retryPost: () => { failPost = false; }, release: () => release?.() };
}

async function openPage(page: Page) {
  await page.addInitScript(user => {
    try {
      localStorage.setItem('tutor-match.session', JSON.stringify({ user, remember: false, sessionId: 'create-package-session' }));
      document.cookie = 'accessToken=create-package-access; path=/; SameSite=Lax';
    } catch {
      // The initial about:blank document has no storage origin; the route navigation does.
    }
  }, teacher);
  await page.goto('/teacher/packages/create');
}

async function openEditPage(page: Page) {
  await page.addInitScript(user => {
    try {
      localStorage.setItem('tutor-match.session', JSON.stringify({ user, remember: false, sessionId: 'edit-package-session' }));
      document.cookie = 'accessToken=edit-package-access; path=/; SameSite=Lax';
    } catch {
      // The initial about:blank document has no storage origin; the route navigation does.
    }
  }, teacher);
  await page.goto('/teacher/packages/pkg-edit-1');
}

async function fillPackageForm(page: Page) {
  await page.getByLabel('Tên gói học').fill('Toán 10 buổi');
  await page.getByLabel('Môn học').click();
  await page.getByText('Toán học', { exact: true }).last().click();
  await page.getByLabel('Nội dung và mục tiêu').fill('Ôn tập theo chuyên đề');
  await page.getByLabel('Số buổi').fill('10');
  await page.getByLabel('Thời lượng mỗi buổi (phút)').fill('60');
  await page.getByLabel('Hạn sử dụng (ngày)').fill('60');
  await page.getByLabel('Giá trọn gói (VNĐ)').fill('500000');
  await page.getByLabel('Trạng thái').click();
  await page.getByText('Bản nháp', { exact: true }).last().click();
  await expect(page.locator('.ant-select-dropdown:visible')).toHaveCount(0);
}

test('package creation layout and live summary fit 375/768/1280', async ({ page }) => {
  const { requests } = await setup(page);
  await openPage(page);
  await expect(page.getByRole('heading', { name: 'Tạo gói học' })).toBeVisible();
  await fillPackageForm(page);
  if (variant !== 'before') {
    await expect(page.getByRole('heading', { name: 'Tóm tắt gói học' })).toBeVisible();
    await expect(page.getByText('500.000đ')).toBeVisible();
  }
  expect(requests).toContainEqual(expect.objectContaining({ method: 'GET', path: '/api/teacher/subjects', query: '' }));
  await page.keyboard.press('Tab');
  await expect(page.locator(':focus-visible').first()).toBeVisible();
  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    if (variant !== 'before') expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidence, `teacher-package-create-${variant}-${width}.png`), fullPage: true });
    if (width === 375 && variant !== 'before') {
      await page.evaluate(async () => {
        document.documentElement.style.scrollBehavior = 'auto';
        window.scrollTo(0, document.documentElement.scrollHeight);
        await new Promise(resolve => setTimeout(resolve, 100));
      });
      const clearance = await page.evaluate(() => {
        const submit = [...document.querySelectorAll('button')].find(button => button.textContent?.trim() === 'Lưu gói học');
        const nav = [...document.querySelectorAll('nav')].map(node => ({ rect: node.getBoundingClientRect(), style: getComputedStyle(node) })).find(item => item.style.position === 'fixed' && item.rect.bottom >= window.innerHeight - 1);
        return { submitBottom: submit?.getBoundingClientRect().bottom, navTop: nav?.rect.top };
      });
      expect(clearance.submitBottom).toBeDefined();
      expect(clearance.navTop).toBeDefined();
      expect(clearance.submitBottom!).toBeLessThan(clearance.navTop! - 8);
    }
  }
});

test('create sends the exact API payload with version zero and returns to package list', async ({ page }) => {
  const { requests } = await setup(page);
  await openPage(page);
  await fillPackageForm(page);
  await page.getByRole('button', { name: 'Lưu gói học' }).click();
  await expect(page.getByText('Đã lưu gói học')).toBeVisible();
  await expect(page).toHaveURL(/\/teacher\/packages$/);
  const create = requests.find(request => request.method === 'POST');
  expect(create?.path).toBe('/api/teacher/packages');
  expect(JSON.parse(create?.body ?? '{}')).toEqual({ subjectId: 'subject-math', name: 'Toán 10 buổi', description: 'Ôn tập theo chuyên đề', totalSessions: 10, sessionDurationMinutes: 60, durationDays: 60, priceVnd: 500000, status: 'DRAFT', version: 0 });
});

test('subject loading, GET error/retry, required fields and POST error preserve the form', async ({ page }) => {
  const loading = await setup(page, { holdSubjects: true });
  await openPage(page);
  await expect(page.getByRole('status')).toContainText('Đang tải dữ liệu gói học');
  loading.release();
  await expect(page.getByLabel('Tên gói học')).toBeVisible();

  await page.unrouteAll({ behavior: 'wait' });
  const retry = await setup(page, { failSubjects: true });
  await page.reload();
  await expect(page.getByText('Chưa tải được dữ liệu gói')).toBeVisible();
  retry.retrySubjects();
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByLabel('Tên gói học')).toBeVisible();

  await page.getByRole('button', { name: 'Lưu gói học' }).click();
  await expect(page.locator('.ant-form-item-explain-error').first()).toBeVisible();
  await fillPackageForm(page);
  await page.unrouteAll({ behavior: 'wait' });
  const failedPost = await setup(page, { failPost: true });
  await page.getByRole('button', { name: 'Lưu gói học' }).click();
  await expect(page.locator('.ant-message-notice')).toBeVisible();
  expect(failedPost.requests.some(request => request.method === 'POST' && request.path === '/api/teacher/packages')).toBe(true);
  failedPost.retryPost();
  await expect(page.getByLabel('Tên gói học')).toHaveValue('Toán 10 buổi');
});

test('edit package displays saved status and current values at 375/768/1280', async ({ page }) => {
  const { requests } = await setup(page);
  const pkg = { id: 'pkg-edit-1', name: 'Toán căn bản', subjectId: 'subject-math', subjectName: 'Toán học', description: 'Nền tảng đại số', totalSessions: 8, sessionDurationMinutes: 45, durationDays: 90, priceVnd: 640000, status: 'ACTIVE', version: 6 };
  await page.route('**/api/teacher/packages/pkg-edit-1', async route => {
    const request = route.request();
    requests.push({ method: request.method(), path: new URL(request.url()).pathname, query: '', body: request.postData() ?? undefined });
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(pkg)) });
  });
  await openEditPage(page);
  await expect(page.getByRole('heading', { name: 'Chỉnh sửa gói học' })).toBeVisible();
  await expect(page.getByLabel('Tên gói học')).toHaveValue('Toán căn bản');
  if (variant !== 'before') {
    await expect(page.getByLabel('Trạng thái gói đã lưu')).toContainText('Đang mở bán');
    await expect(page.getByText('640.000đ')).toBeVisible();
  }
  expect(requests).toContainEqual(expect.objectContaining({ method: 'GET', path: '/api/teacher/packages/pkg-edit-1' }));
  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    if (variant !== 'before') expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidence, `teacher-package-edit-${variant}-${width}.png`), fullPage: true });
    if (width === 375 && variant !== 'before') {
      await page.evaluate(async () => {
        document.documentElement.style.scrollBehavior = 'auto';
        window.scrollTo(0, document.documentElement.scrollHeight);
        await new Promise(resolve => setTimeout(resolve, 100));
      });
      const clearance = await page.evaluate(() => {
        const submit = [...document.querySelectorAll('button')].find(button => button.textContent?.trim() === 'Lưu gói học');
        const nav = [...document.querySelectorAll('nav')].map(node => ({ rect: node.getBoundingClientRect(), style: getComputedStyle(node) })).find(item => item.style.position === 'fixed' && item.rect.bottom >= window.innerHeight - 1);
        return { submitBottom: submit?.getBoundingClientRect().bottom, navTop: nav?.rect.top };
      });
      expect(clearance.submitBottom).toBeDefined();
      expect(clearance.navTop).toBeDefined();
      expect(clearance.submitBottom!).toBeLessThan(clearance.navTop! - 8);
    }
  }
});

test('edit uses the loaded version and preserves the form on conflict before retry', async ({ page }) => {
  const { requests } = await setup(page);
  let version = 6;
  let conflict = true;
  const pkg = () => ({ id: 'pkg-edit-1', name: 'Toán căn bản', subjectId: 'subject-math', subjectName: 'Toán học', description: 'Nền tảng đại số', totalSessions: 8, sessionDurationMinutes: 45, durationDays: 90, priceVnd: 640000, status: 'ACTIVE', version });
  await page.route('**/api/teacher/packages/pkg-edit-1', async route => {
    const request = route.request();
    requests.push({ method: request.method(), path: new URL(request.url()).pathname, query: '', body: request.postData() ?? undefined });
    if (request.method() === 'GET') return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(pkg())) });
    const body = JSON.parse(request.postData() ?? '{}');
    if (conflict) {
      conflict = false;
      version = 7;
      return route.fulfill({ status: 409, contentType: 'application/json', body: JSON.stringify({ ...failed, errors: [{ code: 'CONCURRENT_MODIFICATION' }] }) });
    }
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope({ ...pkg(), ...body })) });
  });
  await openEditPage(page);
  await page.getByLabel('Tên gói học').fill('Toán căn bản nâng cao');
  await page.getByRole('button', { name: 'Lưu gói học' }).click();
  await expect(page.getByLabel('Tên gói học')).toHaveValue('Toán căn bản nâng cao');
  await expect(page.locator('.ant-message-notice')).toBeVisible();
  await page.getByRole('button', { name: 'Lưu gói học' }).click();
  await expect(page.getByText('Đã lưu gói học')).toBeVisible();
  await expect(page).toHaveURL(/\/teacher\/packages$/);
  const updates = requests.filter(request => request.method === 'PUT');
  expect(JSON.parse(updates[0].body ?? '{}')).toEqual({ subjectId: 'subject-math', name: 'Toán căn bản nâng cao', description: 'Nền tảng đại số', totalSessions: 8, sessionDurationMinutes: 45, durationDays: 90, priceVnd: 640000, status: 'ACTIVE', version: 6 });
  expect(JSON.parse(updates[1].body ?? '{}').version).toBe(7);
});
