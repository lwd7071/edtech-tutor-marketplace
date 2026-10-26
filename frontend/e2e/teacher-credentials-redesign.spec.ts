import { expect, test, type Page } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const teacher = { id: 'teacher-credentials-fixture', email: 'teacher-credentials@example.test', fullName: 'Nguyễn Minh An', role: 'TEACHER', status: 'ACTIVE' };
const envelope = (data: unknown) => ({ success: true, message: 'ok', data, errors: null, meta: null });
const failed = { success: false, message: 'temporary error', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null };
const fixtures = [
  { id: 'pending-id', label: 'Chứng chỉ giảng dạy Toán', status: 'PENDING', proofUrl: 'https://private.example.test/pending.pdf', version: 2 },
  { id: 'approved-id', label: 'Cử nhân Sư phạm', status: 'APPROVED', version: 4 },
  { id: 'rejected-id', label: 'IELTS Academic 7.5', status: 'REJECTED', rejectedReason: 'Ảnh tải lên bị mờ', version: 3 },
];

async function setup(page: Page, options: { failGet?: boolean; holdGet?: boolean; empty?: boolean } = {}) {
  const requests: Array<{ method: string; path: string; query: string; body?: Buffer }> = [];
  let failGet = options.failGet ?? false;
  let release: (() => void) | undefined;
  const gate = options.holdGet ? new Promise<void>(resolve => { release = resolve; }) : null;
  await page.route('**/api/auth/login', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope({ accessToken: 'credential-access', refreshToken: 'credential-refresh', user: teacher })) }));
  await page.route('**/api/teacher/credentials**', async route => {
    const request = route.request();
    const url = new URL(request.url());
    requests.push({ method: request.method(), path: url.pathname, query: url.search, body: request.postDataBuffer() ?? undefined });
    if (request.method() === 'GET' && url.pathname.endsWith('/proof')) return route.fulfill({ status: 200, contentType: 'application/pdf', body: Buffer.from('%PDF-1.4 fixture') });
    if (request.method() === 'GET') {
      if (gate) await gate;
      if (failGet) return route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify(failed) });
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(options.empty ? [] : fixtures)) });
    }
    if (request.method() === 'POST' || request.method() === 'PUT') return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope({ id: 'saved-credential', label: 'Chứng chỉ mới', status: 'PENDING', version: 1 })) });
    if (request.method() === 'DELETE') return route.fulfill({ status: 204, body: '' });
    return route.fulfill({ status: 405, body: '' });
  });
  return { requests, retry: () => { failGet = false; }, release: () => release?.() };
}

async function openPage(page: Page) {
  await page.addInitScript((user) => {
    localStorage.setItem('tutor-match.session', JSON.stringify({ user, remember: false, sessionId: 'credentials-fixture-session' }));
    document.cookie = 'accessToken=credentials-fixture-access; path=/; SameSite=Lax';
  }, teacher);
  await page.goto('/teacher/credentials');
}

test('teacher credentials shows separated review states and responsive layout', async ({ page }) => {
  const { requests } = await setup(page);
  await openPage(page);
  await expect(page.getByRole('heading', { name: 'Minh chứng năng lực' })).toBeVisible();
  await expect(page.getByText('Chờ duyệt')).toBeVisible();
  await expect(page.getByText('Đã duyệt')).toBeVisible();
  await expect(page.getByText('Cần bổ sung')).toBeVisible();
  await expect(page.getByText('Lý do từ chối: Ảnh tải lên bị mờ')).toBeVisible();
  expect(requests.some(request => request.method === 'GET' && request.path === '/api/teacher/credentials')).toBe(true);
  await page.keyboard.press('Tab');
  await expect(page.locator(':focus-visible').first()).toBeVisible();
  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidence, `teacher-credentials-${variant}-${width}.png`), fullPage: true });
    if (width === 375) {
      const clearance = await page.evaluate(async () => {
        const submitButton = document.querySelector('form button[type="submit"]');
        let scroller = submitButton?.parentElement ?? null;
        while (scroller) {
          const style = getComputedStyle(scroller);
          if (scroller.scrollHeight > scroller.clientHeight + 1 && /(auto|scroll)/.test(style.overflowY)) break;
          scroller = scroller.parentElement;
        }
        if (scroller) scroller.scrollTop = scroller.scrollHeight;
        else {
          document.documentElement.style.scrollBehavior = 'auto';
          window.scrollTo(0, document.documentElement.scrollHeight);
          await new Promise(resolve => setTimeout(resolve, 100));
        }
        const submit = submitButton?.getBoundingClientRect();
        const bottomNav = [...document.querySelectorAll('nav')].map(node => ({ node, rect: node.getBoundingClientRect(), style: getComputedStyle(node) })).find(entry => entry.style.position === 'fixed' && entry.rect.bottom >= window.innerHeight - 1);
        return { submitBottom: submit?.bottom, navTop: bottomNav?.rect.top };
      });
      expect(clearance.submitBottom).toBeDefined();
      expect(clearance.navTop).toBeDefined();
      expect(clearance.submitBottom!).toBeLessThan(clearance.navTop! - 8);
    }
  }
});

test('teacher credential editing preserves version and multipart proof contract', async ({ page }) => {
  const { requests } = await setup(page);
  await openPage(page);
  const row = page.getByRole('listitem').filter({ hasText: 'IELTS Academic 7.5' });
  await row.getByRole('button', { name: 'Cập nhật và gửi lại' }).click();
  await expect(page.getByRole('heading', { name: 'Cập nhật minh chứng' })).toBeVisible();
  await expect(page.getByLabel('Tên chứng chỉ hoặc bằng cấp')).toHaveValue('IELTS Academic 7.5');
  await page.getByLabel('Tên chứng chỉ hoặc bằng cấp').fill('IELTS Academic 8.0');
  await expect(page.getByRole('button', { name: 'Chọn file' })).toBeVisible();
  await page.locator('input[type=file]').setInputFiles({ name: 'ielts.pdf', mimeType: 'application/pdf', buffer: Buffer.from('%PDF-1.4') });
  await page.getByRole('button', { name: 'Cập nhật và gửi lại' }).last().click();
  await expect(page.getByText('Đã gửi minh chứng để duyệt.', { exact: true })).toBeVisible();
  const update = requests.find(request => request.method === 'PUT');
  expect(update?.path).toBe('/api/teacher/credentials/rejected-id');
  expect(update?.body?.toString()).toContain('name="label"');
  expect(update?.body?.toString()).toContain('IELTS Academic 8.0');
  expect(update?.body?.toString()).toContain('name="version"');
  expect(update?.body?.toString()).toContain('3');
  expect(update?.body?.toString()).toContain('ielts.pdf');

  const approved = page.getByRole('listitem').filter({ hasText: 'Cử nhân Sư phạm' });
  await approved.getByRole('button', { name: 'Chỉnh sửa' }).click();
  await expect(page.getByText('Khi cập nhật, mục này sẽ tạm ẩn khỏi hồ sơ công khai cho đến khi được duyệt lại.')).toBeVisible();
  await page.getByLabel('Tên chứng chỉ hoặc bằng cấp').fill('Cử nhân Sư phạm Toán');
  await page.getByRole('form', { name: 'Cập nhật minh chứng' }).getByRole('button', { name: 'Cập nhật và gửi lại' }).click();
  const updateWithoutNewProof = requests.filter(request => request.method === 'PUT').pop();
  expect(updateWithoutNewProof?.body?.toString()).toContain('name="version"');
  expect(updateWithoutNewProof?.body?.toString()).toContain('4');
  expect(updateWithoutNewProof?.body?.toString()).not.toContain('name="proof"');
});

test('teacher can create and delete credentials with the existing API methods', async ({ page }) => {
  const { requests } = await setup(page);
  await openPage(page);
  await page.getByLabel('Tên chứng chỉ hoặc bằng cấp').fill('Chứng chỉ mới');
  await page.locator('input[type=file]').setInputFiles({ name: 'new-proof.png', mimeType: 'image/png', buffer: Buffer.from('image fixture') });
  await page.getByRole('button', { name: 'Gửi duyệt' }).click();
  await expect(page.getByText('Đã gửi minh chứng để duyệt.', { exact: true })).toBeVisible();
  const create = requests.find(request => request.method === 'POST');
  expect(create?.path).toBe('/api/teacher/credentials');
  expect(create?.body?.toString()).toContain('name="label"');
  expect(create?.body?.toString()).toContain('Chứng chỉ mới');
  expect(create?.body?.toString()).toContain('new-proof.png');

  page.once('dialog', dialog => dialog.accept());
  await page.getByRole('listitem').filter({ hasText: 'Cử nhân Sư phạm' }).getByRole('button', { name: 'Xóa' }).click();
  expect(requests.some(request => request.method === 'DELETE' && request.path === '/api/teacher/credentials/approved-id' && request.query.includes('version=4'))).toBe(true);
});

test('teacher credential proof opens private blob viewer and closes', async ({ page }) => {
  const { requests } = await setup(page);
  await openPage(page);
  await page.getByRole('listitem').filter({ hasText: 'Cử nhân Sư phạm' }).getByRole('button', { name: 'Xem file' }).click();
  await expect(page.getByRole('dialog', { name: 'Xem file minh chứng' })).toBeVisible();
  expect(requests.some(request => request.method === 'GET' && request.path === '/api/teacher/credentials/approved-id/proof')).toBe(true);
  await expect(page.getByText(/private\.example\.test/)).toHaveCount(0);
  await page.getByRole('button', { name: 'Đóng' }).click();
  await expect(page.getByRole('dialog')).toHaveCount(0);
});

test('teacher credentials keeps loading, empty, error and retry states distinct', async ({ page }) => {
  const loading = await setup(page, { holdGet: true });
  await openPage(page);
  await expect(page.getByRole('status')).toContainText('Đang tải minh chứng');
  loading.release();
  await expect(page.getByText('Chờ duyệt')).toBeVisible();

  await page.unrouteAll({ behavior: 'wait' });
  const retryFixture = await setup(page, { failGet: true });
  await page.reload();
  await expect(page.getByText('Không tải được minh chứng. Hãy thử lại.')).toBeVisible();
  retryFixture.retry();
  await page.getByRole('button', { name: 'Thử tải lại' }).click();
  await expect(page.getByText('Chờ duyệt')).toBeVisible();

  await page.unrouteAll({ behavior: 'wait' });
  await setup(page, { empty: true });
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Chưa có minh chứng nào' })).toBeVisible();
});
