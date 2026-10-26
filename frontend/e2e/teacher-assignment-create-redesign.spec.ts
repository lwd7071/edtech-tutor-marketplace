import { expect, test, type Page } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const teacher = { id: 'teacher-assignment-create-fixture', email: 'teacher@example.test', fullName: 'Nguyễn Minh An', role: 'TEACHER', status: 'ACTIVE' };
const learners = [
  { studentId: '11111111-1111-4111-8111-111111111111', studentName: 'Nguyễn Minh Anh', subjectName: 'Toán học', studentPackage: { id: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', subjectId: '33333333-3333-4333-8333-333333333333', packageName: 'Luyện phương trình', remainingSessions: 4, expiresAt: '2026-12-31T16:59:59Z', status: 'ACTIVE' } },
  { studentId: '22222222-2222-4222-8222-222222222222', studentName: 'Trần Gia Bảo', subjectName: 'Vật lý', studentPackage: { id: 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', subjectId: '44444444-4444-4444-8444-444444444444', packageName: 'Điện học căn bản', remainingSessions: 0, expiresAt: '2026-12-31T16:59:59Z', status: 'COMPLETED' } },
  { studentId: '55555555-5555-4555-8555-555555555555', studentName: 'Lê Thu Hà', subjectName: 'Ngữ văn', studentPackage: { id: 'cccccccc-cccc-4ccc-8ccc-cccccccccccc', subjectId: '66666666-6666-4666-8666-666666666666', packageName: 'Gói tạm dừng', remainingSessions: 0, expiresAt: '2026-12-31T16:59:59Z', status: 'PAUSED' } },
];
const envelope = (data: unknown) => ({ success: true, message: 'ok', data, errors: null, meta: { page: 0, size: 100, totalElements: Array.isArray(data) ? data.length : 1, totalPages: 1 } });

async function openAssignmentForm(page: Page, options: { hold?: boolean; fail?: boolean; empty?: boolean } = {}) {
  await page.addInitScript(user => {
    localStorage.setItem('tutor-match.session', JSON.stringify({ user, remember: false, sessionId: 'teacher-assignment-create-session' }));
    document.cookie = 'accessToken=teacher-assignment-create-access; path=/; SameSite=Lax';
  }, teacher);
  let releaseGet: (() => void) | undefined;
  const gate = options.hold ? new Promise<void>(resolveGate => { releaseGet = resolveGate; }) : null;
  const requests: Array<{ method: string; path: string; params: Record<string, string>; body?: unknown }> = [];
  await page.route('**/api/teacher/students**', async route => {
    const request = route.request();
    const url = new URL(request.url());
    requests.push({ method: request.method(), path: url.pathname, params: Object.fromEntries(url.searchParams.entries()) });
    if (gate) await gate;
    if (options.fail) return route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ success: false, message: 'unavailable', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null }) });
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(options.empty ? [] : learners)) });
  });
  await page.route('**/api/teacher/assignments', async route => {
    const request = route.request();
    requests.push({ method: request.method(), path: new URL(request.url()).pathname, params: {}, body: request.postDataJSON() });
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope({ id: '99999999-9999-4999-8999-999999999999' })) });
  });
  await page.goto('/teacher/assignments/new');
  return { requests, releaseGet: () => releaseGet?.() };
}

test('assignment create hierarchy captures mobile, tablet and desktop', async ({ page }) => {
  const { requests } = await openAssignmentForm(page);
  await expect(page.getByRole('heading', { name: 'Giao bài tập' })).toBeVisible();
  await expect(page.getByRole('combobox', { name: 'Học viên và môn học' })).toBeVisible();
  expect(requests).toContainEqual(expect.objectContaining({ method: 'GET', path: '/api/teacher/students', params: { page: '0', size: '100' } }));
  if (variant !== 'before') {
    await page.getByRole('combobox', { name: 'Học viên và môn học' }).click();
    await expect(page.getByText('Nguyễn Minh Anh · Toán học · Luyện phương trình')).toBeVisible();
    await expect(page.getByText('Trần Gia Bảo · Vật lý · Điện học căn bản')).toBeVisible();
    await expect(page.getByText('Lê Thu Hà · Ngữ văn · Gói tạm dừng')).toHaveCount(0);
    await page.keyboard.press('Escape');
    await expect(page.locator('.ant-select-dropdown')).toBeHidden();
  }

  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.evaluate(() => {
      document.documentElement.style.scrollBehavior = 'auto';
      window.scrollTo({ top: 0, left: 0, behavior: 'instant' });
      if (document.scrollingElement) document.scrollingElement.scrollTop = 0;
    });
    await page.evaluate(() => new Promise<void>(resolveFrame => requestAnimationFrame(() => requestAnimationFrame(() => resolveFrame()))));
    const dimensions = await page.evaluate(() => ({
      viewport: document.documentElement.clientWidth,
      content: document.documentElement.scrollWidth,
      offenders: [...document.querySelectorAll<HTMLElement>('*')]
        .map(element => ({ element, rect: element.getBoundingClientRect() }))
        .filter(({ element, rect }) => rect.width > 0 && (rect.left < -1 || rect.right > document.documentElement.clientWidth + 1))
        .slice(0, 8)
        .map(({ element, rect }) => ({ tag: element.tagName, className: typeof element.className === 'string' ? element.className : '', left: Math.round(rect.left), right: Math.round(rect.right), width: Math.round(rect.width) })),
    }));
    if (variant !== 'before') expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidence, `teacher-assignment-create-${variant}-${width}.png`) });
    if (variant !== 'before' && width <= 1000) {
      await page.evaluate(() => window.scrollTo({ top: document.documentElement.scrollHeight, behavior: 'instant' }));
      const clearance = await page.evaluate(() => {
        const submit = document.querySelector('button[type="submit"]')?.getBoundingClientRect();
        const nav = [...document.querySelectorAll('nav')].map(node => ({ rect: node.getBoundingClientRect(), style: getComputedStyle(node) }))
          .find(item => item.style.position === 'fixed' && item.rect.bottom >= window.innerHeight - 1);
        return { submitBottom: submit?.bottom, navTop: nav?.rect.top };
      });
      expect(clearance.submitBottom).toBeLessThan(clearance.navTop! - 8);
    }
  }
  if (variant !== 'before') {
    await expect(page.getByRole('link', { name: 'Danh sách bài tập' })).toHaveAttribute('href', '/teacher/assignments');
    await page.getByRole('textbox', { name: 'Tên bài tập' }).focus();
    expect(await page.getByRole('textbox', { name: 'Tên bài tập' }).evaluate(element => element.matches(':focus-visible'))).toBe(true);
    await page.getByRole('button', { name: 'Lưu bản nháp' }).click();
    await expect(page.getByText('Vui lòng chọn học viên và môn học')).toBeVisible();
  }
});

test('draft and publish keep the existing request and Vietnam time conversion', async ({ page }) => {
  test.skip(variant === 'before');
  const { requests } = await openAssignmentForm(page);
  const recipient = page.getByRole('combobox', { name: 'Học viên và môn học' });
  await recipient.click();
  await page.getByText('Nguyễn Minh Anh · Toán học · Luyện phương trình', { exact: true }).click();
  await page.getByRole('textbox', { name: 'Tên bài tập' }).fill('Ôn phương trình bậc hai');
  await page.getByRole('textbox', { name: 'Đề bài và hướng dẫn' }).fill('Giải các bài tập trong buổi hôm nay.');
  await page.getByLabel('Hạn nộp (giờ Việt Nam)').fill('2026-10-21T12:30');
  await page.getByRole('button', { name: 'Lưu bản nháp' }).click();
  await expect(page.getByRole('heading', { name: 'Bài tập đã giao' })).toBeVisible();
  expect(requests.find(request => request.method === 'POST')?.body).toEqual({
    version: 0,
    studentId: learners[0].studentId,
    subjectId: learners[0].studentPackage.subjectId,
    title: 'Ôn phương trình bậc hai',
    assignmentType: 'FREEFORM',
    status: 'DRAFT',
    dueAt: '2026-10-21T05:30:00.000Z',
    contentBlocks: [{ type: 'TEXT', content: 'Giải các bài tập trong buổi hôm nay.' }],
  });

  await page.goto('/teacher/assignments/new');
  await page.getByRole('combobox', { name: 'Học viên và môn học' }).click();
  await page.getByText('Trần Gia Bảo · Vật lý · Điện học căn bản', { exact: true }).click();
  await page.getByRole('textbox', { name: 'Tên bài tập' }).fill('Bài luyện điện học');
  await page.getByRole('textbox', { name: 'Đề bài và hướng dẫn' }).fill('Vẽ sơ đồ mạch và giải thích.');
  await page.getByLabel('Hạn nộp (giờ Việt Nam)').fill('2026-10-22T09:00');
  await page.getByRole('combobox', { name: 'Trạng thái' }).click();
  await page.getByText('Phát hành ngay', { exact: true }).click();
  await page.getByRole('button', { name: 'Phát hành bài tập' }).click();
  await expect(page.getByRole('heading', { name: 'Bài tập đã giao' })).toBeVisible();
  expect(requests.filter(request => request.method === 'POST')[1]?.body).toEqual(expect.objectContaining({
    studentId: learners[1].studentId,
    subjectId: learners[1].studentPackage.subjectId,
    title: 'Bài luyện điện học',
    status: 'PUBLISHED',
    dueAt: '2026-10-22T02:00:00.000Z',
  }));
});

test('learner loading, error/retry and no-eligible-package states are distinct', async ({ page }) => {
  test.skip(variant === 'before');
  const held = await openAssignmentForm(page, { hold: true });
  await expect(page.getByRole('status', { name: 'Đang tải học viên và gói học' })).toBeVisible();
  held.releaseGet();
  await expect(page.getByRole('combobox', { name: 'Học viên và môn học' })).toBeVisible();
  await page.unrouteAll({ behavior: 'wait' });
  await openAssignmentForm(page, { fail: true });
  await expect(page.getByText('Chưa tải được danh sách học viên')).toBeVisible();
  await page.unrouteAll({ behavior: 'wait' });
  await page.route('**/api/teacher/students**', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope([])) }));
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByRole('heading', { name: 'Chưa có học viên hoặc gói học phù hợp' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Danh sách học viên' })).toHaveAttribute('href', '/teacher/students');
});

test('failed create keeps entered values and shows the existing error feedback', async ({ page }) => {
  test.skip(variant === 'before');
  await openAssignmentForm(page);
  await page.unrouteAll({ behavior: 'wait' });
  await page.route('**/api/teacher/students**', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(learners)) }));
  await page.route('**/api/teacher/assignments', route => route.fulfill({ status: 409, contentType: 'application/json', body: JSON.stringify({ success: false, message: 'conflict', data: null, errors: [{ code: 'CONFLICT' }], meta: null }) }));
  await page.reload();
  await page.getByRole('combobox', { name: 'Học viên và môn học' }).click();
  await page.getByText('Nguyễn Minh Anh · Toán học · Luyện phương trình', { exact: true }).click();
  await page.getByRole('textbox', { name: 'Tên bài tập' }).fill('Bài tập cần giữ lại');
  await page.getByRole('textbox', { name: 'Đề bài và hướng dẫn' }).fill('Nội dung không được mất khi lỗi.');
  await page.getByLabel('Hạn nộp (giờ Việt Nam)').fill('2026-10-21T12:30');
  await page.getByRole('button', { name: 'Lưu bản nháp' }).click();
  await expect(page.getByText('Chưa giao được bài. Kiểm tra thông tin và quan hệ học viên.')).toBeVisible();
  await expect(page.getByRole('textbox', { name: 'Tên bài tập' })).toHaveValue('Bài tập cần giữ lại');
  await expect(page.getByRole('textbox', { name: 'Đề bài và hướng dẫn' })).toHaveValue('Nội dung không được mất khi lỗi.');
});
