import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const admin = { id: 'admin-subjects', email: 'admin@example.test', fullName: 'Quản trị viên', role: 'ADMIN', status: 'ACTIVE' };
const proposal = {
  proposalId: 'subject-proposal-01', teacherId: 'teacher-17', teacherName: 'Nguyễn An',
  proposedName: 'Toán ứng dụng', proposedSubjectName: 'Toán ứng dụng', educationLevel: 'HIGH_SCHOOL',
  proposedCategory: 'HIGH_SCHOOL', description: 'Các chủ đề đại số và mô hình hóa.', status: 'PENDING',
  reviewNote: null, subjectId: null, createdAt: '2026-10-15T09:30:00Z', version: 4,
};

async function seedAdmin(page: import('@playwright/test').Page) {
  await page.addInitScript(user => {
    localStorage.setItem('tutor-match.session', JSON.stringify({ user, remember: false, sessionId: 'admin-subjects-session' }));
    document.cookie = 'accessToken=admin-subjects-access; path=/; SameSite=Lax';
  }, admin);
}

async function mockSubjectProposals(page: import('@playwright/test').Page, options: { empty?: boolean; failOnce?: boolean } = {}) {
  const state = { queries: [] as Array<{ page: string | null; size: string | null; sort: string | null }>, mutations: [] as Array<{ method: string; url: string; body: unknown }> };
  let record = { ...proposal, reviewNote: null as string | null, status: 'PENDING' as 'PENDING' | 'APPROVED' | 'REJECTED' };
  let remainingFailures = options.failOnce ? 1 : 0;
  await page.route('**/api/admin/subject-proposals**', async route => {
    const request = route.request();
    const url = new URL(request.url());
    if (request.method() === 'GET') {
      state.queries.push({ page: url.searchParams.get('page'), size: url.searchParams.get('size'), sort: url.searchParams.get('sort') });
      if (remainingFailures > 0) {
        remainingFailures -= 1;
        await route.fulfill({ status: 422, contentType: 'application/json', body: JSON.stringify({ message: 'Unavailable' }) });
        return;
      }
      const data = options.empty ? [] : [record];
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, message: 'ok', data, errors: null, meta: { page: Number(url.searchParams.get('page') || 0), size: Number(url.searchParams.get('size') || 10), totalElements: data.length, totalPages: data.length ? 1 : 0 } }) });
      return;
    }
    const body = request.postDataJSON();
    state.mutations.push({ method: request.method(), url: url.pathname, body });
    if (url.pathname.endsWith('/reject')) record = { ...record, status: 'REJECTED', reviewNote: (body as { reason: string }).reason };
    else record = { ...record, status: 'APPROVED' };
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, message: 'ok', data: record, errors: null, meta: null }) });
  });
  return state;
}

test('Admin subject queue keeps real proposal data legible and responsive', async ({ page }) => {
  const baseline = process.env.BASELINE_ADMIN_SUBJECTS === '1';
  await seedAdmin(page);
  const state = await mockSubjectProposals(page);
  await page.goto('/admin/subjects', { waitUntil: 'domcontentloaded' });
  await expect(page.getByRole('heading', { name: baseline ? 'Quản trị Đề xuất Môn học' : 'Duyệt đề xuất môn học' })).toBeVisible();
  await expect(page.getByText(proposal.proposedName)).toBeVisible();
  await expect(page.getByText('Nguyễn An')).toBeVisible();
  await expect(page.getByText('Chờ duyệt')).toBeVisible();
  expect(state.queries[0]).toEqual({ page: '0', size: '10', sort: 'createdAt,asc' });

  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    if (!baseline) {
      const dimensions = await page.evaluate(() => ({ body: document.body.scrollWidth, viewport: window.innerWidth }));
      expect(dimensions.body).toBeLessThanOrEqual(dimensions.viewport);
    }
    await page.screenshot({ path: resolve(evidence, `admin-subjects-${baseline ? 'before' : 'after'}-${width}.png`), fullPage: true });
  }
  if (!baseline) {
    await page.getByRole('button', { name: 'Phê duyệt' }).focus();
    expect(await page.locator(':focus').evaluate(element => getComputedStyle(element).outlineStyle)).not.toBe('none');
  }
});

test('Admin approval keeps the CREATE_NEW request and version', async ({ page }) => {
  await seedAdmin(page);
  const state = await mockSubjectProposals(page);
  await page.goto('/admin/subjects', { waitUntil: 'domcontentloaded' });
  await page.getByRole('button', { name: 'Phê duyệt' }).click();
  await expect(page.getByRole('dialog', { name: 'Chuẩn hóa đề xuất' })).toBeVisible();
  await expect(page.getByLabel('Tên môn học chuẩn hóa')).toHaveValue('Toán ứng dụng');
  await page.getByLabel('Mã môn học').fill('MATH_APP');
  await page.getByRole('button', { name: 'Phê duyệt & tạo môn học' }).click();
  await expect(page.getByRole('dialog')).toHaveCount(0);
  expect(state.mutations).toContainEqual({ method: 'POST', url: '/api/admin/subject-proposals/subject-proposal-01/approve', body: expect.objectContaining({ resolution: 'CREATE_NEW', code: 'MATH_APP', version: 4 }) });
});

test('Admin rejection validates the reason and keeps the versioned payload', async ({ page }) => {
  await seedAdmin(page);
  const state = await mockSubjectProposals(page);
  await page.goto('/admin/subjects', { waitUntil: 'domcontentloaded' });
  await page.getByRole('button', { name: 'Từ chối' }).click();
  await page.getByRole('button', { name: 'Xác nhận từ chối' }).click();
  await expect(page.getByText('Vui lòng nhập lý do từ chối đề xuất')).toBeVisible();
  await page.getByRole('textbox', { name: 'Lý do từ chối' }).fill('Thông tin mô tả chưa đầy đủ.');
  await page.getByRole('button', { name: 'Xác nhận từ chối' }).click();
  await expect(page.getByRole('dialog')).toHaveCount(0);
  expect(state.mutations).toContainEqual({ method: 'POST', url: '/api/admin/subject-proposals/subject-proposal-01/reject', body: { reason: 'Thông tin mô tả chưa đầy đủ.', version: 4 } });
});

test('Admin proposal query recovers from error and presents empty state', async ({ page }) => {
  await seedAdmin(page);
  await mockSubjectProposals(page, { failOnce: true });
  await page.goto('/admin/subjects', { waitUntil: 'domcontentloaded' });
  await expect(page.getByText('Không tải được đề xuất môn học.')).toBeVisible();
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByText(proposal.proposedName)).toBeVisible();

  await page.unrouteAll({ behavior: 'wait' });
  await mockSubjectProposals(page, { empty: true });
  await page.reload({ waitUntil: 'domcontentloaded' });
  await expect(page.getByText('Chưa có đề xuất môn học trong danh sách này.')).toBeVisible();
});
