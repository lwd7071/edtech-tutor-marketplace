import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const student = { id: 'student-assignment-detail-visual', email: 'assignment.detail@example.test', fullName: 'Nguyễn Minh An', role: 'STUDENT', status: 'ACTIVE' };
const attachment = { id: 'attachment-solution', secureUrl: 'https://files.example.test/solution.pdf', originalFilename: 'loi-giai.pdf', mimeType: 'application/pdf', fileSize: 2048 };
const baseAssignment: Record<string, any> = {
  id: 'assignment-detail-visual', teacherId: 'teacher-1', studentId: student.id, subjectId: 'subject-1', title: 'Đạo hàm và ứng dụng', assignmentType: 'FREEFORM',
  contentBlocks: [{ id: 'block-1', type: 'TEXT', content: 'Tính đạo hàm của hàm số sau.\nTrình bày đầy đủ các bước giải.' }],
  dueAt: '2026-11-22T10:00:00Z', status: 'PUBLISHED', version: 3,
  assignmentAttachments: [attachment], submissionAttachments: [], submission: null,
};
const envelope = (data: unknown) => ({ success: true, message: 'ok', data, errors: null, meta: null });

test('student assignment detail redesign preserves prompt, attachments, draft/submit and state rules', async ({ page }) => {
  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  let assignment: Record<string, any> = { ...baseAssignment };
  let detailStatus = 200;
  let submitStatus = 201;
  let delay = 0;
  const submissions: Array<Record<string, any>> = [];
  const uploadedFiles: string[] = [];
  const uploadTypes: string[] = [];
  await page.route('**/api/auth/login', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope({ accessToken: 'visual-access', refreshToken: 'visual-refresh', user: student })) }));
  await page.route('**/api/attachments', async route => {
    uploadedFiles.push(attachment.id);
    uploadTypes.push(route.request().postData() || '');
    await route.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify(envelope(attachment)) });
  });
  await page.route('**/api/student/assignments/assignment-detail-visual/submissions', async route => {
    const body = route.request().postDataJSON();
    submissions.push(body);
    if (submitStatus !== 201) {
      await route.fulfill({ status: submitStatus, contentType: 'application/json', body: JSON.stringify({ success: false, message: 'conflict', data: null, errors: [{ code: 'CONCURRENT_MODIFICATION', message: 'conflict' }], meta: null }) });
      return;
    }
    assignment = { ...assignment, submission: { id: 'submission-1', assignmentId: assignment.id, studentId: student.id, contentBlocks: body.contentBlocks, submittedAt: body.status === 'SUBMITTED' ? '2026-10-16T05:00:00Z' : null, status: body.status, score: null, feedbackText: null, gradedAt: null, version: assignment.submission?.version == null ? 1 : assignment.submission.version + 1 }, submissionAttachments: body.contentBlocks.filter((block: any) => block.attachmentId).map(() => attachment) };
    await route.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify(envelope(assignment.submission)) });
  });
  await page.route('**/api/student/assignments/assignment-detail-visual', async route => {
    if (delay) await new Promise(resolveDelay => setTimeout(resolveDelay, delay));
    await route.fulfill({ status: detailStatus, contentType: 'application/json', body: JSON.stringify(detailStatus === 200 ? envelope(assignment) : { success: false, message: 'unavailable', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null }) });
  });

  await page.goto('/auth/login');
  await page.getByPlaceholder('Nhập email của bạn').fill(student.email);
  await page.getByPlaceholder('Nhập mật khẩu').fill('Password123!');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page).toHaveURL(/\/student$/);
  await page.goto('/student/assignments/assignment-detail-visual');
  await expect(page.getByRole('heading', { name: 'Đạo hàm và ứng dụng' })).toBeVisible();

  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    if (variant === 'after') {
      await expect(page.getByRole('heading', { name: 'Đề bài' })).toBeVisible();
      await expect(page.getByText('Tính đạo hàm của hàm số sau.')).toBeVisible();
      await expect(page.getByText('loi-giai.pdf')).toBeVisible();
      await expect(page.getByRole('button', { name: 'Lưu bản nháp' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Nộp bài' })).toBeVisible();
      const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
      expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    }
    await page.screenshot({ path: resolve(evidence, `student-assignment-detail-${variant}-${width}.png`), fullPage: true });
  }
  if (variant === 'before') return;

  for (const width of [375, 768]) {
    await page.setViewportSize({ width, height: 900 });
    await page.getByRole('button', { name: 'Nộp bài' }).scrollIntoViewIfNeeded();
    const clearance = await page.evaluate(() => ({
      submitBottom: document.querySelector('.tm-assignment-actions .is-primary')?.getBoundingClientRect().bottom ?? 0,
      navTop: document.querySelector('.tm-workspace-bottom')?.getBoundingClientRect().top ?? Number.POSITIVE_INFINITY,
    }));
    expect(clearance.submitBottom).toBeLessThanOrEqual(clearance.navTop);
  }

  await expect(page.getByRole('link', { name: 'Bài tập của tôi' })).toHaveAttribute('href', '/student/assignments');
  const answer = page.getByRole('textbox', { name: 'Câu trả lời' });
  await answer.fill('Lời giải nháp của em');
  await page.locator('input[type="file"]').setInputFiles({ name: 'loi-giai.pdf', mimeType: 'application/pdf', buffer: Buffer.from('fixture') });
  await expect.poll(() => uploadedFiles.length).toBe(1);
  expect(uploadTypes[0]).toContain('SUBMISSION');
  await page.getByRole('button', { name: 'Lưu bản nháp' }).click();
  await expect(page.getByText('Đã lưu bản nháp', { exact: true })).toBeVisible();
  expect(submissions[0]).toEqual({ version: 0, status: 'DRAFT', contentBlocks: [{ type: 'TEXT', content: 'Lời giải nháp của em' }, { type: 'FILE', attachmentId: attachment.id }] });
  await expect(page.getByRole('heading', { name: 'Bản nháp' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Tải xuống loi-giai.pdf' })).toHaveCount(2);

  await answer.fill('Lời giải đã hoàn chỉnh');
  await page.getByRole('button', { name: 'Lưu bản nháp' }).focus();
  await page.keyboard.press('Tab');
  await expect(page.getByRole('button', { name: 'Nộp bài' })).toBeFocused();
  await page.getByRole('button', { name: 'Nộp bài' }).click();
  await expect(page.getByText('Đã nộp bài', { exact: true })).toBeVisible();
  expect(submissions[1]).toEqual({ version: 1, status: 'SUBMITTED', contentBlocks: [{ type: 'TEXT', content: 'Lời giải đã hoàn chỉnh' }, { type: 'FILE', attachmentId: attachment.id }] });
  await expect(page.locator('.tm-assignment-submission-status')).toHaveText('Đang chờ gia sư chấm');

  submitStatus = 409;
  await answer.fill('Bài sửa giữ lại khi xung đột');
  await page.getByRole('button', { name: 'Lưu bản nháp' }).click();
  await expect(page.getByText('Bài làm đã được thay đổi. Vui lòng tải lại.')).toBeVisible();
  await expect(answer).toHaveValue('Bài sửa giữ lại khi xung đột');

  submitStatus = 201;
  assignment = { ...baseAssignment, dueAt: '2026-10-15T10:00:00Z', submission: assignment.submission };
  await page.reload();
  await expect(page.getByText('Đã quá hạn nộp bài')).toBeVisible();
  expect(await page.getByRole('button', { name: 'Nộp bài' }).count()).toBe(0);
  assignment = { ...baseAssignment, status: 'CLOSED' };
  await page.reload();
  await expect(page.getByText('Đã đóng', { exact: true })).toBeVisible();
  await expect(page.getByText('Bài tập đã đóng. Bạn vẫn có thể xem đề và bài đã lưu.')).toBeVisible();
  expect(await page.getByRole('button', { name: 'Nộp bài' }).count()).toBe(0);
  assignment = { ...baseAssignment, submission: { id: 'submission-graded', contentBlocks: [{ type: 'TEXT', content: 'Câu trả lời đã chấm' }], status: 'GRADED', score: 9, feedbackText: 'Lập luận rõ ràng.', version: 4 }, submissionAttachments: [] };
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Bài đã chấm' })).toBeVisible();
  await expect(page.getByText('9/10')).toBeVisible();
  await expect(page.getByText('Lập luận rõ ràng.')).toBeVisible();
  expect(await page.getByRole('button', { name: 'Nộp bài' }).count()).toBe(0);

  detailStatus = 503;
  await page.reload();
  await expect(page.getByText('Chưa đọc được bài tập')).toBeVisible();
  detailStatus = 200;
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByRole('heading', { name: 'Đạo hàm và ứng dụng' })).toBeVisible();
  delay = 1000;
  const pendingReload = page.reload({ waitUntil: 'commit' });
  await expect(page.getByRole('status', { name: 'Đang tải bài tập' })).toBeVisible();
  await pendingReload;
  await expect(page.getByRole('heading', { name: 'Đạo hàm và ứng dụng' })).toBeVisible();
});
