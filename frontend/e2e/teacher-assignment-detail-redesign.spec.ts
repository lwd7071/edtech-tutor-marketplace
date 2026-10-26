import { expect, test, type Page } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const assignmentId = '99999999-9999-4999-8999-999999999999';
const submissionId = '88888888-8888-4888-8888-888888888888';
const teacher = { id: 'teacher-assignment-detail-fixture', email: 'teacher@example.test', fullName: 'Nguyễn Minh An', role: 'TEACHER', status: 'ACTIVE' };
const initialAssignment = {
  id: assignmentId,
  teacherId: '77777777-7777-4777-8777-777777777777',
  studentId: '11111111-1111-4111-8111-111111111111',
  subjectId: '22222222-2222-4222-8222-222222222222',
  title: 'Ôn tập phương trình bậc hai',
  assignmentType: 'FREEFORM',
  contentBlocks: [
    { type: 'TEXT', content: 'Giải các câu 1 đến 3 trong tài liệu.' },
    { type: 'FILE', attachmentId: '33333333-3333-4333-8333-333333333333' },
    { type: 'TEXT', content: 'Trình bày từng bước giải.' },
  ],
  dueAt: '2026-10-21T05:30:00Z',
  status: 'PUBLISHED',
  version: 7,
  studentName: 'Nguyễn Minh Anh',
  subjectName: 'Toán học',
  submittedCount: 1,
  totalStudents: 1,
  submissions: [{
    id: submissionId,
    assignmentId,
    studentId: '11111111-1111-4111-8111-111111111111',
    contentBlocks: [{ type: 'TEXT', content: 'Em đã hoàn thành bài tập.' }],
    submittedAt: '2026-10-20T14:00:00Z',
    status: 'SUBMITTED',
    score: null,
    feedbackText: null,
    gradedAt: null,
    version: 2,
  }],
};
const envelope = (data: unknown) => ({ success: true, message: 'ok', data, errors: null, meta: null });

async function openAssignmentDetail(page: Page, options: { hold?: boolean; fail?: boolean; empty?: boolean; status?: 'DRAFT' | 'PUBLISHED' | 'CLOSED'; submissionStatus?: 'DRAFT' | 'SUBMITTED' | 'GRADED'; conflict?: boolean } = {}) {
  await page.addInitScript(user => {
    localStorage.setItem('tutor-match.session', JSON.stringify({ user, remember: false, sessionId: 'teacher-assignment-detail-session' }));
    document.cookie = 'accessToken=teacher-assignment-detail-access; path=/; SameSite=Lax';
  }, teacher);
  let releaseGet: (() => void) | undefined;
  const gate = options.hold ? new Promise<void>(resolveGate => { releaseGet = resolveGate; }) : null;
  let current = {
    ...initialAssignment,
    status: options.status ?? 'PUBLISHED',
    submissions: options.empty ? [] : [{
      ...initialAssignment.submissions[0],
      status: options.submissionStatus ?? initialAssignment.submissions[0].status,
      submittedAt: options.submissionStatus === 'DRAFT' ? null : initialAssignment.submissions[0].submittedAt,
      score: options.submissionStatus === 'GRADED' ? 9 : null,
    }],
  };
  let getCount = 0;
  const requests: Array<{ method: string; path: string; body?: unknown }> = [];
  await page.route(`**/api/teacher/assignments/${assignmentId}**`, async route => {
    const request = route.request();
    requests.push({ method: request.method(), path: new URL(request.url()).pathname, body: request.method() === 'GET' ? undefined : request.postDataJSON() });
    if (request.method() === 'GET') {
      getCount += 1;
      if (gate) await gate;
      if (options.fail) return route.fulfill({ status: 404, contentType: 'application/json', body: JSON.stringify({ success: false, message: 'not found', data: null, errors: [{ code: 'NOT_FOUND' }], meta: null }) });
      if (options.conflict && getCount > 1) current = { ...current, title: 'Tiêu đề đã cập nhật từ nơi khác', version: 8 };
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(current)) });
    }

    if (options.conflict) {
      current = { ...current, title: 'Tiêu đề đã cập nhật từ nơi khác', version: 8 };
      return route.fulfill({ status: 409, contentType: 'application/json', body: JSON.stringify({ success: false, message: 'conflict', data: null, errors: [{ code: 'CONCURRENT_MODIFICATION' }], meta: null }) });
    }
    if (request.method() === 'PUT') {
      const body = request.postDataJSON();
      current = { ...current, ...body, version: current.version + 1 };
    } else if (request.url().endsWith('/publish')) {
      current = { ...current, status: 'PUBLISHED', version: current.version + 1 };
    } else if (request.url().endsWith('/close')) {
      current = { ...current, status: 'CLOSED', version: current.version + 1 };
    }
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(current)) });
  });
  await page.goto(`/teacher/assignments/${assignmentId}`);
  return { requests, releaseGet: () => releaseGet?.() };
}

test('teacher assignment detail captures mobile, tablet and desktop', async ({ page }) => {
  const { requests } = await openAssignmentDetail(page);
  await expect(page.getByRole('heading', { name: 'Ôn tập phương trình bậc hai' })).toBeVisible();
  expect(requests).toContainEqual(expect.objectContaining({ method: 'GET', path: `/api/teacher/assignments/${assignmentId}` }));
  if (variant !== 'before') {
    await expect(page.getByLabel('Thông tin bài tập')).toContainText('Nguyễn Minh Anh');
    await expect(page.getByLabel('Thông tin bài tập')).toContainText('Toán học');
    await expect(page.getByRole('link', { name: 'Xem và chấm bài' })).toHaveAttribute('href', `/teacher/submissions/${submissionId}`);
    await expect(page.getByRole('button', { name: 'Đóng nhận bài' })).toBeVisible();
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
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    if (variant !== 'before') expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidence, `teacher-assignment-detail-${variant}-${width}.png`) });
    if (variant !== 'before' && width <= 1000) {
      const action = await page.getByRole('button', { name: 'Đóng nhận bài' }).boundingBox();
      const navTop = await page.evaluate(() => [...document.querySelectorAll('nav')]
        .map(node => ({ rect: node.getBoundingClientRect(), style: getComputedStyle(node) }))
        .find(item => item.style.position === 'fixed' && item.rect.bottom >= window.innerHeight - 1));
      expect(action!.y + action!.height).toBeLessThanOrEqual(navTop!.rect.top);
    }
  }
  if (variant !== 'before') {
    await expect(page.getByRole('link', { name: 'Danh sách bài tập' })).toHaveAttribute('href', '/teacher/assignments');
    await page.getByRole('button', { name: 'Đóng nhận bài' }).focus();
    expect(await page.getByRole('button', { name: 'Đóng nhận bài' }).evaluate(element => element.matches(':focus-visible'))).toBe(true);
  }
});

test('draft edit keeps version, timezone and non-text content blocks', async ({ page }) => {
  test.skip(variant === 'before');
  const { requests } = await openAssignmentDetail(page, { status: 'DRAFT' });
  await page.getByRole('textbox', { name: 'Tên bài tập' }).fill('Ôn tập phương trình bậc hai – bản sửa');
  await page.getByRole('textbox', { name: 'Đề bài' }).fill('Giải câu 4 và 5.');
  await page.getByLabel('Hạn nộp').fill('2026-10-22T12:00');
  await page.getByRole('button', { name: 'Lưu chỉnh sửa' }).click();
  await expect(page.getByRole('textbox', { name: 'Tên bài tập' })).toHaveValue('Ôn tập phương trình bậc hai – bản sửa');
  expect(requests.find(request => request.method === 'PUT')).toMatchObject({
    path: `/api/teacher/assignments/${assignmentId}`,
    body: {
      studentId: initialAssignment.studentId,
      subjectId: initialAssignment.subjectId,
      title: 'Ôn tập phương trình bậc hai – bản sửa',
      assignmentType: 'FREEFORM',
      status: 'DRAFT',
      dueAt: '2026-10-22T05:00:00.000Z',
      contentBlocks: [
        { type: 'TEXT', content: 'Giải câu 4 và 5.' },
        { type: 'FILE', attachmentId: '33333333-3333-4333-8333-333333333333' },
      ],
      version: 7,
    },
  });
});

test('draft publish and published close send the current version', async ({ page }) => {
  test.skip(variant === 'before');
  const { requests } = await openAssignmentDetail(page, { status: 'DRAFT', empty: true });
  await page.getByRole('button', { name: 'Phát hành bản đã lưu' }).click();
  await expect(page.getByRole('button', { name: 'Đóng nhận bài' })).toBeVisible();
  expect(requests.find(request => request.path.endsWith('/publish'))?.body).toEqual({ version: 7 });
  await page.getByRole('button', { name: 'Đóng nhận bài' }).click();
  await expect(page.getByText('Đã đóng')).toBeVisible();
  expect(requests.find(request => request.path.endsWith('/close'))?.body).toEqual({ version: 8 });
});

test('loading, not-found retry, submissions empty and closed read-only states', async ({ page }) => {
  test.skip(variant === 'before');
  const held = await openAssignmentDetail(page, { status: 'CLOSED', empty: true, hold: true });
  await expect(page.getByRole('status', { name: 'Đang tải bài tập' })).toBeVisible();
  held.releaseGet();
  await expect(page.getByText('Học viên chưa nộp bài.')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Lưu chỉnh sửa' })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Phát hành bản đã lưu' })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Đóng nhận bài' })).toHaveCount(0);
  await page.unrouteAll({ behavior: 'wait' });
  await openAssignmentDetail(page, { fail: true });
  await expect(page.getByText('Chưa đọc được bài tập')).toBeVisible();
  await page.unrouteAll({ behavior: 'wait' });
  await page.route(`**/api/teacher/assignments/${assignmentId}`, route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(initialAssignment)) }));
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByRole('heading', { name: initialAssignment.title })).toBeVisible();
});

test('submission draft and graded states show the matching label and score', async ({ page }) => {
  test.skip(variant === 'before');
  await openAssignmentDetail(page, { status: 'PUBLISHED', submissionStatus: 'GRADED' });
  await expect(page.getByText('Đã chấm')).toBeVisible();
  await expect(page.getByText('9/10')).toBeVisible();
  await page.unrouteAll({ behavior: 'wait' });
  await openAssignmentDetail(page, { status: 'PUBLISHED', submissionStatus: 'DRAFT' });
  await expect(page.getByText('Bản nháp')).toBeVisible();
  await expect(page.getByText('Chưa gửi bài')).toBeVisible();
});

test('stale draft version reports conflict and reloads server version', async ({ page }) => {
  test.skip(variant === 'before');
  await openAssignmentDetail(page, { status: 'DRAFT', conflict: true });
  await page.getByRole('textbox', { name: 'Tên bài tập' }).fill('Bản sửa cũ');
  await page.getByRole('button', { name: 'Lưu chỉnh sửa' }).click();
  await expect(page.getByText('Dữ liệu đã được thay đổi bởi người khác. Vui lòng tải lại.')).toBeVisible();
  await expect(page.getByRole('textbox', { name: 'Tên bài tập' })).toHaveValue('Tiêu đề đã cập nhật từ nơi khác');
});
