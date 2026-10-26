import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const student = { id: 'student-assignment-visual', email: 'assignment@example.test', fullName: 'Nguyễn Minh An', role: 'STUDENT', status: 'ACTIVE' };
const assignments = [
  { id: 'assignment-1', teacherId: 'teacher-1', studentId: student.id, subjectId: 'subject-1', title: 'Phương trình bậc hai', assignmentType: 'FREEFORM', contentBlocks: [], dueAt: '2026-10-15T10:00:00Z', status: 'PUBLISHED', version: 1 },
  { id: 'assignment-2', teacherId: 'teacher-2', studentId: student.id, subjectId: 'subject-2', title: 'Ôn tập hình học', assignmentType: 'FREEFORM', contentBlocks: [], dueAt: '2026-10-22T10:00:00Z', status: 'CLOSED', version: 2 },
];
const envelope = (data: unknown, meta: unknown = null) => ({ success: true, message: 'ok', data, errors: null, meta });

test('student assignments redesign preserves progress filters, search and pagination', async ({ page }) => {
  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  let responseStatus = 200;
  let empty = false;
  let delay = 0;
  const requests: URL[] = [];
  await page.route('**/api/auth/login', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope({ accessToken: 'visual-access', refreshToken: 'visual-refresh', user: student })) }));
  await page.route('**/api/student/assignments**', async route => {
    const url = new URL(route.request().url());
    requests.push(url);
    if (delay) await new Promise(resolveDelay => setTimeout(resolveDelay, delay));
    await route.fulfill({ status: responseStatus, contentType: 'application/json', body: JSON.stringify(responseStatus === 200
      ? envelope(empty ? [] : assignments, { page: Number(url.searchParams.get('page') || 0), size: Number(url.searchParams.get('size') || 10), totalElements: empty ? 0 : 13, totalPages: empty ? 0 : 2 })
      : { success: false, message: 'unavailable', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null }) });
  });

  await page.goto('/auth/login');
  await page.getByPlaceholder('Nhập email của bạn').fill(student.email);
  await page.getByPlaceholder('Nhập mật khẩu').fill('Password123!');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page).toHaveURL(/\/student$/);
  await page.goto('/student/assignments');
  await expect(page.getByRole('heading', { name: 'Bài tập của tôi' })).toBeVisible();
  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    if (variant === 'after') {
      await expect(page.getByRole('link', { name: 'Phương trình bậc hai' })).toHaveAttribute('href', '/student/assignments/assignment-1');
      await expect(page.getByText('Quá hạn').first()).toBeVisible();
      await expect(page.getByText('Chưa nộp').first()).toBeVisible();
      await expect(page.getByText('Đã đóng')).toBeVisible();
      const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
      expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    }
    await page.screenshot({ path: resolve(evidence, `student-assignments-${variant}-${width}.png`), fullPage: true });
  }
  if (variant === 'before') return;

  const todo = page.getByRole('tab', { name: 'Chưa làm' });
  await todo.focus();
  await expect(todo).toBeFocused();
  await page.getByRole('tab', { name: 'Đã nộp' }).click();
  await expect.poll(() => requests.at(-1)?.searchParams.get('progress')).toBe('SUBMITTED');
  await page.getByRole('tab', { name: 'Đã chấm điểm' }).click();
  await expect.poll(() => requests.at(-1)?.searchParams.get('progress')).toBe('GRADED');
  await page.getByRole('tab', { name: 'Chưa làm' }).click();
  await expect.poll(() => requests.at(-1)?.searchParams.get('progress')).toBe('TODO');

  await page.getByRole('textbox', { name: 'Tìm theo tiêu đề' }).fill('không tồn tại');
  await expect(page.getByRole('heading', { name: 'Không tìm thấy bài tập trên trang này' })).toBeVisible();
  await page.getByRole('textbox', { name: 'Tìm theo tiêu đề' }).fill('');

  await page.getByTitle('Next Page').click();
  await expect.poll(() => requests.at(-1)?.searchParams.get('page')).toBe('1');
  await expect.poll(() => requests.at(-1)?.searchParams.get('size')).toBe('10');
  await page.getByText('10 / page').click();
  await page.getByText('20 / page', { exact: true }).click();
  await expect.poll(() => requests.at(-1)?.searchParams.get('size')).toBe('20');
  await page.getByRole('tab', { name: 'Đã chấm điểm' }).click();
  await expect.poll(() => requests.at(-1)?.searchParams.get('progress')).toBe('GRADED');
  await expect.poll(() => requests.at(-1)?.searchParams.get('page')).toBe('0');

  await page.setViewportSize({ width: 375, height: 900 });
  const lastOpenLink = page.getByRole('link', { name: 'Mở bài tập' }).last();
  await lastOpenLink.scrollIntoViewIfNeeded();
  const clearance = await page.evaluate(() => ({
    linkBottom: document.querySelector('.tm-student-assignment-open:last-of-type')?.getBoundingClientRect().bottom ?? 0,
    navTop: document.querySelector('.tm-workspace-bottom')?.getBoundingClientRect().top ?? Number.POSITIVE_INFINITY,
  }));
  expect(clearance.linkBottom).toBeLessThanOrEqual(clearance.navTop);

  empty = true;
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Chưa có bài tập ở mục này' })).toBeVisible();
  empty = false;
  responseStatus = 503;
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Chưa tải được bài tập' })).toBeVisible();
  responseStatus = 200;
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByRole('link', { name: 'Phương trình bậc hai' })).toBeVisible();

  delay = 1000;
  const pendingReload = page.reload({ waitUntil: 'commit' });
  await expect(page.getByRole('status', { name: 'Đang tải bài tập' })).toBeVisible();
  await pendingReload;
  await expect(page.getByRole('heading', { name: 'Bài tập của tôi' })).toBeVisible();
});
