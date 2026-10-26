import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const student = { id: 'chat-student', email: 'chat@example.test', fullName: 'Nguyễn Minh An', role: 'STUDENT', status: 'ACTIVE' };
const conversation = { id: 'conversation-1', participantId: 'teacher-1', participantName: 'Trần Thu Hà', participantAvatar: null, lastMessagePreview: 'Em gửi bài tập qua đây nhé cô.', lastMessageAt: '2026-10-16T08:30:00Z', unreadCount: 2 };
const attachment = { id: 'message-file-1', secureUrl: 'https://files.example.test/bai-tap.pdf', originalFilename: 'bai-tap.pdf', mimeType: 'application/pdf', fileSize: 2048 };
const envelope = (data: unknown, meta: unknown = null) => ({ success: true, message: 'ok', data, errors: null, meta });

test('student messages redesign preserves inbox, thread history, attachment, route and responsive workspace', async ({ page }) => {
  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  let conversationPage = 0;
  let messagePage = 0;
  const conversationError = false;
  let messageError = false;
  const uploads: string[] = [];
  await page.route('**/api/auth/login', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope({ accessToken: 'visual-access', refreshToken: 'visual-refresh', user: student })) }));
  await page.route('**/api/conversations**', async route => {
    const url = new URL(route.request().url());
    conversationPage = Number(url.searchParams.get('page') || 0);
    await route.fulfill({ status: conversationError ? 503 : 200, contentType: 'application/json', body: JSON.stringify(conversationError ? { success: false, message: 'unavailable', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null } : envelope([conversation], { page: conversationPage, size: 20, totalElements: 1, totalPages: 1, hasNext: false })) });
  });
  await page.route('**/api/conversations/conversation-1/messages**', async route => {
    const url = new URL(route.request().url());
    messagePage = Number(url.searchParams.get('page') || 0);
    await route.fulfill({ status: messageError ? 503 : 200, contentType: 'application/json', body: JSON.stringify(messageError ? { success: false, message: 'unavailable', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null } : envelope(messagePage === 0 ? [
      { id: 'message-1', conversationId: conversation.id, senderId: 'teacher-1', senderName: conversation.participantName, messageType: 'TEXT', content: 'Chào An, em cần cô xem phần nào?', sentAt: '2026-10-16T08:20:00Z', attachment: null },
      { id: 'message-2', conversationId: conversation.id, senderId: student.id, senderName: student.fullName, messageType: 'TEXT', content: 'Em gửi bài tập qua đây nhé cô.', sentAt: '2026-10-16T08:30:00Z', attachment: null },
    ] : [{ id: 'message-old', conversationId: conversation.id, senderId: 'teacher-1', messageType: 'TEXT', content: 'Tin nhắn cũ hơn.', sentAt: '2026-10-16T08:00:00Z', attachment: null }], { page: messagePage, size: 20, totalElements: 40, totalPages: 2, hasNext: messagePage === 0 })) });
  });
  await page.route('**/api/attachments', async route => {
    uploads.push(route.request().postData() || '');
    await route.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify(envelope(attachment)) });
  });
  await page.routeWebSocket('**/ws', socket => { socket.onMessage(() => {}); });

  await page.goto('/auth/login');
  await page.getByPlaceholder('Nhập email của bạn').fill(student.email);
  await page.getByPlaceholder('Nhập mật khẩu').fill('Password123!');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page).toHaveURL(/\/student$/);
  await page.goto('/student/messages');
  await expect(page.getByRole('heading', { name: 'Tin nhắn' })).toBeVisible();
  await expect(page.getByRole('button', { name: /Trần Thu Hà/ })).toBeVisible();
  await expect(page.getByText('15:30')).toBeVisible();

  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    if (variant === 'after') {
      if (width >= 768) await expect(page.getByRole('heading', { name: 'Chọn một cuộc trò chuyện' })).toBeVisible();
      else await expect(page.getByRole('heading', { name: 'Tin nhắn' })).toBeVisible();
      const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
      expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    }
    await page.screenshot({ path: resolve(evidence, `student-messages-${variant}-${width}.png`), fullPage: true });
  }
  await page.getByRole('button', { name: /Trần Thu Hà/ }).click();
  await expect(page).toHaveURL(/\?id=conversation-1/);
  await expect(page.getByText('Chào An, em cần cô xem phần nào?')).toBeVisible();
  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    if (variant === 'after') {
      await expect(page.getByLabel('Nội dung tin nhắn')).toBeVisible();
      const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
      expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    }
    await page.screenshot({ path: resolve(evidence, `student-messages-thread-${variant}-${width}.png`), fullPage: true });
  }
  if (variant === 'before') return;

  await page.getByRole('button', { name: 'Tải tin nhắn cũ hơn' }).click();
  await expect(page.getByText('Tin nhắn cũ hơn.')).toBeVisible();
  expect(messagePage).toBe(1);
  await page.setViewportSize({ width: 375, height: 900 });
  await page.getByRole('button', { name: 'Danh sách' }).click();
  await expect(page).toHaveURL(/\/student\/messages$/);

  await page.getByRole('button', { name: /Trần Thu Hà/ }).click();
  await page.getByLabel('Chọn tệp đính kèm').setInputFiles({ name: 'bai-tap.pdf', mimeType: 'application/pdf', buffer: Buffer.from('exercise') });
  await page.getByRole('button', { name: 'Gửi tệp đính kèm' }).click();
  await expect(page.getByText('bai-tap.pdf')).toBeVisible();
  expect(uploads).toHaveLength(1);
  expect(uploads[0]).toContain('MESSAGE');

  messageError = true;
  await page.reload();
  await page.goto('/student/messages?id=conversation-1');
  await expect(page.getByText('Chưa tải được tin nhắn')).toBeVisible();
  messageError = false;
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByText('Chào An, em cần cô xem phần nào?')).toBeVisible();
  await page.keyboard.press('Tab');
  const focused = await page.evaluate(() => document.activeElement?.matches(':focus-visible') ?? false);
  expect(focused).toBe(true);
  for (const width of [375, 768]) {
    await page.setViewportSize({ width, height: 900 });
    const bounds = await page.evaluate(() => ({ composer: document.querySelector('.tm-chat-composer')?.getBoundingClientRect().bottom ?? 0, nav: document.querySelector('.tm-workspace-bottom')?.getBoundingClientRect().top ?? Number.POSITIVE_INFINITY }));
    expect(bounds.composer).toBeGreaterThan(0);
    expect(bounds.nav).toBeLessThan(900);
    expect(bounds.composer).toBeLessThanOrEqual(bounds.nav + 1);
  }
});

test('teacher messages route retains the shared chat shell and role workspace navigation', async ({ page }) => {
  const teacher = { id: 'chat-teacher', email: 'teacher-chat@example.test', fullName: 'Trần Thu Hà', role: 'TEACHER', status: 'ACTIVE' };
  const teacherConversation = { ...conversation, participantId: 'student-1', participantName: 'Nguyễn Minh An' };
  await page.route('**/api/auth/login', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope({ accessToken: 'visual-teacher-access', refreshToken: 'visual-teacher-refresh', user: teacher })) }));
  await page.route('**/api/conversations**', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope([teacherConversation], { page: 0, size: 20, totalElements: 1, totalPages: 1, hasNext: false })) }));
  await page.routeWebSocket('**/ws', socket => { socket.onMessage(() => {}); });
  await page.goto('/auth/login');
  await page.getByPlaceholder('Nhập email của bạn').fill(teacher.email);
  await page.getByPlaceholder('Nhập mật khẩu').fill('Password123!');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page).toHaveURL(/\/teacher$/);
  await page.goto('/teacher/messages');
  await expect(page.getByRole('heading', { name: 'Tin nhắn' })).toBeVisible();
  await expect(page.getByRole('button', { name: /Nguyễn Minh An/ })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Tin nhắn' }).first()).toHaveAttribute('href', '/teacher/messages');
});
