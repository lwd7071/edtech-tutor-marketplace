import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const teacher = { id: 'chat-teacher', email: 'teacher-chat@example.test', fullName: 'Trần Thu Hà', role: 'TEACHER', status: 'ACTIVE' };
const conversation = {
  id: 'teacher-conversation-1',
  participantId: 'student-1',
  participantName: 'Nguyễn Minh An',
  participantAvatar: null,
  lastMessagePreview: 'Em đã hoàn thành bài tập ạ.',
  lastMessageAt: '2026-10-16T08:30:00Z',
  unreadCount: 3,
};
const envelope = (data: unknown, meta: unknown = null) => ({ success: true, message: 'ok', data, errors: null, meta });

async function openTeacherMessages(page: import('@playwright/test').Page) {
  await page.addInitScript(user => {
    localStorage.setItem('tutor-match.session', JSON.stringify({ user, remember: false, sessionId: 'teacher-chat-session' }));
    document.cookie = 'accessToken=teacher-chat-access; path=/; SameSite=Lax';
  }, teacher);
  await page.route('**/api/conversations/teacher-conversation-1/messages**', route => route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify(envelope([
      { id: 'student-message-1', conversationId: conversation.id, senderId: 'student-1', senderName: conversation.participantName, content: 'Em đã hoàn thành bài tập ạ.', messageType: 'TEXT', sentAt: '2026-10-16T08:30:00Z', attachment: null },
      { id: 'teacher-message-1', conversationId: conversation.id, senderId: teacher.id, senderName: teacher.fullName, content: 'Cô sẽ xem và phản hồi em nhé.', messageType: 'TEXT', sentAt: '2026-10-16T08:34:00Z', attachment: null },
    ], { page: 0, size: 20, totalElements: 2, totalPages: 1, hasNext: false })),
  }));
  await page.route(url => new URL(url).pathname === '/api/conversations', route => route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify(envelope([conversation], { page: 0, size: 20, totalElements: 1, totalPages: 1, hasNext: false })),
  }));
  await page.routeWebSocket('**/ws', socket => { socket.onMessage(() => {}); });
  await page.goto('/teacher/messages');
}

test('teacher inbox and selected thread use student language at all target sizes', async ({ page }) => {
  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  await openTeacherMessages(page);
  await expect(page.getByRole('heading', { name: 'Tin nhắn' })).toBeVisible();
  await expect(page.getByRole('button', { name: /Nguyễn Minh An/ })).toBeVisible();
  if (variant !== 'before') {
    await expect(page.getByText('Trao đổi với học viên của bạn')).toBeVisible();
    await expect(page.getByLabel('3 tin nhắn chưa đọc')).toBeVisible();
    await expect(page.getByRole('main')).toHaveCount(1);
  }

  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    if (variant !== 'before') {
      if (width >= 768) await expect(page.getByText('Tin nhắn với học viên sẽ xuất hiện tại đây.')).toBeVisible();
      const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
      expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    }
    await page.screenshot({ path: resolve(evidence, `teacher-messages-${variant}-${width}.png`), fullPage: true });
  }

  await page.getByRole('button', { name: /Nguyễn Minh An/ }).click();
  await expect(page).toHaveURL(/\/teacher\/messages\?id=teacher-conversation-1/);
  await expect(page.getByText('Cô sẽ xem và phản hồi em nhé.')).toBeVisible();
  if (variant !== 'before') {
    await expect(page.getByRole('region', { name: 'Cuộc trò chuyện với Nguyễn Minh An' })).toBeVisible();
    await expect(page.getByText('Tin nhắn riêng giữa bạn và học viên')).toBeVisible();
    await expect(page.getByLabel('Nội dung tin nhắn')).toBeVisible();
  }

  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    if (variant !== 'before') {
      const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
      expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
      if (width <= 768) {
        const composer = await page.locator('.tm-chat-composer').boundingBox();
        const bottomNavigation = await page.locator('.tm-workspace-bottom').boundingBox();
        expect(composer).not.toBeNull();
        expect(bottomNavigation).not.toBeNull();
        expect(composer!.y + composer!.height).toBeLessThanOrEqual(bottomNavigation!.y + 1);
      }
    }
    await page.screenshot({ path: resolve(evidence, `teacher-messages-thread-${variant}-${width}.png`), fullPage: true });
  }

  if (variant !== 'before') {
    await page.setViewportSize({ width: 375, height: 900 });
    await page.getByRole('button', { name: 'Danh sách' }).click();
    await expect(page).toHaveURL('/teacher/messages');
    await expect(page.getByRole('button', { name: /Nguyễn Minh An/ })).toBeVisible();
  }
});

test('teacher conversation and message loading, empty, error and retry states stay distinct', async ({ page }) => {
  await page.addInitScript(user => {
    localStorage.setItem('tutor-match.session', JSON.stringify({ user, remember: false, sessionId: 'teacher-chat-states-session' }));
    document.cookie = 'accessToken=teacher-chat-states-access; path=/; SameSite=Lax';
  }, teacher);

  let conversationState: 'loading' | 'empty' | 'error' | 'populated' = 'loading';
  let releaseConversationList!: () => void;
  const conversationGate = new Promise<void>(resolveGate => { releaseConversationList = resolveGate; });
  const releaseConversations = releaseConversationList;
  let holdMessages = true;
  let releaseMessageHistory!: () => void;
  const messageGate = new Promise<void>(resolveGate => { releaseMessageHistory = resolveGate; });
  const releaseMessages = releaseMessageHistory;
  let messageError = false;

  await page.route(url => new URL(url).pathname === '/api/conversations', async route => {
    await conversationGate;
    if (conversationState === 'error') {
      return route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ success: false, message: 'unavailable', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null }) });
    }
    const data = conversationState === 'populated' ? [conversation] : [];
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(data, { page: 0, size: 20, totalElements: data.length, totalPages: data.length ? 1 : 0, hasNext: false })) });
  });
  await page.route('**/api/conversations/teacher-conversation-1/messages**', async route => {
    if (holdMessages) await messageGate;
    if (messageError) {
      return route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ success: false, message: 'unavailable', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null }) });
    }
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope([], { page: 0, size: 20, totalElements: 0, totalPages: 0, hasNext: false })) });
  });
  await page.routeWebSocket('**/ws', socket => { socket.onMessage(() => {}); });

  await page.goto('/teacher/messages');
  await expect(page.getByRole('status', { name: 'Đang tải cuộc trò chuyện' })).toBeVisible();
  conversationState = 'empty';
  releaseConversations();
  await expect(page.getByText('Chưa có cuộc trò chuyện với học viên nào.')).toBeVisible();

  conversationState = 'error';
  await page.reload();
  await expect(page.getByText('Chưa tải được danh sách tin nhắn')).toBeVisible();
  conversationState = 'populated';
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByRole('button', { name: /Nguyễn Minh An/ })).toBeVisible();

  await page.getByRole('button', { name: /Nguyễn Minh An/ }).click();
  await expect(page.getByRole('status').filter({ hasText: 'Đang tải tin nhắn…' })).toBeVisible();
  holdMessages = false;
  releaseMessages();
  await expect(page.getByText('Chưa có tin nhắn. Hãy gửi lời chào để bắt đầu.')).toBeVisible();

  messageError = true;
  await page.reload();
  await expect(page.getByText('Chưa tải được tin nhắn')).toBeVisible();
  messageError = false;
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByText('Chưa có tin nhắn. Hãy gửi lời chào để bắt đầu.')).toBeVisible();
});
