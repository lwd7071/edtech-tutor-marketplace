import { expect, test, type Page } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const teacher = { id: 'payout-teacher', email: 'payout@example.test', fullName: 'Trần Thu Hà', role: 'TEACHER', status: 'APPROVED' };
const wallet = { id: 'wallet-1', teacherId: teacher.id, pendingBalanceVnd: 500000, availableBalanceVnd: 1500000, reservedBalanceVnd: 200000, heldBalanceVnd: 1200000, version: 4 };
const bankAccount = { id: 'bank-1', bankBin: '970422', bankName: 'MB Bank', accountNumberMasked: '****6789', accountHolderName: 'TRAN THU HA', isVerified: true, isDefault: true, createdAt: '2026-10-01T08:30:00Z', version: 3 };
const payout = { id: 'payout-12345678', teacherId: teacher.id, walletId: wallet.id, bankAccountId: bankAccount.id, amountVnd: 350000, status: 'PENDING', teacherNote: 'Rút về tài khoản mặc định', adminNote: null, bankReference: null, proofUrl: null, version: 0, createdAt: '2026-10-16T08:30:00Z' };
const envelope = (data: unknown, meta: unknown = null) => ({ success: true, message: 'ok', data, errors: null, meta });

async function installPayoutFixtures(page: Page) {
  const requests: Array<{ method: string; path: string; query: URLSearchParams; body: string | null; headers: Record<string, string> }> = [];
  const status = { wallet: 200, bank: 200, payouts: 200, create: 201 };
  let walletData = wallet;
  let banks = [bankAccount];
  let payouts = [payout];
  let responseDelay = 0;
  const wait = async () => { if (responseDelay) await new Promise(resolveDelay => setTimeout(resolveDelay, responseDelay)); };
  await page.addInitScript(user => {
    localStorage.setItem('tutor-match.session', JSON.stringify({ user, remember: false, sessionId: 'teacher-payout-session' }));
    document.cookie = 'accessToken=teacher-payout-access; path=/; SameSite=Lax';
  }, teacher);
  await page.route('**/api/teacher/wallet', async route => {
    const request = route.request();
    requests.push({ method: request.method(), path: new URL(request.url()).pathname, query: new URLSearchParams(), body: request.postData(), headers: await request.allHeaders() });
    await wait();
    await route.fulfill({ status: status.wallet, contentType: 'application/json', body: JSON.stringify(status.wallet === 200 ? envelope(walletData) : { success: false, message: 'unavailable', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null }) });
  });
  await page.route('**/api/teacher/bank-accounts**', async route => {
    const request = route.request();
    const url = new URL(request.url());
    requests.push({ method: request.method(), path: url.pathname, query: url.searchParams, body: request.postData(), headers: await request.allHeaders() });
    await wait();
    await route.fulfill({ status: status.bank, contentType: 'application/json', body: JSON.stringify(status.bank === 200 ? envelope(banks) : { success: false, message: 'unavailable', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null }) });
  });
  await page.route('**/api/teacher/payout-requests**', async route => {
    const request = route.request();
    const url = new URL(request.url());
    requests.push({ method: request.method(), path: url.pathname, query: url.searchParams, body: request.postData(), headers: await request.allHeaders() });
    await wait();
    if (request.method() === 'GET') {
      await route.fulfill({ status: status.payouts, contentType: 'application/json', body: JSON.stringify(status.payouts === 200 ? envelope(payouts, { page: Number(url.searchParams.get('page') || 0), size: Number(url.searchParams.get('size') || 20), totalElements: payouts.length, totalPages: payouts.length ? 1 : 0, hasNext: false }) : { success: false, message: 'unavailable', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null }) });
      return;
    }
    const body = JSON.parse(request.postData() || '{}');
    if (status.create !== 201) {
      await route.fulfill({ status: status.create, contentType: 'application/json', body: JSON.stringify({ success: false, message: 'Tạm thời không thể tạo yêu cầu rút tiền', data: null, errors: [{ code: 'INTERNAL_ERROR', message: 'Tạm thời không thể tạo yêu cầu rút tiền' }], meta: null }) });
      return;
    }
    const created = { ...payout, id: 'payout-created-1', bankAccountId: body.bankAccountId, amountVnd: body.amountVnd, teacherNote: body.teacherNote, status: 'PENDING' };
    payouts = [created, ...payouts];
    await route.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify(envelope(created)) });
  });
  return {
    requests,
    setStatus: (next: Partial<typeof status>) => Object.assign(status, next),
    setPayouts: (next: typeof payouts) => { payouts = next; },
    setBanks: (next: typeof banks) => { banks = next; },
    setWallet: (next: typeof walletData) => { walletData = next; },
    setDelay: (milliseconds: number) => { responseDelay = milliseconds; },
  };
}

test('teacher payout route has its own hierarchy and fits the approved reference at three widths', async ({ page }) => {
  await installPayoutFixtures(page);
  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  await page.goto('/teacher/payouts');

  if (variant === 'before') {
    await expect(page.getByRole('heading', { name: 'Ví và thu nhập gia sư' })).toBeVisible();
  } else {
    await expect(page.getByRole('heading', { name: 'Yêu cầu rút tiền' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Ví và thu nhập gia sư' })).toHaveCount(0);
    await expect(page.getByRole('heading', { name: 'Lịch sử yêu cầu' })).toBeVisible();
    await expect(page.getByText('1.500.000 ₫', { exact: true })).toBeVisible();
    await expect(page.getByText('500.000 ₫', { exact: true })).toBeVisible();
    await expect(page.getByText('200.000 ₫', { exact: true })).toBeVisible();
    await expect(page.getByText('1.200.000 ₫', { exact: true })).toHaveCount(0);
    await expect(page.getByRole('table').getByText('Chờ duyệt')).toBeVisible();
    await expect(page.getByRole('main')).toHaveCount(1);
    const requestButton = page.getByRole('button', { name: 'Tạo yêu cầu rút tiền' });
    await requestButton.focus();
    await expect(requestButton).toBeFocused();
    await page.keyboard.press('Enter');
    await expect(page.getByRole('dialog', { name: 'Tạo yêu cầu rút tiền' })).toBeVisible();
    await page.keyboard.press('Escape');
    await expect(page.getByRole('dialog', { name: 'Tạo yêu cầu rút tiền' })).toBeHidden();
    await requestButton.evaluate(button => button.blur());
  }

  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    if (variant === 'after') {
      const size = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
      expect(size.content).toBeLessThanOrEqual(size.viewport + 1);
    }
    await page.screenshot({ path: resolve(evidence, `teacher-payouts-${variant}-${width}.png`), fullPage: true });
  }
});

test('payout submits the existing contract and preserves an idempotent retry', async ({ page }) => {
  test.skip(variant === 'before', 'The before capture records the previous route UI only.');
  const fixture = await installPayoutFixtures(page);
  fixture.setStatus({ create: 503 });
  await page.goto('/teacher/payouts');
  await page.getByRole('button', { name: 'Tạo yêu cầu rút tiền' }).click();
  const dialog = page.getByRole('dialog', { name: 'Tạo yêu cầu rút tiền' });
  await expect(dialog).toBeVisible();
  await dialog.getByRole('button', { name: 'Xác nhận rút tiền' }).click();
  await expect(page.getByText('Vui lòng nhập số tiền muốn rút')).toBeVisible();
  await dialog.getByLabel('Số tiền muốn rút (VND)').fill('200000');
  await dialog.getByLabel('Ghi chú cho ban quản trị (không bắt buộc)').fill('Chuyển theo kỳ học');
  await dialog.getByRole('button', { name: 'Xác nhận rút tiền' }).click();
  await expect(page.getByText('Tạm thời không thể tạo yêu cầu rút tiền')).toBeVisible();
  await expect(dialog).toBeVisible();
  await expect(dialog.getByLabel('Số tiền muốn rút (VND)')).toHaveValue('200.000');
  fixture.setStatus({ create: 201 });
  await dialog.getByRole('button', { name: 'Xác nhận rút tiền' }).click();
  await expect(dialog).toBeHidden();
  await expect(page.getByText('Tạo yêu cầu rút tiền thành công. Ban quản trị sẽ sớm duyệt và chuyển khoản.')).toBeVisible();

  const posts = fixture.requests.filter(request => request.method === 'POST' && request.path === '/api/teacher/payout-requests');
  expect(posts).toHaveLength(2);
  expect(JSON.parse(posts[0].body ?? '{}')).toEqual({ bankAccountId: bankAccount.id, amountVnd: 200000, teacherNote: 'Chuyển theo kỳ học', walletVersion: wallet.version });
  expect(posts[1].body).toBe(posts[0].body);
  expect(posts[0].headers['idempotency-key']).toMatch(/^[0-9a-f-]{36}$/i);
  expect(posts[1].headers['idempotency-key']).toBe(posts[0].headers['idempotency-key']);
  await expect(page.getByRole('table').getByText('#payout-c')).toBeVisible();
  expect(fixture.requests.filter(request => request.method === 'GET').map(request => request.path)).toEqual(expect.arrayContaining([
    '/api/teacher/wallet', '/api/teacher/bank-accounts', '/api/teacher/payout-requests',
  ]));
  expect(fixture.requests.some(request => request.path.includes('/api/admin/'))).toBe(false);
});

test('payout wallet, bank and history loading/error/empty states recover independently', async ({ page }) => {
  test.skip(variant === 'before', 'The before capture records the previous route UI only.');
  test.setTimeout(45000);
  const fixture = await installPayoutFixtures(page);
  fixture.setStatus({ wallet: 503, bank: 503, payouts: 503 });
  fixture.setDelay(500);
  await page.goto('/teacher/payouts');
  await expect(page.getByRole('status', { name: 'Đang tải số dư khả dụng' })).toBeVisible();
  await expect(page.getByRole('status', { name: 'Đang tải lịch sử rút tiền' })).toBeVisible();
  await expect(page.getByRole('status').filter({ hasText: 'Đang tải tài khoản nhận tiền' })).toBeVisible();
  await expect(page.getByText('Chưa tải được số dư khả dụng')).toBeVisible({ timeout: 15000 });
  await expect(page.getByText('Chưa tải được tài khoản nhận tiền')).toBeVisible({ timeout: 15000 });
  await expect(page.getByText('Chưa tải được lịch sử rút tiền')).toBeVisible({ timeout: 15000 });
  await expect(page.getByText('1.500.000 ₫', { exact: true })).toHaveCount(0);

  fixture.setDelay(0);
  fixture.setStatus({ wallet: 200 });
  await page.getByRole('button', { name: 'Thử tải số dư' }).click();
  await expect(page.getByText('1.500.000 ₫', { exact: true })).toBeVisible();
  fixture.setStatus({ bank: 200 });
  fixture.setBanks([]);
  await page.getByRole('button', { name: 'Thử tải lại tài khoản' }).click();
  await expect(page.getByRole('link', { name: 'Thêm tài khoản ngân hàng' })).toHaveAttribute('href', '/teacher/bank-accounts');
  fixture.setStatus({ payouts: 200 });
  fixture.setPayouts([]);
  await page.getByRole('button', { name: 'Thử lại lịch sử' }).click();
  await expect(page.getByText('Chưa có yêu cầu rút tiền')).toBeVisible();

  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    const size = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    expect(size.content).toBeLessThanOrEqual(size.viewport + 1);
  }
});
