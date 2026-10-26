import { expect, test, type Page } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const teacher = { id: 'teacher-students-fixture', email: 'teacher@example.test', fullName: 'Nguyễn Minh An', role: 'TEACHER', status: 'ACTIVE' };
const rows = [
  { studentId: 'student-1', studentName: 'Nguyễn Minh Anh', subjectName: 'Toán học', studentPackage: { id: 'package-1', subjectId: 'subject-math', packageName: 'Toán nền tảng', remainingSessions: 5, expiresAt: '2027-01-31T16:59:59Z', status: 'ACTIVE' } },
  { studentId: 'student-2', studentName: 'Trần Gia Bảo', subjectName: 'Vật lý', studentPackage: { id: 'package-2', subjectId: 'subject-physics', packageName: 'Cơ học căn bản', remainingSessions: 0, expiresAt: '2026-10-10T16:59:59Z', status: 'INACTIVE' } },
];
const envelope = (data: unknown, page = 0) => {
  const totalElements = Array.isArray(data) && data.length === 0 ? 0 : 24;
  return { success: true, message: 'ok', data, errors: null, meta: { page, size: 12, totalElements, totalPages: totalElements ? 2 : 0 } };
};

async function openPage(page: Page) {
  await page.addInitScript(user => {
    try {
      localStorage.setItem('tutor-match.session', JSON.stringify({ user, remember: false, sessionId: 'teacher-students-session' }));
      document.cookie = 'accessToken=teacher-students-access; path=/; SameSite=Lax';
    } catch {
      // The initial about:blank document has no storage origin; the route navigation does.
    }
  }, teacher);
  await page.goto('/teacher/students');
}

async function interceptLearners(page: Page, options: { fail?: boolean; hold?: boolean; empty?: boolean } = {}) {
  const requests: Array<{ method: string; path: string; page?: string; size?: string }> = [];
  let releaseGet: (() => void) | undefined;
  const gate = options.hold ? new Promise<void>(resolve => { releaseGet = resolve; }) : null;
  await page.route('**/api/teacher/students*', async route => {
    const request = route.request();
    const url = new URL(request.url());
    requests.push({ method: request.method(), path: url.pathname, page: url.searchParams.get('page') ?? undefined, size: url.searchParams.get('size') ?? undefined });
    if (gate) await gate;
    if (options.fail) return route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ success: false, message: 'unavailable', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null }) });
    const pageNumber = Number(url.searchParams.get('page') ?? 0);
    const data = options.empty ? [] : pageNumber === 0 ? rows : [{ studentId: 'student-3', studentName: 'Lê Minh Khang', subjectName: 'Hóa học', studentPackage: { id: 'package-3', subjectId: 'subject-chemistry', packageName: 'Hóa căn bản', remainingSessions: 2, expiresAt: '2027-02-28T16:59:59Z', status: 'ACTIVE' } }];
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(data, pageNumber)) });
  });
  return { requests, releaseGet: () => releaseGet?.() };
}

test('teacher student list matches fixture and fits 375/768/1280', async ({ page }) => {
  const { requests } = await interceptLearners(page);
  await openPage(page);
  await expect(page.getByRole('heading', { name: 'Học viên và gói đang học' })).toBeVisible();
  expect(requests).toContainEqual(expect.objectContaining({ method: 'GET', path: '/api/teacher/students', page: '0', size: '12' }));
  if (variant !== 'before') {
    await expect(page.getByText('Toán nền tảng')).toBeVisible();
    await expect(page.getByText('Cơ học căn bản')).toBeVisible();
  }
  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    if (variant !== 'before') expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidence, `teacher-students-${variant}-${width}.png`), fullPage: true });
    if (width === 375 && variant !== 'before') {
      await page.evaluate(async () => {
        document.documentElement.style.scrollBehavior = 'auto';
        window.scrollTo(0, document.documentElement.scrollHeight);
        await new Promise(resolve => setTimeout(resolve, 100));
      });
      const clearance = await page.evaluate(() => {
        const actions = [...document.querySelectorAll('a,button')].filter(element => ['Tạo buổi học', 'Giao bài tập'].includes(element.textContent?.trim() ?? ''));
        const nav = [...document.querySelectorAll('nav')].map(node => ({ rect: node.getBoundingClientRect(), style: getComputedStyle(node) })).find(item => item.style.position === 'fixed' && item.rect.bottom >= window.innerHeight - 1);
        return { actionsBottom: Math.max(...actions.map(element => element.getBoundingClientRect().bottom)), navTop: nav?.rect.top };
      });
      expect(clearance.actionsBottom).toBeDefined();
      expect(clearance.navTop).toBeDefined();
      expect(clearance.actionsBottom).toBeLessThan(clearance.navTop! - 8);
    }
  }
});

test('student links, assignment destination, eligible booking modal and server pagination stay intact', async ({ page }) => {
  const { requests } = await interceptLearners(page);
  await openPage(page);
  await expect(page.getByRole('link', { name: 'Nguyễn Minh Anh' })).toHaveAttribute('href', '/teacher/students/student-1');
  await expect(page.getByRole('link', { name: 'Giao bài tập' }).first()).toHaveAttribute('href', '/teacher/assignments/new?studentId=student-1&subjectId=subject-math');
  await expect(page.getByRole('button', { name: 'Tạo buổi học' }).nth(1)).toBeDisabled();
  await page.getByRole('button', { name: 'Tạo buổi học' }).first().click();
  await expect(page.getByRole('dialog', { name: 'Đặt lịch buổi học mới' })).toBeVisible();
  await page.getByRole('button', { name: 'Hủy' }).click();
  await page.getByRole('listitem', { name: '2' }).click();
  await expect(page.getByText('Hóa căn bản')).toBeVisible();
  expect(requests).toContainEqual(expect.objectContaining({ method: 'GET', path: '/api/teacher/students', page: '1', size: '12' }));
});

test('loading, GET error and retry, then empty state are distinct', async ({ page }) => {
  const held = await interceptLearners(page, { hold: true });
  await openPage(page);
  await expect(page.getByRole('status', { name: 'Đang tải danh sách học viên' })).toBeVisible();
  held.releaseGet();
  await expect(page.getByText('Toán nền tảng')).toBeVisible();

  await page.unrouteAll({ behavior: 'wait' });
  await interceptLearners(page, { fail: true });
  await page.reload();
  await expect(page.getByText('Chưa tải được học viên')).toBeVisible();
  await page.unrouteAll({ behavior: 'wait' });
  await interceptLearners(page, { empty: true });
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByText('Chưa có gói học của học viên trong tài khoản.')).toBeVisible();
});
