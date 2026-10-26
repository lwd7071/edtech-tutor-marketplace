import { expect, test, type Page } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const teacher = { id: 'teacher-assignments-fixture', email: 'teacher@example.test', fullName: 'Nguyễn Minh An', role: 'TEACHER', status: 'ACTIVE' };
const assignments = [
  { id: '11111111-1111-4111-8111-111111111111', title: 'Ôn tập phương trình bậc hai', studentName: 'Nguyễn Minh Anh', subjectName: 'Toán học', dueAt: '2026-10-20T12:00:00Z', status: 'PUBLISHED', submittedCount: 2, totalStudents: 5 },
  { id: '22222222-2222-4222-8222-222222222222', title: 'Bài tập điện học', studentName: 'Trần Gia Bảo', subjectName: 'Vật lý', dueAt: '2026-10-23T13:30:00Z', status: 'DRAFT', submittedCount: 0, totalStudents: 1 },
  { id: '33333333-3333-4333-8333-333333333333', title: 'Luyện viết đoạn văn', studentName: 'Lê Thu Hà', subjectName: 'Ngữ văn', dueAt: '2026-10-25T02:00:00Z', status: 'CLOSED', submittedCount: 4, totalStudents: 4 },
  ...Array.from({ length: 10 }, (_, index) => ({
    id: `aaaaaaaa-aaaa-4aaa-8aaa-${String(index + 1).padStart(12, '0')}`,
    title: `Bài tập bổ sung ${index + 1}`,
    studentName: `Học viên ${index + 1}`,
    subjectName: 'Toán học',
    dueAt: '2026-10-28T12:00:00Z',
    status: 'PUBLISHED',
    submittedCount: index % 3,
    totalStudents: 3,
  })),
];
const envelope = (data: unknown, page = 0, total = 24) => ({ success: true, message: 'ok', data, errors: null, meta: { page, size: 12, totalElements: total, totalPages: total ? 2 : 0 } });

async function openTeacherAssignments(page: Page) {
  await page.addInitScript(user => {
    localStorage.setItem('tutor-match.session', JSON.stringify({ user, remember: false, sessionId: 'teacher-assignments-session' }));
    document.cookie = 'accessToken=teacher-assignments-access; path=/; SameSite=Lax';
  }, teacher);
  await page.goto('/teacher/assignments');
}

async function interceptAssignments(page: Page, options: { fail?: boolean; hold?: boolean; empty?: boolean } = {}) {
  const requests: Array<{ method: string; path: string; params: Record<string, string> }> = [];
  let releaseGet: (() => void) | undefined;
  const gate = options.hold ? new Promise<void>(resolveGate => { releaseGet = resolveGate; }) : null;
  await page.route('**/api/teacher/assignments**', async route => {
    const request = route.request();
    const url = new URL(request.url());
    requests.push({ method: request.method(), path: url.pathname, params: Object.fromEntries(url.searchParams.entries()) });
    if (gate) await gate;
    if (options.fail) return route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ success: false, message: 'unavailable', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null }) });
    const pageNumber = Number(url.searchParams.get('page') ?? 0);
    const data = options.empty ? [] : pageNumber === 0 ? assignments.slice(0, 12) : assignments.slice(12);
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(data, pageNumber, options.empty ? 0 : 13)) });
  });
  return { requests, releaseGet: () => releaseGet?.() };
}

test('teacher assignment hierarchy fits mobile, tablet and desktop', async ({ page }) => {
  const { requests } = await interceptAssignments(page);
  await openTeacherAssignments(page);
  if (variant === 'before') {
    await expect(page.getByRole('heading', { name: 'Quản lý bài tập' })).toBeVisible();
    await expect(page.getByText('Ôn tập phương trình bậc hai')).toBeVisible();
  } else {
    await expect(page.getByRole('main')).toHaveCount(1);
    await expect(page.getByRole('heading', { name: 'Bài tập đã giao' })).toBeVisible();
    await expect(page.getByText('2 / 5')).toBeVisible();
    await expect(page.getByText('Đang giao').first()).toBeVisible();
    await expect(page.getByText('Bản nháp').first()).toBeVisible();
    await expect(page.getByText('Đã đóng').first()).toBeVisible();
    await expect(page.getByText('Nguyễn Minh Anh · Toán học')).toBeVisible();
  }
  expect(requests).toContainEqual(expect.objectContaining({ method: 'GET', path: '/api/teacher/assignments', params: { page: '0', size: '12' } }));

  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.evaluate(() => {
      document.documentElement.style.scrollBehavior = 'auto';
      window.scrollTo({ top: 0, left: 0, behavior: 'instant' });
      if (document.scrollingElement) document.scrollingElement.scrollTop = 0;
      document.querySelectorAll<HTMLElement>('*').forEach(element => {
        element.scrollTop = 0;
        element.scrollLeft = 0;
      });
    });
    await page.evaluate(() => new Promise<void>(resolveFrame => requestAnimationFrame(() => requestAnimationFrame(() => resolveFrame()))));
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    if (variant !== 'before') expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    // Capture the actual viewport: full-page screenshots render the fixed
    // workspace sidebar once at its scroll position, far below the page top.
    if (variant !== 'before') await expect(page.getByRole('heading', { name: 'Bài tập đã giao' })).toBeInViewport();
    await page.screenshot({ path: resolve(evidence, `teacher-assignments-${variant}-${width}.png`), fullPage: false });
    if (width <= 1000 && variant !== 'before') {
      await page.evaluate(() => window.scrollTo(0, document.documentElement.scrollHeight));
      const clearance = await page.evaluate(() => {
        const lastRow = [...document.querySelectorAll('article')].at(-1);
        const nav = [...document.querySelectorAll('nav')].map(node => ({ rect: node.getBoundingClientRect(), style: getComputedStyle(node) })).find(item => item.style.position === 'fixed' && item.rect.bottom >= window.innerHeight - 1);
        return { rowBottom: lastRow?.getBoundingClientRect().bottom, navTop: nav?.rect.top };
      });
      expect(clearance.rowBottom).toBeLessThan(clearance.navTop! - 8);
    }
  }

  if (variant !== 'before') {
    const createLink = page.getByRole('link', { name: 'Giao bài tập' });
    await expect(createLink).toHaveAttribute('href', '/teacher/assignments/new');
    await expect(page.getByRole('link', { name: 'Xem chi tiết' }).first()).toHaveAttribute('href', `/teacher/assignments/${assignments[0].id}`);
    await createLink.focus();
    await page.keyboard.press('Tab');
    await expect(page.getByRole('link', { name: 'Xem chi tiết' }).first()).toBeFocused();
    expect(await page.getByRole('link', { name: 'Xem chi tiết' }).first().evaluate(element => element.matches(':focus-visible'))).toBe(true);
  }
});

test('pagination preserves page and size API parameters and detail route', async ({ page }) => {
  const { requests } = await interceptAssignments(page);
  await openTeacherAssignments(page);
  await page.getByRole('listitem', { name: '2' }).click();
  await expect.poll(() => requests.at(-1)?.params.page).toBe('1');
  expect(requests.at(-1)?.params).toEqual(expect.objectContaining({ page: '1', size: '12' }));
  if (variant !== 'before') {
    await expect(page.getByText('Bài tập bổ sung 10')).toBeVisible();
    await expect(page.getByRole('link', { name: 'Xem chi tiết' }).first()).toHaveAttribute('href', `/teacher/assignments/${assignments[12].id}`);
  }
});

test('loading, GET error/retry and empty states stay distinct', async ({ page }) => {
  const held = await interceptAssignments(page, { hold: true });
  await openTeacherAssignments(page);
  if (variant === 'before') {
    await expect(page.getByText('Quản lý bài tập')).toBeVisible();
  } else {
    await expect(page.getByRole('status', { name: 'Đang tải bài tập' })).toBeVisible();
  }
  held.releaseGet();
  await expect(page.getByText('Ôn tập phương trình bậc hai')).toBeVisible();

  await page.unrouteAll({ behavior: 'wait' });
  await interceptAssignments(page, { fail: true });
  await page.reload();
  await expect(page.getByText('Chưa tải được bài tập')).toBeVisible();
  await page.unrouteAll({ behavior: 'wait' });
  await interceptAssignments(page, { empty: true });
  await page.getByRole('button', { name: 'Thử lại' }).click();
  if (variant === 'before') {
    await expect(page.getByText('Chưa có bài tập.')).toBeVisible();
  } else {
    await expect(page.getByRole('heading', { name: 'Chưa có bài tập nào' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Giao bài tập' }).first()).toHaveAttribute('href', '/teacher/assignments/new');
  }
});
