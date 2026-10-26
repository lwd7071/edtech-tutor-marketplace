import { expect, test, type Page } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const teacher = { id: 'teacher-student-detail-fixture', email: 'teacher@example.test', fullName: 'Nguyễn Minh An', role: 'TEACHER', status: 'ACTIVE' };
const rows = [
  { studentId: 'student-1', studentName: 'Nguyễn Minh Anh', subjectName: 'Toán học', studentPackage: { id: 'package-1', subjectId: 'subject-math', packageName: 'Toán nền tảng', remainingSessions: 5, expiresAt: '2027-01-31T16:59:59Z', status: 'ACTIVE' } },
  { studentId: 'student-1', studentName: 'Nguyễn Minh Anh', subjectName: 'Toán học', studentPackage: { id: 'package-2', subjectId: 'subject-math', packageName: 'Luyện thi học kỳ', remainingSessions: 0, expiresAt: '2026-10-10T16:59:59Z', status: 'INACTIVE' } },
];
const envelope = (data: unknown, page = 0) => {
  const totalElements = Array.isArray(data) && data.length === 0 ? 0 : 24;
  return { success: true, message: 'ok', data, errors: null, meta: { page, size: 12, totalElements, totalPages: totalElements ? 2 : 0 } };
};

async function openPage(page: Page) {
  await page.addInitScript(user => {
    localStorage.setItem('tutor-match.session', JSON.stringify({ user, remember: false, sessionId: 'teacher-student-detail-session' }));
    document.cookie = 'accessToken=teacher-student-detail-access; path=/; SameSite=Lax';
  }, teacher);
  await page.goto('/teacher/students/student-1');
}

async function interceptPackages(page: Page, options: { fail?: boolean; hold?: boolean; empty?: boolean } = {}) {
  const requests: Array<{ method: string; path: string; page?: string; size?: string; studentId?: string }> = [];
  let releaseGet: (() => void) | undefined;
  const gate = options.hold ? new Promise<void>(resolve => { releaseGet = resolve; }) : null;
  await page.route('**/api/teacher/students*', async route => {
    const request = route.request();
    const url = new URL(request.url());
    requests.push({ method: request.method(), path: url.pathname, page: url.searchParams.get('page') ?? undefined, size: url.searchParams.get('size') ?? undefined, studentId: url.searchParams.get('studentId') ?? undefined });
    if (gate) await gate;
    if (options.fail) return route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ success: false, message: 'unavailable', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null }) });
    const pageNumber = Number(url.searchParams.get('page') ?? 0);
    const data = options.empty ? [] : pageNumber === 0 ? rows : [{ studentId: 'student-1', studentName: 'Nguyễn Minh Anh', subjectName: 'Toán học', studentPackage: { id: 'package-3', subjectId: 'subject-math', packageName: 'Toán nâng cao', remainingSessions: 2, expiresAt: '2027-02-28T16:59:59Z', status: 'ACTIVE' } }];
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(data, pageNumber)) });
  });
  return { requests, releaseGet: () => releaseGet?.() };
}

test('student package detail follows learner context and fits 375/768/1280', async ({ page }) => {
  const { requests } = await interceptPackages(page);
  await openPage(page);
  await expect(page.getByRole('heading', { name: 'Gói học của học viên' })).toBeVisible();
  expect(requests).toContainEqual(expect.objectContaining({ method: 'GET', path: '/api/teacher/students', page: '0', size: '12', studentId: 'student-1' }));
  if (variant !== 'before') {
    await expect(page.getByText('Nguyễn Minh Anh')).toBeVisible();
    await expect(page.getByRole('link', { name: 'Danh sách học viên' })).toHaveAttribute('href', '/teacher/students');
  }
  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    if (variant !== 'before') expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidence, `teacher-student-detail-${variant}-${width}.png`), fullPage: true });
    if (width === 375 && variant !== 'before') {
      await page.evaluate(() => window.scrollTo(0, document.documentElement.scrollHeight));
      const clearance = await page.evaluate(() => {
        const actions = [...document.querySelectorAll('a,button')].filter(element => ['Tạo buổi học', 'Giao bài tập'].includes(element.textContent?.trim() ?? ''));
        const nav = [...document.querySelectorAll('nav')].map(node => ({ rect: node.getBoundingClientRect(), style: getComputedStyle(node) })).find(item => item.style.position === 'fixed' && item.rect.bottom >= window.innerHeight - 1);
        return { actionsBottom: Math.max(...actions.map(element => element.getBoundingClientRect().bottom)), navTop: nav?.rect.top };
      });
      expect(clearance.actionsBottom).toBeLessThan(clearance.navTop! - 8);
    }
  }
});

test('booking eligibility, package assignment and pagination preserve existing actions', async ({ page }) => {
  const { requests } = await interceptPackages(page);
  await openPage(page);
  await expect(page.getByRole('link', { name: 'Giao bài tập' }).first()).toHaveAttribute('href', '/teacher/assignments/new?studentId=student-1&subjectId=subject-math');
  await expect(page.getByRole('button', { name: 'Tạo buổi học' }).nth(1)).toBeDisabled();
  await page.getByRole('button', { name: 'Tạo buổi học' }).first().click();
  await expect(page.getByRole('dialog', { name: 'Đặt lịch buổi học mới' })).toBeVisible();
  await page.getByRole('button', { name: 'Hủy' }).click();
  await page.getByRole('listitem', { name: '2' }).click();
  await expect(page.getByText('Toán nâng cao')).toBeVisible();
  expect(requests).toContainEqual(expect.objectContaining({ page: '1', size: '12', studentId: 'student-1' }));
});

test('loading, GET error and retry, then empty state are distinct', async ({ page }) => {
  const held = await interceptPackages(page, { hold: true });
  await openPage(page);
  await expect(page.getByRole('status', { name: 'Đang tải gói học của học viên' })).toBeVisible();
  held.releaseGet();
  await expect(page.getByText('Toán nền tảng')).toBeVisible();

  await page.unrouteAll({ behavior: 'wait' });
  await interceptPackages(page, { fail: true });
  await page.reload();
  await expect(page.getByText('Chưa tải được gói học')).toBeVisible();
  await page.unrouteAll({ behavior: 'wait' });
  await interceptPackages(page, { empty: true });
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByText('Học viên chưa có gói học.')).toBeVisible();
});
