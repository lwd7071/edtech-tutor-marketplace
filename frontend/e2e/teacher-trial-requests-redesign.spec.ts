import { expect, test, type Page } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const teacher = { id: 'teacher-trials-fixture', email: 'teacher@example.test', fullName: 'Nguyễn Minh An', role: 'TEACHER', status: 'ACTIVE' };
const rows = [
  { id: '11111111-1111-4111-8111-111111111111', teacherId: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', studentId: '22222222-2222-4222-8222-222222222222', subjectId: '33333333-3333-4333-8333-333333333333', preferredStartTime: '2026-10-19T12:00:00Z', note: 'Mình muốn ôn phương trình bậc hai trước kỳ kiểm tra.', status: 'PENDING', createdAt: '2026-10-16T08:00:00Z', version: 7 },
  { id: '44444444-4444-4444-8444-444444444444', teacherId: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', studentId: '55555555-5555-4555-8555-555555555555', subjectId: '66666666-6666-4666-8666-666666666666', preferredStartTime: '2026-10-20T13:30:00Z', note: 'Trao đổi về phần điện học.', status: 'ACCEPTED', createdAt: '2026-10-16T09:00:00Z', version: 2 },
  { id: '77777777-7777-4777-8777-777777777777', teacherId: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', studentId: '88888888-8888-4888-8888-888888888888', subjectId: '99999999-9999-4999-8999-999999999999', preferredStartTime: '2026-10-21T13:00:00Z', note: 'Cần luyện giao tiếp.', status: 'REJECTED', rejectionReason: 'Không còn giờ phù hợp.', createdAt: '2026-10-16T10:00:00Z', version: 3 },
];
const envelope = (data: unknown, total = 3) => ({ success: true, message: 'ok', data, errors: null, meta: { page: 0, size: 12, totalElements: total, totalPages: 1 } });

async function openTeacherPage(page: Page) {
  await page.addInitScript(user => {
    localStorage.setItem('tutor-match.session', JSON.stringify({ user, remember: false, sessionId: 'teacher-trials-session' }));
    document.cookie = 'accessToken=teacher-trials-access; path=/; SameSite=Lax';
  }, teacher);
  await page.goto('/teacher/trial-requests');
}

async function interceptTrials(page: Page, options: { fail?: boolean; hold?: boolean; empty?: boolean; conflict?: boolean } = {}) {
  const requests: Array<{ method: string; path: string; params: Record<string, string>; body?: unknown }> = [];
  let releaseGet: (() => void) | undefined;
  const gate = options.hold ? new Promise<void>(resolveGate => { releaseGet = resolveGate; }) : null;
  await page.route('**/api/teacher/trial-requests**', async route => {
    const request = route.request();
    const url = new URL(request.url());
    const entry = { method: request.method(), path: url.pathname, params: Object.fromEntries(url.searchParams.entries()), body: request.method() === 'GET' ? undefined : request.postDataJSON() };
    requests.push(entry);
    if (request.method() === 'GET') {
      if (gate) await gate;
      if (options.fail) return route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ success: false, message: 'unavailable', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null }) });
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(options.empty ? [] : rows, options.empty ? 0 : 3)) });
    }
    if (options.conflict) return route.fulfill({ status: 409, contentType: 'application/json', body: JSON.stringify({ success: false, message: 'changed', data: null, errors: [{ code: 'CONCURRENT_MODIFICATION', message: 'changed' }], meta: null }) });
    return route.fulfill({ status: request.url().includes('/accept') ? 201 : 200, contentType: 'application/json', body: JSON.stringify(envelope(rows[0])) });
  });
  return { requests, releaseGet: () => releaseGet?.() };
}

test('trial request hierarchy fits mobile, tablet and desktop', async ({ page }) => {
  const { requests } = await interceptTrials(page);
  await openTeacherPage(page);
  await expect(page.getByRole('heading', { name: 'Yêu cầu học thử' })).toBeVisible();
  expect(requests).toContainEqual(expect.objectContaining({ method: 'GET', path: '/api/teacher/trial-requests', params: { page: '0', size: '12' } }));
  await expect(page.getByText('Mình muốn ôn phương trình bậc hai trước kỳ kiểm tra.')).toBeVisible();
  if (variant !== 'before') {
    await expect(page.getByText('22222222')).toBeVisible();
    await expect(page.getByText('33333333')).toBeVisible();
    await expect(page.getByText('Không còn giờ phù hợp.')).toBeVisible();
  }
  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidence, `teacher-trial-requests-${variant}-${width}.png`), fullPage: true });
    if (width === 375 && variant !== 'before') {
      await page.evaluate(() => window.scrollTo(0, document.documentElement.scrollHeight));
      const clearance = await page.evaluate(() => {
        const lastRequest = [...document.querySelectorAll('article')].at(-1);
        const nav = [...document.querySelectorAll('nav')].map(node => ({ rect: node.getBoundingClientRect(), style: getComputedStyle(node) })).find(item => item.style.position === 'fixed' && item.rect.bottom >= window.innerHeight - 1);
        return { requestBottom: lastRequest?.getBoundingClientRect().bottom, navTop: nav?.rect.top };
      });
      expect(clearance.requestBottom).toBeLessThan(clearance.navTop! - 8);
    }
  }
  if (variant !== 'before') {
    await page.getByRole('button', { name: 'Chọn thời gian và chấp nhận' }).focus();
    await page.keyboard.press('Tab');
    await expect(page.getByRole('button', { name: 'Từ chối' }).first()).toBeFocused();
    expect(await page.getByRole('button', { name: 'Từ chối' }).first().evaluate(element => element.matches(':focus-visible'))).toBe(true);
  }
});

test('accept and reject retain the trial API payload and optimistic version', async ({ page }) => {
  const { requests } = await interceptTrials(page);
  await openTeacherPage(page);
  await page.getByRole('button', { name: 'Chọn thời gian và chấp nhận' }).click();
  await page.getByLabel('Bắt đầu (giờ Việt Nam)').fill('2026-10-19T19:00');
  await page.getByLabel('Kết thúc').fill('2026-10-19T20:00');
  await page.getByLabel('Hình thức').click();
  await page.getByText('Online', { exact: true }).last().click();
  await page.getByRole('button', { name: 'Tạo buổi học thử' }).click();
  const trialId = rows[0].id;
  await expect.poll(() => requests.some(request => request.method === 'POST' && request.path.endsWith(`/${trialId}/accept`))).toBe(true);
  expect(requests.find(request => request.path.endsWith(`/${trialId}/accept`))?.body).toEqual({ startTime: '2026-10-19T12:00:00.000Z', endTime: '2026-10-19T13:00:00.000Z', deliveryMode: 'ONLINE', version: 7 });
  await expect(page.getByText('Đã phản hồi yêu cầu')).toBeVisible();

  await page.getByRole('button', { name: 'Từ chối' }).first().click();
  await page.getByRole('textbox', { name: '* Lý do' }).fill('Lịch đã kín.');
  await page.getByRole('button', { name: 'Xác nhận từ chối' }).click();
  await expect.poll(() => requests.some(request => request.path.endsWith(`/${trialId}/reject`))).toBe(true);
  expect(requests.find(request => request.path.endsWith(`/${trialId}/reject`))?.body).toEqual({ reason: 'Lịch đã kín.', version: 7 });
});

test('loading, error/retry, empty and concurrent update stay distinct', async ({ page }) => {
  const held = await interceptTrials(page, { hold: true });
  await openTeacherPage(page);
  await expect(page.getByRole('region', { name: 'Đang tải yêu cầu học thử' })).toBeVisible();
  held.releaseGet();
  await expect(page.getByText('Mình muốn ôn phương trình bậc hai trước kỳ kiểm tra.')).toBeVisible();

  await page.unrouteAll({ behavior: 'wait' });
  await interceptTrials(page, { fail: true });
  await page.reload();
  await expect(page.getByText('Chưa tải được yêu cầu học thử')).toBeVisible();
  await page.unrouteAll({ behavior: 'wait' });
  await interceptTrials(page, { empty: true });
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByRole('heading', { name: 'Chưa có yêu cầu học thử' })).toBeVisible();

  await page.unrouteAll({ behavior: 'wait' });
  await interceptTrials(page, { conflict: true });
  await page.reload();
  await page.getByRole('button', { name: 'Từ chối' }).first().click();
  await page.getByRole('textbox', { name: '* Lý do' }).fill('Lịch đã kín.');
  await page.getByRole('button', { name: 'Xác nhận từ chối' }).click();
  await expect(page.getByText('Dữ liệu đã được thay đổi bởi người khác. Vui lòng tải lại.')).toBeVisible();
});
