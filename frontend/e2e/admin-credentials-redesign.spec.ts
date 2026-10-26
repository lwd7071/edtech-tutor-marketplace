import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const admin = { id: 'admin-credentials', email: 'admin@example.test', fullName: 'Quản trị viên', role: 'ADMIN', status: 'ACTIVE' };
const credential = { id: 'credential-001', teacherId: 'teacher-017', label: 'Bằng cử nhân sư phạm', status: 'PENDING', rejectedReason: null, version: 3 };

async function seedAdmin(page: import('@playwright/test').Page) {
  await page.addInitScript(user => {
    localStorage.setItem('tutor-match.session', JSON.stringify({ user, remember: false, sessionId: 'admin-credentials-session' }));
    document.cookie = 'accessToken=admin-credentials-access; path=/; SameSite=Lax';
  }, admin);
}

async function mockCredentials(page: import('@playwright/test').Page, options: { empty?: boolean; failListOnce?: boolean; failProofOnce?: boolean; delayList?: number } = {}) {
  const state = { listStatuses: [] as string[], proofCalls: 0, mutations: [] as Array<{ method: string; pathname: string; version: string | null; reason: string | null }> };
  let record = { ...credential, rejectedReason: null as string | null };
  let remainingListFailures = options.failListOnce ? 1 : 0;
  let proofFailed = false;

  await page.route('**/api/admin/credentials**', async route => {
    const request = route.request();
    const url = new URL(request.url());
    if (url.pathname.endsWith('/proof')) {
      state.proofCalls += 1;
      if (options.failProofOnce && !proofFailed) {
        proofFailed = true;
        await route.fulfill({ status: 500, contentType: 'application/json', body: JSON.stringify({ message: 'Unavailable' }) });
        return;
      }
      await route.fulfill({ status: 200, contentType: 'application/pdf', body: Buffer.from('%PDF-1.4 fixture') });
      return;
    }

    if (request.method() === 'GET') {
      const status = url.searchParams.get('status') ?? '';
      state.listStatuses.push(status);
      if (remainingListFailures > 0) {
        remainingListFailures -= 1;
        await route.fulfill({ status: 500, contentType: 'application/json', body: JSON.stringify({ message: 'Unavailable' }) });
        return;
      }
      if (options.delayList) await new Promise(resolveDelay => setTimeout(resolveDelay, options.delayList));
      const data = options.empty ? [] : status === record.status ? [record] : status === 'PENDING' ? [] : [{ ...credential, status, rejectedReason: status === 'REJECTED' ? 'Thiếu giấy tờ bổ sung.' : null }];
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, message: 'ok', data, errors: null, meta: null }) });
      return;
    }

    state.mutations.push({ method: request.method(), pathname: url.pathname, version: url.searchParams.get('version'), reason: url.searchParams.get('reason') });
    if (url.pathname.endsWith('/approve')) record = { ...record, status: 'APPROVED' };
    if (url.pathname.endsWith('/reject')) record = { ...record, status: 'REJECTED', rejectedReason: url.searchParams.get('reason') };
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, message: 'ok', data: record, errors: null, meta: null }) });
  });
  return state;
}

test('Admin credential queue shows the three API states and responsive list', async ({ page }) => {
  await seedAdmin(page);
  const state = await mockCredentials(page);
  await page.goto('/admin/credentials', { waitUntil: 'domcontentloaded' });
  await expect(page.getByRole('heading', { name: 'Duyệt minh chứng năng lực' })).toBeVisible();
  await expect(page.getByText(credential.label)).toBeVisible();
  await expect(page.getByText('Mã gia sư: teacher-017')).toBeVisible();
  expect([...new Set(state.listStatuses)]).toEqual(['PENDING']);

  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    const dimensions = await page.evaluate(() => ({ body: document.body.scrollWidth, viewport: window.innerWidth }));
    expect(dimensions.body).toBeLessThanOrEqual(dimensions.viewport);
    await page.screenshot({ path: resolve(evidence, `admin-credentials-after-${width}.png`), fullPage: true });
  }

  await page.getByRole('button', { name: 'Xem file' }).focus();
  await page.keyboard.press('Tab');
  await expect(page.locator(':focus')).toHaveText('Duyệt');
  expect(await page.locator(':focus').evaluate(element => getComputedStyle(element).outlineStyle)).not.toBe('none');

  await page.getByRole('tab', { name: 'Đã duyệt' }).click();
  await expect(page.getByText(credential.label)).toBeVisible();
  await expect(page.getByRole('article').getByText('Đã duyệt')).toBeVisible();
  await expect(page.getByRole('button', { name: /^Duyệt/ })).toHaveCount(0);
  await expect(page.getByRole('button', { name: /^Từ chối/ })).toHaveCount(0);
  await page.getByRole('tab', { name: 'Đã từ chối' }).click();
  await expect(page.getByText('Thiếu giấy tờ bổ sung.')).toBeVisible();
  await expect(page.getByRole('button', { name: /^Duyệt/ })).toHaveCount(0);
  expect([...new Set(state.listStatuses)]).toEqual(['PENDING', 'APPROVED', 'REJECTED']);
});

test('Admin credential preview preserves file authorization and recovers after error', async ({ page }) => {
  await seedAdmin(page);
  const state = await mockCredentials(page, { failProofOnce: true });
  await page.goto('/admin/credentials', { waitUntil: 'domcontentloaded' });
  await page.getByRole('button', { name: 'Xem file' }).click();
  await expect(page.getByText('Không mở được file minh chứng.')).toBeVisible();
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByRole('dialog', { name: 'Bằng cử nhân sư phạm' })).toBeVisible();
  await expect(page.getByTitle('File minh chứng')).toHaveAttribute('src', /^blob:/);
  expect(state.proofCalls).toBe(2);
  expect([...new Set(state.listStatuses)]).toEqual(['PENDING']);
  await page.getByRole('button', { name: 'Đóng' }).click();
});

test('Admin approves the credential using its ID and version', async ({ page }) => {
  await seedAdmin(page);
  const state = await mockCredentials(page);
  await page.goto('/admin/credentials', { waitUntil: 'domcontentloaded' });
  await page.getByRole('button', { name: `Duyệt ${credential.label}` }).click();
  await expect(page.getByText('Không có minh chứng ở trạng thái này.')).toBeVisible();
  expect(state.mutations).toEqual([{ method: 'POST', pathname: '/api/admin/credentials/credential-001/approve', version: '3', reason: null }]);
});

test('Admin rejection requires a reason and keeps the existing query payload', async ({ page }) => {
  await seedAdmin(page);
  const state = await mockCredentials(page);
  page.on('dialog', dialog => dialog.accept('Thiếu bản sao có xác thực.'));
  await page.goto('/admin/credentials', { waitUntil: 'domcontentloaded' });
  await page.getByRole('button', { name: `Từ chối ${credential.label}` }).click();
  await expect(page.getByText('Không có minh chứng ở trạng thái này.')).toBeVisible();
  expect(state.mutations).toEqual([{ method: 'POST', pathname: '/api/admin/credentials/credential-001/reject', version: '3', reason: 'Thiếu bản sao có xác thực.' }]);
});

test('Admin credential page presents retry and empty states', async ({ page }) => {
  await seedAdmin(page);
  await mockCredentials(page, { failListOnce: true });
  await page.goto('/admin/credentials', { waitUntil: 'domcontentloaded' });
  await expect(page.getByText('Không tải được danh sách minh chứng.')).toBeVisible();
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByText(credential.label)).toBeVisible();

  await page.unrouteAll({ behavior: 'wait' });
  await mockCredentials(page, { empty: true });
  await page.reload({ waitUntil: 'domcontentloaded' });
  await expect(page.getByText('Không có minh chứng ở trạng thái này.')).toBeVisible();
});
