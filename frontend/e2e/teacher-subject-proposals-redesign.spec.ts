import { expect, test, type Page } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const teacher = { id: 'teacher-proposal-fixture', email: 'teacher@example.test', fullName: 'Nguyễn Minh An', role: 'TEACHER', status: 'ACTIVE' };
const envelope = (data: unknown) => ({ success: true, message: 'ok', data, errors: null, meta: null });
const failed = { success: false, message: 'temporary error', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null };
const proposals = [
  { id: 'proposal-1', proposedName: 'Khoa học máy tính', educationLevel: 'HIGH_SCHOOL', description: 'Dạy lập trình cơ bản', status: 'PENDING', reviewNote: null },
  { id: 'proposal-2', proposedName: 'Văn học so sánh', educationLevel: 'UNIVERSITY', description: 'Nghiên cứu các tác phẩm', status: 'APPROVED', reviewNote: 'Đã thêm vào danh mục.' },
  { id: 'proposal-3', proposedName: 'Thiên văn học', educationLevel: 'OTHER', description: 'Nhập môn quan sát bầu trời', status: 'REJECTED', reviewNote: 'Vui lòng bổ sung phạm vi môn học.' },
];

async function setup(page: Page, options: { empty?: boolean; failGet?: boolean; holdGet?: boolean; failPost?: boolean } = {}) {
  const requests: Array<{ method: string; path: string; query: string; body?: string }> = [];
  let failGet = options.failGet ?? false;
  let failPost = options.failPost ?? false;
  let release: (() => void) | undefined;
  const gate = options.holdGet ? new Promise<void>(resolve => { release = resolve; }) : null;
  await page.route('**/api/teacher/subject-proposals**', async route => {
    const request = route.request();
    const url = new URL(request.url());
    requests.push({ method: request.method(), path: url.pathname, query: url.search, body: request.postData() ?? undefined });
    if (request.method() === 'GET') {
      if (gate) await gate;
      if (failGet) return route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify(failed) });
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(options.empty ? [] : proposals)) });
    }
    if (request.method() === 'POST') {
      if (failPost) return route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify(failed) });
      return route.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify(envelope({ id: 'proposal-new', proposedName: 'Sinh học', educationLevel: 'HIGH_SCHOOL', description: 'Cấp ba', status: 'PENDING' })) });
    }
    return route.fulfill({ status: 405, body: '' });
  });
  return { requests, retry: () => { failGet = false; }, recoverPost: () => { failPost = false; }, release: () => release?.() };
}

async function openPage(page: Page) {
  await page.addInitScript(user => {
    try {
      localStorage.setItem('tutor-match.session', JSON.stringify({ user, remember: false, sessionId: 'proposal-fixture-session' }));
      document.cookie = 'accessToken=proposal-fixture-access; path=/; SameSite=Lax';
    } catch {
      // The initial about:blank document has no storage origin; the route navigation does.
    }
  }, teacher);
  await page.goto('/teacher/subject-proposals');
}

test('teacher proposal history displays statuses, remains readable and fits 375/768/1280', async ({ page }) => {
  const { requests } = await setup(page);
  await openPage(page);
  await expect(page.getByRole('heading', { name: variant === 'before' ? /Đề xuất Môn học mới/ : 'Đề xuất môn học' })).toBeVisible();
  await expect(page.getByText('Khoa học máy tính')).toBeVisible();
  await expect(page.getByText('Đã thêm vào danh mục.')).toBeVisible();
  await expect(page.getByText('Vui lòng bổ sung phạm vi môn học.')).toBeVisible();
  expect(requests).toContainEqual(expect.objectContaining({ method: 'GET', path: '/api/teacher/subject-proposals', query: '?size=100' }));
  await page.keyboard.press('Tab');
  await expect(page.locator(':focus-visible').first()).toBeVisible();
  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    if (variant !== 'before') expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidence, `teacher-subject-proposals-${variant}-${width}.png`), fullPage: true });
  }
});

test('proposal form validates, posts the existing contract and prepends server result', async ({ page }) => {
  const { requests } = await setup(page);
  await openPage(page);
  const submit = page.getByRole('button', { name: 'Gửi đề xuất' });
  await submit.click();
  await expect(page.getByText('Vui lòng nhập tên môn học')).toBeVisible();
  await page.getByLabel('Tên môn học').fill('Sinh học');
  await page.locator('#educationLevel').click();
  await page.getByText('THPT', { exact: true }).last().click();
  await page.getByLabel('Mô tả').fill('Cấp ba');
  await submit.click();
  await expect(page.getByText('Gửi đề xuất thành công!')).toBeVisible();
  await expect(page.getByText('Sinh học').first()).toBeVisible();
  const post = requests.find(request => request.method === 'POST');
  expect(post?.path).toBe('/api/teacher/subject-proposals');
  expect(JSON.parse(post?.body ?? '{}')).toEqual({ proposedName: 'Sinh học', educationLevel: 'HIGH_SCHOOL', description: 'Cấp ba' });
});

test('loading, empty, GET error/retry and POST failure stay distinct', async ({ page }) => {
  const loading = await setup(page, { holdGet: true });
  await openPage(page);
  await expect(page.getByRole('status')).toContainText('Đang tải danh sách đề xuất');
  loading.release();
  await expect(page.getByText('Khoa học máy tính')).toBeVisible();

  await page.unrouteAll({ behavior: 'wait' });
  const retry = await setup(page, { failGet: true });
  await page.reload();
  await expect(page.getByText('Không thể tải danh sách đề xuất.')).toBeVisible();
  retry.retry();
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByText('Khoa học máy tính')).toBeVisible();

  await page.unrouteAll({ behavior: 'wait' });
  const empty = await setup(page, { empty: true, failPost: true });
  await page.reload();
  await expect(page.getByText('Bạn chưa gửi đề xuất môn học nào.')).toBeVisible();
  await page.getByLabel('Tên môn học').fill('Sinh học');
  await page.locator('#educationLevel').click();
  await page.getByText('THPT', { exact: true }).last().click();
  await page.getByRole('button', { name: 'Gửi đề xuất' }).click();
  await expect(page.getByText('Gửi đề xuất thất bại.')).toBeVisible();
  empty.recoverPost();
  await expect(page.getByLabel('Tên môn học')).toHaveValue('Sinh học');
});
