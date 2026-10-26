import { expect, test, type Page } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const teacher = { id: 'teacher-docs-fixture', email: 'teacher-docs@example.test', fullName: 'Nguyễn Minh An', role: 'TEACHER', status: 'ACTIVE' };
const envelope = (data: unknown) => ({ success: true, message: 'ok', data, errors: null, meta: null });
const failed = { success: false, message: 'Dữ liệu tạm thời chưa tải được', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null };
const wireDocuments = [
  { id: 'identity-pending', title: 'Giay-to-ca-nhan.pdf', secureUrl: 'https://private.example.test/identity.pdf', documentType: 'IDENTITY', verificationStatus: 'PENDING', verifiedAt: '2026-10-12T09:00:00Z' },
  { id: 'identity-verified', title: 'Minh-chung-hoc-tap.png', secureUrl: 'https://private.example.test/evidence.png', documentType: 'IDENTITY', verificationStatus: 'VERIFIED', verifiedAt: '2026-10-11T09:00:00Z' },
];

async function mockDocuments(page: Page, options: { empty?: boolean; failGetUntilRetry?: boolean; failDelete?: boolean; holdGetUntilRelease?: boolean } = {}) {
  const requests: Array<{ method: string; path: string; body?: Buffer }> = [];
  let getError = options.failGetUntilRetry ?? false;
  let releaseGetGate: (() => void) | undefined;
  const getGate = options.holdGetUntilRelease ? new Promise<void>(resolve => { releaseGetGate = resolve; }) : null;
  await page.route('**/api/auth/login', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope({ accessToken: 'teacher-docs-access', refreshToken: 'teacher-docs-refresh', user: teacher })) }));
  await page.route('**/api/teacher/documents**', async route => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    requests.push({ method: request.method(), path, body: request.postDataBuffer() ?? undefined });
    if (request.method() === 'GET') {
      if (getGate) await getGate;
      if (getError) return route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify(failed) });
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(options.empty ? [] : wireDocuments)) });
    }
    if (request.method() === 'POST') {
      return route.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify(envelope({ id: 'identity-uploaded', title: 'Bang-tot-nghiep.pdf', secureUrl: 'https://private.example.test/uploaded.pdf', documentType: 'IDENTITY', verificationStatus: 'PENDING', verifiedAt: '2026-10-16T09:00:00Z' })) });
    }
    if (request.method() === 'DELETE') {
      if (options.failDelete) return route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify(failed) });
      return route.fulfill({ status: 204, body: '' });
    }
    return route.fulfill({ status: 405, body: '' });
  });
  return { requests, recoverGet: () => { getError = false; }, releaseGet: () => releaseGetGate?.() };
}

async function openDocuments(page: Page) {
  await page.goto('/auth/login');
  await page.getByPlaceholder('Nhập email của bạn').fill(teacher.email);
  await page.getByPlaceholder('Nhập mật khẩu').fill('Password123!');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page).toHaveURL(/\/teacher$/);
  await page.goto('/teacher/documents');
}

test('teacher documents displays private identity evidence and status at all breakpoints', async ({ page }) => {
  const { requests } = await mockDocuments(page);
  await openDocuments(page);
  await expect(page.getByRole('heading', { name: 'Xác minh danh tính' })).toBeVisible();
  await page.keyboard.press('Tab');
  await expect(page.locator(':focus-visible').first()).toBeVisible();
  await expect(page.getByText('Giay-to-ca-nhan.pdf')).toBeVisible();
  await expect(page.getByText('Minh-chung-hoc-tap.png')).toBeVisible();
  await expect(page.getByText('Đang chờ')).toBeVisible();
  await expect(page.getByText('Đã xác thực')).toBeVisible();
  await expect(page.getByText('Tệp này riêng tư và không hiển thị trên hồ sơ công khai.')).toBeVisible();
  expect(requests.some(request => request.method === 'GET' && request.path === '/api/teacher/documents')).toBe(true);
  await expect(page.getByRole('link', { name: /private\.example\.test/ })).toHaveCount(0);

  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidence, `teacher-documents-${variant}-${width}.png`), fullPage: true });
  }
});

test('teacher documents keeps empty, loading error and retry states separate', async ({ page }) => {
  const fixture = await mockDocuments(page, { empty: true, failGetUntilRetry: true });
  await openDocuments(page);
  await expect(page.getByRole('alert', { name: 'Không thể tải tài liệu.' })).toContainText('Không thể tải tài liệu.');
  fixture.recoverGet();
  await page.getByRole('button', { name: 'Thử tải lại' }).click();
  await expect(page.getByText('Chưa có tài liệu nào.')).toBeVisible();
  await expect(page.getByRole('alert', { name: 'Không thể tải tài liệu.' })).toHaveCount(0);
});

test('teacher uploads an identity file and removes it after confirmation', async ({ page }) => {
  const { requests } = await mockDocuments(page);
  await openDocuments(page);
  const fileInput = page.locator('input[type=file]');
  await fileInput.setInputFiles({ name: 'Bang-tot-nghiep.pdf', mimeType: 'application/pdf', buffer: Buffer.from('fixture document') });
  await expect(page.getByRole('list', { name: 'Tài liệu xác minh đã tải lên' }).getByText('Bang-tot-nghiep.pdf')).toBeVisible();
  const upload = requests.find(request => request.method === 'POST');
  expect(upload?.path).toBe('/api/teacher/documents');
  expect(upload?.body?.toString()).toContain('Bang-tot-nghiep.pdf');
  expect(upload?.body?.toString()).toContain('documentType');

  await page.getByRole('button', { name: 'Xóa Giay-to-ca-nhan.pdf' }).click();
  await expect(page.getByText('Bạn có chắc chắn muốn xóa tài liệu này không?')).toBeVisible();
  await page.getByRole('button', { name: 'Đồng ý' }).click();
  await expect(page.getByText('Giay-to-ca-nhan.pdf')).toHaveCount(0);
  expect(requests.some(request => request.method === 'DELETE' && request.path === '/api/teacher/documents/identity-pending')).toBe(true);
});

test('teacher document delete failure keeps the item visible', async ({ page }) => {
  await mockDocuments(page, { failDelete: true });
  await openDocuments(page);
  await page.getByRole('button', { name: 'Xóa Giay-to-ca-nhan.pdf' }).click();
  await page.getByRole('button', { name: 'Đồng ý' }).click();
  await expect(page.getByText('Không thể xóa tài liệu.')).toBeVisible();
  await expect(page.getByText('Giay-to-ca-nhan.pdf')).toBeVisible();
});

test('teacher documents announces loading while the list request is pending', async ({ page }) => {
  const fixture = await mockDocuments(page, { holdGetUntilRelease: true });
  await openDocuments(page);
  await expect(page.getByRole('status')).toContainText('Đang tải tài liệu...');
  fixture.releaseGet();
  await expect(page.getByText('Giay-to-ca-nhan.pdf')).toBeVisible();
});
