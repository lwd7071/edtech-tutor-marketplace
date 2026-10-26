import { expect, test, type Page } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const teacher = { id: 'wallet-teacher', email: 'wallet@example.test', fullName: 'Trần Thu Hà', role: 'TEACHER', status: 'APPROVED' };
const wallet = { id: 'wallet-1', teacherId: teacher.id, pendingBalanceVnd: 500000, availableBalanceVnd: 1500000, reservedBalanceVnd: 200000, heldBalanceVnd: 1200000, version: 4 };
const ledger = [
  { id: 'ledger-credit', walletId: wallet.id, entryType: 'SETTLEMENT_RELEASE_AVAILABLE', amountVnd: 450000, balanceBucket: 'AVAILABLE', direction: 'CREDIT', referenceType: 'BOOKING', referenceId: 'booking-1', description: 'Quyết toán buổi học', createdAt: '2026-10-16T08:30:00Z' },
  { id: 'ledger-payout', walletId: wallet.id, entryType: 'PAYOUT_RESERVED', amountVnd: 200000, balanceBucket: 'RESERVED', direction: 'DEBIT', referenceType: 'PAYOUT', referenceId: 'payout-1', description: 'Đang chờ duyệt', createdAt: '2026-10-15T08:30:00Z' },
];
const bankAccount = { id: 'bank-1', bankBin: '970422', bankName: 'MB Bank', accountNumberMasked: '****6789', accountHolderName: 'TRAN THU HA', isVerified: true, isDefault: true, createdAt: '2026-10-01T08:30:00Z', version: 3 };
const payout = { id: 'payout-1', teacherId: teacher.id, walletId: wallet.id, bankAccountId: bankAccount.id, amountVnd: 350000, status: 'PENDING', teacherNote: 'Rút về tài khoản mặc định', adminNote: null, bankReference: null, proofUrl: null, version: 0, createdAt: '2026-10-16T08:30:00Z' };
const envelope = (data: unknown, meta: unknown = null) => ({ success: true, message: 'ok', data, errors: null, meta });

async function installFinanceFixtures(page: Page) {
  const requests: Array<{ method: string; path: string; query: URLSearchParams; body: string | null; headers: Record<string, string> }> = [];
  let walletStatus = 200;
  let ledgerStatus = 200;
  let payoutsStatus = 200;
  let bankStatus = 200;
  let ledgerEntries = [...ledger];
  let payouts = [payout];
  let bankAccounts = [bankAccount];
  let responseDelayMs = 0;
  const delayResponse = async () => {
    if (responseDelayMs) await new Promise(resolveDelay => setTimeout(resolveDelay, responseDelayMs));
  };

  await page.addInitScript(user => {
    localStorage.setItem('tutor-match.session', JSON.stringify({ user, remember: false, sessionId: 'teacher-wallet-session' }));
    document.cookie = 'accessToken=teacher-wallet-access; path=/; SameSite=Lax';
  }, teacher);
  await page.route('**/api/teacher/wallet', async route => {
    requests.push({ method: route.request().method(), path: new URL(route.request().url()).pathname, query: new URLSearchParams(), body: route.request().postData(), headers: await route.request().allHeaders() });
    await delayResponse();
    await route.fulfill({ status: walletStatus, contentType: 'application/json', body: JSON.stringify(walletStatus === 200 ? envelope(wallet) : { success: false, message: 'unavailable', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null }) });
  });
  await page.route('**/api/teacher/wallet/ledger**', async route => {
    const url = new URL(route.request().url());
    requests.push({ method: route.request().method(), path: url.pathname, query: url.searchParams, body: route.request().postData(), headers: await route.request().allHeaders() });
    await delayResponse();
    await route.fulfill({ status: ledgerStatus, contentType: 'application/json', body: JSON.stringify(ledgerStatus === 200 ? envelope(ledgerEntries, { page: Number(url.searchParams.get('page') || 0), size: Number(url.searchParams.get('size') || 20), totalElements: ledgerEntries.length ? 21 : 0, totalPages: ledgerEntries.length ? 2 : 0, hasNext: Number(url.searchParams.get('page') || 0) === 0 }) : { success: false, message: 'unavailable', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null }) });
  });
  await page.route('**/api/teacher/payout-requests**', async route => {
    const url = new URL(route.request().url());
    requests.push({ method: route.request().method(), path: url.pathname, query: url.searchParams, body: route.request().postData(), headers: await route.request().allHeaders() });
    if (route.request().method() === 'GET') {
      await delayResponse();
      await route.fulfill({ status: payoutsStatus, contentType: 'application/json', body: JSON.stringify(payoutsStatus === 200 ? envelope(payouts, { page: Number(url.searchParams.get('page') || 0), size: Number(url.searchParams.get('size') || 20), totalElements: payouts.length, totalPages: payouts.length ? 1 : 0, hasNext: false }) : { success: false, message: 'unavailable', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null }) });
      return;
    }
    const created = { ...payout, id: 'payout-created', bankAccountId: JSON.parse(route.request().postData() || '{}').bankAccountId, amountVnd: JSON.parse(route.request().postData() || '{}').amountVnd, status: 'PENDING' };
    payouts = [created, ...payouts];
    await route.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify(envelope(created)) });
  });
  await page.route('**/api/teacher/bank-accounts**', async route => {
    const url = new URL(route.request().url());
    requests.push({ method: route.request().method(), path: url.pathname, query: url.searchParams, body: route.request().postData(), headers: await route.request().allHeaders() });
    if (route.request().method() === 'GET') {
      await delayResponse();
      await route.fulfill({ status: bankStatus, contentType: 'application/json', body: JSON.stringify(bankStatus === 200 ? envelope(bankAccounts) : { success: false, message: 'unavailable', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null }) });
      return;
    }
    if (route.request().method() === 'POST') {
      const body = JSON.parse(route.request().postData() || '{}');
      const created = { ...bankAccount, ...body, id: 'bank-created', accountNumberMasked: '****4321', version: 0 };
      bankAccounts = [...bankAccounts, created];
      await route.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify(envelope(created)) });
      return;
    }
    if (route.request().method() === 'PUT') {
      const body = JSON.parse(route.request().postData() || '{}');
      const updated = { ...bankAccount, ...body, id: 'bank-1', accountNumberMasked: '****6789', version: 4 };
      bankAccounts = bankAccounts.map(account => account.id === 'bank-1' ? updated : account);
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(updated)) });
      return;
    }
    bankAccounts = bankAccounts.filter(account => account.id !== url.pathname.split('/').at(-1));
    await route.fulfill({ status: 204, body: '' });
  });
  return {
    requests,
    setStatuses: (next: Partial<{ wallet: number; ledger: number; payouts: number; bank: number }>) => {
      if (next.wallet !== undefined) walletStatus = next.wallet;
      if (next.ledger !== undefined) ledgerStatus = next.ledger;
      if (next.payouts !== undefined) payoutsStatus = next.payouts;
      if (next.bank !== undefined) bankStatus = next.bank;
    },
    setLedger: (entries: typeof ledger) => { ledgerEntries = entries; },
    setPayouts: (next: typeof payouts) => { payouts = next; },
    setBankAccounts: (accounts: typeof bankAccounts) => { bankAccounts = accounts; },
    setDelay: (milliseconds: number) => { responseDelayMs = milliseconds; },
  };
}

async function openWallet(page: Page) {
  await page.goto('/teacher/wallet');
  await expect(page.getByRole('heading', { name: 'Ví và thu nhập gia sư' })).toBeVisible();
  await expect(page.getByText('1.500.000 ₫', { exact: true })).toBeVisible();
}

test('teacher wallet balances and ledger keep finance meaning across responsive sizes', async ({ page }) => {
  const fixture = await installFinanceFixtures(page);
  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  await openWallet(page);
  expect(fixture.requests).toEqual(expect.arrayContaining([
    expect.objectContaining({ method: 'GET', path: '/api/teacher/wallet' }),
    expect.objectContaining({ method: 'GET', path: '/api/teacher/wallet/ledger' }),
    expect.objectContaining({ method: 'GET', path: '/api/teacher/payout-requests' }),
    expect.objectContaining({ method: 'GET', path: '/api/teacher/bank-accounts' }),
  ]));
  if (variant === 'after') {
    await expect(page.getByText('1.200.000 ₫', { exact: true })).toBeVisible();
    await expect(page.getByText('Tiền buổi học đang giữ')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Yêu cầu rút tiền' })).toBeEnabled();
    await expect(page.getByRole('table').getByText('Quyết toán buổi học').first()).toBeVisible();
  }
  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    if (variant === 'after') {
      await expect(page.getByRole('tab', { name: 'Sổ cái giao dịch' })).toBeVisible();
      await expect(page.getByRole('tab', { name: /Lịch sử rút tiền/ })).toBeVisible();
      await expect(page.getByRole('tab', { name: /Tài khoản ngân hàng/ })).toBeVisible();
    }
    if (variant === 'after') {
      const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
      expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
      if (width === 375 || width === 768) {
        const firstEntry = await page.getByRole('list', { name: 'Giao dịch' }).getByRole('listitem').first().boundingBox();
        const bottomNavigation = await page.getByRole('navigation', { name: 'Điều hướng nhanh' }).boundingBox();
        expect(firstEntry).not.toBeNull();
        expect(bottomNavigation).not.toBeNull();
        expect(firstEntry!.y + firstEntry!.height).toBeLessThanOrEqual(bottomNavigation!.y);
      }
    }
    await page.screenshot({ path: resolve(evidence, `teacher-wallet-${variant}-${width}.png`), fullPage: true });
    if (variant === 'after' && width === 768) {
      await page.evaluate(() => window.scrollTo(0, document.documentElement.scrollHeight));
      const pageTwo = await page.getByRole('listitem', { name: '2', exact: true }).boundingBox();
      const bottomNavigation = await page.getByRole('navigation', { name: 'Điều hướng nhanh' }).boundingBox();
      expect(pageTwo).not.toBeNull();
      expect(bottomNavigation).not.toBeNull();
      expect(pageTwo!.y + pageTwo!.height).toBeLessThanOrEqual(bottomNavigation!.y);
      await page.evaluate(() => window.scrollTo(0, 0));
    }
  }
  if (variant === 'before') return;

  await page.setViewportSize({ width: 1280, height: 900 });
  const ledgerTab = page.getByRole('tab', { name: 'Sổ cái giao dịch' });
  await ledgerTab.focus();
  await page.keyboard.press('ArrowRight');
  const payoutsTab = page.getByRole('tab', { name: /Lịch sử rút tiền/ });
  await expect(payoutsTab).toHaveAttribute('aria-selected', 'true');
  await expect(payoutsTab).toBeFocused();
  await page.keyboard.press('ArrowRight');
  const banksTab = page.getByRole('tab', { name: /Tài khoản ngân hàng/ });
  await expect(banksTab).toHaveAttribute('aria-selected', 'true');
  await page.keyboard.press('ArrowLeft');
  await expect(payoutsTab).toHaveAttribute('aria-selected', 'true');
  await ledgerTab.click();
  await page.getByRole('listitem', { name: '2', exact: true }).click();
  await expect.poll(() => fixture.requests.filter(request => request.path === '/api/teacher/wallet/ledger').at(-1)?.query.get('page')).toBe('1');
  await page.getByRole('tab', { name: /Lịch sử rút tiền/ }).click();
  await expect(page.getByRole('tabpanel', { name: /Lịch sử rút tiền/ }).getByText('350.000 ₫', { exact: true }).first()).toBeVisible();
  await page.getByRole('tab', { name: /Tài khoản ngân hàng/ }).click();
  await expect(page.getByText('****6789')).toBeVisible();
});

test('wallet and finance tabs announce loading while their requests settle', async ({ page }) => {
  const fixture = await installFinanceFixtures(page);
  fixture.setDelay(2500);
  await page.goto('/teacher/wallet');
  await expect(page.getByRole('status', { name: 'Đang tải số dư ví' })).toBeVisible();
  await expect(page.getByRole('status', { name: 'Đang tải sổ cái' })).toBeVisible();

  await page.getByRole('tab', { name: /Tài khoản ngân hàng/ }).click();
  await expect(page.getByRole('status', { name: 'Đang tải tài khoản ngân hàng' })).toBeVisible();
  await page.getByRole('tab', { name: /Lịch sử rút tiền/ }).click();
  await expect(page.getByRole('status', { name: 'Đang tải lịch sử rút tiền' })).toBeVisible();

  await expect(page.getByText('350.000 ₫', { exact: true }).first()).toBeVisible({ timeout: 5000 });
  await expect(page.getByText('1.500.000 ₫', { exact: true })).toBeVisible();
});

test('teacher wallet payout and bank actions preserve payloads, versions and idempotency', async ({ page }) => {
  const fixture = await installFinanceFixtures(page);
  await openWallet(page);
  await page.getByRole('button', { name: 'Yêu cầu rút tiền' }).click();
  await expect(page.getByRole('dialog', { name: 'Tạo yêu cầu rút tiền' })).toBeVisible();
  await page.getByLabel('Số tiền muốn rút (VND)').fill('100000');
  await page.getByRole('button', { name: 'Xác nhận rút tiền' }).click();
  const payoutPost = fixture.requests.find(request => request.method === 'POST' && request.path === '/api/teacher/payout-requests');
  expect(JSON.parse(payoutPost?.body ?? '{}')).toEqual({ bankAccountId: bankAccount.id, amountVnd: 100000, walletVersion: wallet.version });
  expect(payoutPost?.headers['idempotency-key']).toMatch(/^[0-9a-f-]{36}$/i);
  await expect(page.getByText(/Tạo yêu cầu rút tiền thành công/)).toBeVisible();

  await page.getByRole('tab', { name: /Tài khoản ngân hàng/ }).click();
  await page.getByRole('button', { name: 'Thêm tài khoản' }).click();
  const addBankDialog = page.getByRole('dialog', { name: 'Thêm tài khoản ngân hàng' });
  const bankSelect = addBankDialog.getByRole('combobox', { name: /Ngân hàng/ });
  await bankSelect.click();
  await bankSelect.press('Home');
  await bankSelect.press('Enter');
  await page.getByLabel('Số tài khoản').fill('012345678901');
  await page.getByLabel('Tên chủ tài khoản (viết hoa không dấu)').fill('nguyen van a');
  await page.getByRole('button', { name: 'Lưu tài khoản' }).click();
  const bankPost = fixture.requests.find(request => request.method === 'POST' && request.path === '/api/teacher/bank-accounts');
  expect(JSON.parse(bankPost?.body ?? '{}')).toEqual({ bankBin: '970422', bankName: 'MB Bank (Ngân hàng Quân đội)', accountNumber: '012345678901', accountHolderName: 'NGUYEN VAN A', isDefault: false, version: 0 });
  expect(bankPost?.headers['idempotency-key']).toMatch(/^[0-9a-f-]{36}$/i);

  await page.getByRole('button', { name: 'Sửa tài khoản MB Bank' }).first().click();
  const editBankDialog = page.getByRole('dialog', { name: 'Chỉnh sửa tài khoản ngân hàng' });
  await expect(editBankDialog).toBeVisible();
  await page.getByLabel('Số tài khoản').fill('012345678902');
  await page.getByRole('button', { name: 'Lưu tài khoản' }).click();
  const bankPut = fixture.requests.find(request => request.method === 'PUT' && request.path.endsWith('/bank-1'));
  expect(JSON.parse(bankPut?.body ?? '{}')).toEqual({ bankBin: '970422', bankName: 'MB Bank', accountNumber: '012345678902', accountHolderName: 'TRAN THU HA', isDefault: true, version: 3 });

  await page.getByRole('button', { name: 'Xóa' }).first().click();
  await expect(page.getByText('Xác nhận xóa tài khoản này?')).toBeVisible();
  await page.getByRole('button', { name: 'Xóa', exact: true }).last().click();
  const bankDelete = fixture.requests.find(request => request.method === 'DELETE' && request.path.endsWith('/bank-1'));
  expect(bankDelete?.headers['if-match']).toBe('"4"');
});

test('wallet and finance lists keep loading errors and empty responses separate', async ({ page }) => {
  const fixture = await installFinanceFixtures(page);
  fixture.setStatuses({ wallet: 503, ledger: 503, payouts: 503, bank: 503 });
  await page.goto('/teacher/wallet');
  await expect(page.getByText('Chưa tải được số dư ví')).toBeVisible();
  await expect(page.getByText('1.500.000 ₫', { exact: true })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Thử tải số dư' })).toBeVisible();
  fixture.setStatuses({ wallet: 200, ledger: 200, payouts: 200, bank: 200 });
  await page.getByRole('button', { name: 'Thử tải số dư' }).click();
  await expect(page.getByText('1.500.000 ₫', { exact: true })).toBeVisible();
  await page.getByRole('tab', { name: 'Sổ cái giao dịch' }).click();
  await expect(page.getByText('Chưa tải được sổ cái')).toBeVisible();
  await page.getByRole('button', { name: 'Thử lại sổ cái' }).click();
  await expect(page.getByRole('table').getByText('Quyết toán buổi học').first()).toBeVisible();
  fixture.setLedger([]);
  await page.reload();
  await page.getByRole('tab', { name: 'Sổ cái giao dịch' }).click();
  await expect(page.getByText('Chưa có giao dịch trong sổ cái')).toBeVisible();

  fixture.setStatuses({ payouts: 503 });
  await page.reload();
  await page.getByRole('tab', { name: /Lịch sử rút tiền/ }).click();
  await expect(page.getByText('Chưa tải được lịch sử rút tiền')).toBeVisible();
  fixture.setStatuses({ payouts: 200 });
  await page.getByRole('button', { name: 'Thử lại lịch sử' }).click();
  await expect(page.getByText('350.000 ₫', { exact: true }).first()).toBeVisible();
  fixture.setPayouts([]);
  await page.reload();
  await page.getByRole('tab', { name: /Lịch sử rút tiền/ }).click();
  await expect(page.getByText('Chưa có yêu cầu rút tiền')).toBeVisible();

  fixture.setStatuses({ bank: 503 });
  await page.reload();
  await page.getByRole('tab', { name: /Tài khoản ngân hàng/ }).click();
  await expect(page.getByText('Chưa tải được tài khoản ngân hàng')).toBeVisible();
  fixture.setStatuses({ bank: 200 });
  await page.getByRole('button', { name: 'Thử lại tài khoản' }).click();
  await expect(page.getByText('****6789')).toBeVisible();
  fixture.setBankAccounts([]);
  await page.reload();
  await page.getByRole('tab', { name: /Tài khoản ngân hàng/ }).click();
  await expect(page.getByText('Bạn chưa liên kết tài khoản ngân hàng nào')).toBeVisible();
});
