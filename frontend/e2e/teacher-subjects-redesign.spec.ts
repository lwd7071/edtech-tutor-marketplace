import { expect, test, type Page } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const teacher = { id: 'teacher-subjects-fixture', email: 'teacher-subjects@example.test', fullName: 'Nguyễn Minh An', role: 'TEACHER', status: 'ACTIVE' };
const envelope = (data: unknown) => ({ success: true, message: 'ok', data, errors: null, meta: null });
const failed = { success: false, message: 'temporary error', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null };
const subject = (id: string, name: string, educationLevel = 'HIGH_SCHOOL') => ({ id, subject: { id, name, educationLevel } });
const existing = [subject('math-id', 'Toán học'), subject('english-id', 'Tiếng Anh', 'UNIVERSITY')];
const publicCatalog = [
  { id: 'physics-id', name: 'Vật lý', educationLevel: 'HIGH_SCHOOL' },
  { id: 'math-id', name: 'Toán học', educationLevel: 'HIGH_SCHOOL' },
  { id: 'english-id', name: 'Tiếng Anh', educationLevel: 'UNIVERSITY' },
];

async function setup(page: Page, options: { empty?: boolean; failGet?: boolean; holdGet?: boolean } = {}) {
  const requests: Array<{ method: string; path: string; query: string; body?: string }> = [];
  let failGet = options.failGet ?? false;
  let release: (() => void) | undefined;
  const gate = options.holdGet ? new Promise<void>(resolve => { release = resolve; }) : null;
  await page.route('**/api/teacher/subjects**', async route => {
    const request = route.request();
    const url = new URL(request.url());
    requests.push({ method: request.method(), path: url.pathname, query: url.search, body: request.postData() ?? undefined });
    if (request.method() === 'GET') {
      if (gate) await gate;
      if (failGet) return route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify(failed) });
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(options.empty ? [] : existing)) });
    }
    if (request.method() === 'POST') return route.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify(envelope(subject('physics-id', 'Vật lý'))) });
    if (request.method() === 'DELETE') return route.fulfill({ status: 204, body: '' });
    return route.fulfill({ status: 405, body: '' });
  });
  await page.route('**/api/public/subjects**', async route => {
    const request = route.request();
    const url = new URL(request.url());
    requests.push({ method: request.method(), path: url.pathname, query: url.search });
    const keyword = (url.searchParams.get('keyword') ?? '').toLocaleLowerCase();
    const data = publicCatalog.filter(item => item.name.toLocaleLowerCase().includes(keyword));
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(data)) });
  });
  return { requests, retry: () => { failGet = false; }, release: () => release?.() };
}

async function openSubjects(page: Page) {
  await page.addInitScript(user => {
    localStorage.setItem('tutor-match.session', JSON.stringify({ user, remember: false, sessionId: 'subjects-fixture-session' }));
    document.cookie = 'accessToken=subjects-fixture-access; path=/; SameSite=Lax';
  }, teacher);
  await page.goto('/teacher/subjects');
}

test('teacher subjects shows current teaching list and catalog at 375, 768 and 1280px', async ({ page }) => {
  const { requests } = await setup(page);
  await openSubjects(page);
  await expect(page.getByRole('heading', { name: /Môn đang dạy|Quản lý Môn dạy/ })).toBeVisible();
  await expect(page.getByText('Toán học', { exact: true })).toBeVisible();
  await expect(page.getByText('Tiếng Anh', { exact: true })).toBeVisible();
  expect(requests.some(request => request.method === 'GET' && request.path === '/api/teacher/subjects')).toBe(true);
  await page.keyboard.press('Tab');
  await expect(page.locator(':focus-visible').first()).toBeVisible();
  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    if (variant !== 'before') expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidence, `teacher-subjects-${variant}-${width}.png`), fullPage: true });
    if (width === 375) {
      const clearance = await page.evaluate(async () => {
        const submit = [...document.querySelectorAll('button')].find(button => button.textContent?.trim() === 'Thêm môn học');
        document.documentElement.style.scrollBehavior = 'auto';
        window.scrollTo(0, document.documentElement.scrollHeight);
        await new Promise(resolve => setTimeout(resolve, 100));
        const submitRect = submit?.getBoundingClientRect();
        const nav = [...document.querySelectorAll('nav')].map(node => ({ rect: node.getBoundingClientRect(), style: getComputedStyle(node) })).find(entry => entry.style.position === 'fixed' && entry.rect.bottom >= window.innerHeight - 1);
        return { actionBottom: submitRect?.bottom, navTop: nav?.rect.top };
      });
      expect(clearance.actionBottom).toBeDefined();
      expect(clearance.navTop).toBeDefined();
      expect(clearance.actionBottom!).toBeLessThan(clearance.navTop! - 8);
    }
  }
});

test('teacher can search and add a catalog subject while duplicate selection is blocked', async ({ page }) => {
  const { requests } = await setup(page);
  await openSubjects(page);
  const search = page.getByRole('combobox', { name: 'Tìm kiếm môn học' });
  await search.fill('Vật');
  await expect(page.getByText('Vật lý (HIGH_SCHOOL)')).toBeVisible();
  await page.getByText('Vật lý (HIGH_SCHOOL)').click();
  await page.getByRole('button', { name: 'Thêm môn học' }).click();
  await expect(page.getByText('Đã thêm môn học thành công.')).toBeVisible();
  expect(requests.some(request => request.method === 'GET' && request.path === '/api/public/subjects' && request.query.includes('keyword=V%E1%BA%ADt') && request.query.includes('size=100'))).toBe(true);
  expect(requests.some(request => request.method === 'POST' && request.path === '/api/teacher/subjects/physics-id')).toBe(true);
  await expect(page.getByText('Vật lý').first()).toBeVisible();

  await search.fill('Toán');
  await expect(page.getByText('Toán học (HIGH_SCHOOL)')).toBeVisible();
  await page.getByText('Toán học (HIGH_SCHOOL)').click();
  await page.getByRole('button', { name: 'Thêm môn học' }).click();
  await expect(page.getByText('Bạn đã đăng ký môn học này rồi.')).toBeVisible();
  expect(requests.filter(request => request.method === 'POST')).toHaveLength(1);
});

test('teacher removes an assigned subject only after confirmation', async ({ page }) => {
  const { requests } = await setup(page);
  await openSubjects(page);
  const row = page.getByRole('listitem').filter({ hasText: 'Toán học' });
  await row.getByRole('button', { name: 'Xóa môn Toán học' }).click();
  await expect(page.getByText('Bạn có chắc chắn muốn xóa môn này không?')).toBeVisible();
  await page.getByRole('button', { name: 'Đồng ý' }).click();
  await expect(page.getByText('Đã xóa môn học.')).toBeVisible();
  expect(requests.some(request => request.method === 'DELETE' && request.path === '/api/teacher/subjects/math-id')).toBe(true);
  await expect(page.getByText('Toán học', { exact: true })).toHaveCount(0);
});

test('teacher subjects keeps loading, empty, GET error and retry distinct', async ({ page }) => {
  const loading = await setup(page, { holdGet: true });
  await openSubjects(page);
  await expect(page.getByRole('status')).toContainText('Đang tải môn');
  loading.release();
  await expect(page.getByText('Toán học', { exact: true })).toBeVisible();

  await page.unrouteAll({ behavior: 'wait' });
  const retry = await setup(page, { failGet: true });
  await page.reload();
  await expect(page.getByText('Không thể tải danh sách môn học.')).toBeVisible();
  await retry.retry();
  await page.getByRole('button', { name: 'Thử tải lại' }).click();
  await expect(page.getByText('Toán học', { exact: true })).toBeVisible();

  await page.unrouteAll({ behavior: 'wait' });
  await setup(page, { empty: true });
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Chưa có môn học nào' })).toBeVisible();
});
