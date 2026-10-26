import { expect, test, type Page } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const submissionId = '88888888-8888-4888-8888-888888888888';
const assignmentId = '99999999-9999-4999-8999-999999999999';
const teacher = { id: 'teacher-submission-fixture', email: 'teacher@example.test', fullName: 'Nguyễn Minh An', role: 'TEACHER', status: 'ACTIVE' };
const initialSubmission = {
  id: submissionId,
  assignmentId,
  studentId: '11111111-1111-4111-8111-111111111111',
  contentBlocks: [
    { type: 'TEXT', content: 'Em đã giải câu 1 và câu 2. Bước biến đổi ở câu 2 là...' },
    { type: 'FILE', attachmentId: '22222222-2222-4222-8222-222222222222' },
  ],
  submittedAt: '2026-10-20T14:00:00Z',
  status: 'SUBMITTED',
  score: null,
  feedbackText: null,
  gradedAt: null,
  version: 4,
};
const envelope = (data: unknown) => ({ success: true, message: 'ok', data, errors: null, meta: null });

async function openSubmission(page: Page, options: { hold?: boolean; fail?: boolean; gradeFailure?: boolean; status?: 'DRAFT' | 'SUBMITTED' | 'GRADED'; conflict?: boolean } = {}) {
  await page.addInitScript(user => {
    localStorage.setItem('tutor-match.session', JSON.stringify({ user, remember: false, sessionId: 'teacher-submission-session' }));
    document.cookie = 'accessToken=teacher-submission-access; path=/; SameSite=Lax';
  }, teacher);
  let releaseGet: (() => void) | undefined;
  const gate = options.hold ? new Promise<void>(resolveGate => { releaseGet = resolveGate; }) : null;
  let current = {
    ...initialSubmission,
    status: options.status ?? 'SUBMITTED',
    score: options.status === 'GRADED' ? 8.5 : null,
    feedbackText: options.status === 'GRADED' ? 'Lập luận tốt, cần ghi rõ đơn vị.' : null,
    gradedAt: options.status === 'GRADED' ? '2026-10-21T02:00:00Z' : null,
  };
  let getCount = 0;
  const requests: Array<{ method: string; path: string; body?: unknown }> = [];
  await page.route(`**/api/teacher/submissions/${submissionId}**`, async route => {
    const request = route.request();
    requests.push({ method: request.method(), path: new URL(request.url()).pathname, body: request.method() === 'GET' ? undefined : request.postDataJSON() });
    if (request.method() === 'GET') {
      getCount += 1;
      if (gate) await gate;
      if (options.fail) return route.fulfill({ status: 404, contentType: 'application/json', body: JSON.stringify({ success: false, message: 'not found', data: null, errors: [{ code: 'NOT_FOUND' }], meta: null }) });
      if (options.conflict && getCount > 1) current = { ...current, score: 9.5, feedbackText: 'Điểm đã được cập nhật ở nơi khác.', version: 5 };
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(current)) });
    }
    if (options.conflict) {
      current = { ...current, score: 9.5, feedbackText: 'Điểm đã được cập nhật ở nơi khác.', version: 5 };
      return route.fulfill({ status: 409, contentType: 'application/json', body: JSON.stringify({ success: false, message: 'conflict', data: null, errors: [{ code: 'CONCURRENT_MODIFICATION' }], meta: null }) });
    }
    if (options.gradeFailure) {
      return route.fulfill({ status: 500, contentType: 'application/json', body: JSON.stringify({ success: false, message: 'failed', data: null, errors: [{ code: 'SYSTEM_ERROR', message: 'Không thể lưu nhận xét.' }], meta: null }) });
    }
    const body = request.postDataJSON();
    current = { ...current, ...body, status: 'GRADED', gradedAt: '2026-10-21T02:00:00Z', version: current.version + 1 };
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(current)) });
  });
  await page.goto(`/teacher/submissions/${submissionId}`);
  return { requests, releaseGet: () => releaseGet?.() };
}

test('submission review captures mobile, tablet and desktop', async ({ page }) => {
  const { requests } = await openSubmission(page);
  await expect(page.getByRole('heading', { name: variant === 'before' ? 'Bài làm của học viên' : 'Bài nộp' })).toBeVisible();
  expect(requests).toContainEqual(expect.objectContaining({ method: 'GET', path: `/api/teacher/submissions/${submissionId}` }));
  if (variant !== 'before') {
    await expect(page.locator('main').getByText('Nguyễn Minh An')).toHaveCount(0);
    await expect(page.getByRole('link', { name: 'Bài tập liên quan' })).toHaveAttribute('href', `/teacher/assignments/${assignmentId}`);
    await expect(page.getByText('Mã học viên')).toBeVisible();
    await expect(page.getByText('Em đã giải câu 1 và câu 2. Bước biến đổi ở câu 2 là...')).toBeVisible();
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
    await page.screenshot({ path: resolve(evidence, `teacher-submission-${variant}-${width}.png`) });
  }
  if (variant !== 'before') {
    await page.getByRole('spinbutton', { name: 'Điểm /10' }).focus();
    expect(await page.getByRole('spinbutton', { name: 'Điểm /10' }).evaluate(element => element.matches(':focus-visible'))).toBe(true);
    for (const width of [375, 768]) {
      await page.setViewportSize({ width, height: 900 });
      const saveButton = page.getByRole('button', { name: 'Lưu kết quả chấm' });
      await saveButton.scrollIntoViewIfNeeded();
      const buttonBottom = (await saveButton.boundingBox())!.y + (await saveButton.boundingBox())!.height;
      const navTop = (await page.locator('.tm-workspace-bottom').boundingBox())!.y;
      expect(buttonBottom).toBeLessThanOrEqual(navTop);
    }
  }
});

test('submitted work saves the exact score, feedback and version then becomes read-only', async ({ page }) => {
  test.skip(variant === 'before');
  const { requests } = await openSubmission(page);
  await page.getByRole('spinbutton', { name: 'Điểm /10' }).fill('7.5');
  await page.getByRole('textbox', { name: 'Nhận xét' }).fill('Cần ghi rõ đơn vị ở câu 2.');
  await page.getByRole('button', { name: 'Lưu kết quả chấm' }).click();
  await expect(page.getByRole('button', { name: 'Lưu kết quả chấm' })).toHaveCount(0);
  await expect(page.getByText('7.5/10')).toBeVisible();
  expect(requests.find(request => request.method === 'POST')).toEqual({
    method: 'POST',
    path: `/api/teacher/submissions/${submissionId}/grade`,
    body: { score: 7.5, feedbackText: 'Cần ghi rõ đơn vị ở câu 2.', version: 4 },
  });
});

test('DRAFT and GRADED submissions never expose a grading action', async ({ page }) => {
  test.skip(variant === 'before');
  await openSubmission(page, { status: 'DRAFT' });
  await expect(page.getByText('Bản nháp', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Lưu kết quả chấm' })).toHaveCount(0);
  await page.unrouteAll({ behavior: 'wait' });
  await openSubmission(page, { status: 'GRADED' });
  await expect(page.getByText('Đã chấm', { exact: true })).toBeVisible();
  await expect(page.getByText('8.5/10')).toBeVisible();
  await expect(page.getByText('Lập luận tốt, cần ghi rõ đơn vị.')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Lưu kết quả chấm' })).toHaveCount(0);
});

test('loading and not-found error/retry states remain distinct', async ({ page }) => {
  test.skip(variant === 'before');
  const held = await openSubmission(page, { hold: true });
  await expect(page.getByRole('status', { name: 'Đang tải bài nộp' })).toBeVisible();
  held.releaseGet();
  await expect(page.getByRole('heading', { name: 'Bài nộp' })).toBeVisible();
  await page.unrouteAll({ behavior: 'wait' });
  await openSubmission(page, { fail: true });
  await expect(page.getByText('Chưa đọc được bài nộp')).toBeVisible();
  await page.unrouteAll({ behavior: 'wait' });
  await page.route(`**/api/teacher/submissions/${submissionId}**`, route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(initialSubmission)) }));
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByRole('heading', { name: 'Bài nộp' })).toBeVisible();
});

test('stale grading version reports conflict and reloads current result', async ({ page }) => {
  test.skip(variant === 'before');
  await openSubmission(page, { conflict: true });
  await page.getByRole('spinbutton', { name: 'Điểm /10' }).fill('7');
  await page.getByRole('button', { name: 'Lưu kết quả chấm' }).click();
  await expect(page.getByText('Dữ liệu đã được thay đổi bởi người khác. Vui lòng tải lại.')).toBeVisible();
  await expect(page.getByRole('spinbutton', { name: 'Điểm /10' })).toHaveValue('9.50');
  await expect(page.getByRole('textbox', { name: 'Nhận xét' })).toHaveValue('Điểm đã được cập nhật ở nơi khác.');
});

test('grading server error keeps entered values and reports the API message', async ({ page }) => {
  test.skip(variant === 'before');
  await openSubmission(page, { gradeFailure: true });
  await page.getByRole('spinbutton', { name: 'Điểm /10' }).fill('6.75');
  await page.getByRole('textbox', { name: 'Nhận xét' }).fill('Bổ sung bước giải ở câu 3.');
  await page.getByRole('button', { name: 'Lưu kết quả chấm' }).click();
  await expect(page.getByText('Không thể lưu nhận xét.')).toBeVisible();
  await expect(page.getByRole('spinbutton', { name: 'Điểm /10' })).toHaveValue('6.75');
  await expect(page.getByRole('textbox', { name: 'Nhận xét' })).toHaveValue('Bổ sung bước giải ở câu 3.');
});
