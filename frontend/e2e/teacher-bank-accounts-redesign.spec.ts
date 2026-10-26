import { expect, test, type Page } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const teacher = { id: 'bank-teacher', email: 'bank@example.test', fullName: 'Trần Thu Hà', role: 'TEACHER', status: 'APPROVED' };
const firstAccount = { id: 'bank-1', bankBin: '970436', bankName: 'Vietcombank', accountNumberMasked: '****6789', accountHolderName: 'TRAN THU HA', isVerified: true, isDefault: true, createdAt: '2026-10-01T08:30:00Z', version: 6 };
const secondAccount = { id: 'bank-2', bankBin: '970422', bankName: 'MB Bank', accountNumberMasked: '****1234', accountHolderName: 'NGUYEN VAN A', isVerified: false, isDefault: false, createdAt: '2026-09-20T08:30:00Z', version: 2 };
const wallet = { id: 'wallet-1', teacherId: teacher.id, pendingBalanceVnd: 500000, availableBalanceVnd: 1500000, reservedBalanceVnd: 200000, heldBalanceVnd: 1200000, version: 4 };
const ledger = [
  { id: 'ledger-1', walletId: wallet.id, entryType: 'SETTLEMENT_RELEASE_AVAILABLE', amountVnd: 450000, balanceBucket: 'AVAILABLE', direction: 'CREDIT', description: 'Quyết toán buổi học', createdAt: '2026-10-16T08:30:00Z' },
];
const envelope = (data: unknown, meta: unknown = null) => ({ success: true, message: 'ok', data, errors: null, meta });

async function installFixtures(page: Page) {
  const requests: Array<{ method: string; path: string; body: string | null; headers: Record<string, string> }> = [];
  let bankStatus = 200;
  let accounts = [firstAccount, secondAccount];
  let responseDelay = 0;
  await page.addInitScript(user => {
    localStorage.setItem('tutor-match.session', JSON.stringify({ user, remember: false, sessionId: 'teacher-bank-session' }));
    document.cookie = 'accessToken=teacher-bank-access; path=/; SameSite=Lax';
  }, teacher);
  await page.route('**/api/teacher/wallet', async route => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(wallet)) });
  });
  await page.route('**/api/teacher/wallet/ledger**', async route => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(ledger, { page: 0, size: 20, totalElements: 1, totalPages: 1, hasNext: false })) });
  });
  await page.route('**/api/teacher/payout-requests**', async route => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope([], { page: 0, size: 20, totalElements: 0, totalPages: 0, hasNext: false })) });
  });
  await page.route('**/api/teacher/bank-accounts**', async route => {
    const request = route.request();
    const url = new URL(request.url());
    requests.push({ method: request.method(), path: url.pathname, body: request.postData(), headers: await request.allHeaders() });
    if (request.method() === 'GET') {
      if (responseDelay) await new Promise(resolveDelay => setTimeout(resolveDelay, responseDelay));
      await route.fulfill({
        status: bankStatus,
        contentType: 'application/json',
        body: JSON.stringify(bankStatus === 200 ? envelope(accounts) : { success: false, message: 'unavailable', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null }),
      });
      return;
    }
    if (request.method() === 'POST') {
      const body = JSON.parse(request.postData() || '{}');
      const created = { ...secondAccount, ...body, id: 'bank-created', accountNumberMasked: '****4321', version: 0 };
      accounts = [...accounts, created];
      await route.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify(envelope(created)) });
      return;
    }
    if (request.method() === 'PUT') {
      const body = JSON.parse(request.postData() || '{}');
      const updated = { ...firstAccount, ...body, id: firstAccount.id, accountNumberMasked: '****7654', version: 7 };
      accounts = accounts.map(account => account.id === firstAccount.id ? updated : account);
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(updated)) });
      return;
    }
    accounts = accounts.filter(account => account.id !== url.pathname.split('/').at(-1));
    await route.fulfill({ status: 204, body: '' });
  });
  return {
    requests,
    setBankStatus: (status: number) => { bankStatus = status; },
    setAccounts: (next: typeof accounts) => { accounts = next; },
    setResponseDelay: (milliseconds: number) => { responseDelay = milliseconds; },
  };
}

test('bank account route uses its own screen and responsive account list', async ({ page }) => {
  await installFixtures(page);
  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  await page.goto('/teacher/bank-accounts');

  if (variant === 'before') {
    await expect(page.getByRole('heading', { name: 'Ví và thu nhập gia sư' })).toBeVisible();
  } else {
    await expect(page.getByRole('heading', { name: 'Tài khoản ngân hàng' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Tài khoản nhận tiền' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Ví và thu nhập gia sư' })).toHaveCount(0);
    await expect(page.getByRole('main')).toHaveCount(1);
    await expect(page.getByText('****6789')).toBeVisible();
    await expect(page.getByText('****1234')).toBeVisible();
    await expect(page.locator('body')).not.toContainText('012345678901');
    const addAccount = page.getByRole('button', { name: 'Thêm tài khoản' });
    await addAccount.focus();
    await expect(addAccount).toBeFocused();
    await page.keyboard.press('Enter');
    await expect(page.getByRole('dialog', { name: 'Thêm tài khoản ngân hàng' })).toBeVisible();
    await page.keyboard.press('Escape');
    await expect(page.getByRole('dialog', { name: 'Thêm tài khoản ngân hàng' })).toBeHidden();
  }

  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    if (variant === 'after') {
      const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
      expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    }
    await page.screenshot({ path: resolve(evidence, `teacher-bank-accounts-${variant}-${width}.png`), fullPage: true });
  }
});

test('bank account actions keep validation, masking, versions and idempotency', async ({ page }) => {
  test.skip(variant === 'before', 'The before capture records the old route UI only.');
  const fixture = await installFixtures(page);
  await page.goto('/teacher/bank-accounts');
  await expect(page.getByRole('heading', { name: 'Tài khoản ngân hàng' })).toBeVisible();

  await page.getByRole('button', { name: 'Thêm tài khoản' }).click();
  const addDialog = page.getByRole('dialog', { name: 'Thêm tài khoản ngân hàng' });
  await addDialog.getByRole('button', { name: 'Lưu tài khoản' }).click();
  await expect(page.getByText('Vui lòng chọn ngân hàng')).toBeVisible();
  const bankSelect = addDialog.getByRole('combobox', { name: /Ngân hàng/ });
  await bankSelect.click();
  await bankSelect.press('Home');
  await bankSelect.press('Enter');
  await addDialog.getByLabel('Số tài khoản').fill('012345678901');
  await addDialog.getByLabel('Tên chủ tài khoản (viết hoa không dấu)').fill('nguyen van a');
  await addDialog.getByRole('button', { name: 'Lưu tài khoản' }).click();
  const bankPost = fixture.requests.find(request => request.method === 'POST');
  expect(JSON.parse(bankPost?.body ?? '{}')).toEqual({ bankBin: '970422', bankName: 'MB Bank (Ngân hàng Quân đội)', accountNumber: '012345678901', accountHolderName: 'NGUYEN VAN A', isDefault: false, version: 0 });
  expect(bankPost?.headers['idempotency-key']).toMatch(/^[0-9a-f-]{36}$/i);
  await expect(page.getByText('****4321')).toBeVisible();
  await expect(page.getByText('Thêm tài khoản ngân hàng thành công')).toBeVisible();
  await expect(page.locator('body')).not.toContainText('012345678901');

  await page.getByRole('button', { name: 'Sửa tài khoản Vietcombank' }).click();
  const editDialog = page.getByRole('dialog', { name: 'Chỉnh sửa tài khoản ngân hàng' });
  await editDialog.getByLabel('Số tài khoản').fill('012345678902');
  await editDialog.getByRole('button', { name: 'Lưu tài khoản' }).click();
  const bankPut = fixture.requests.find(request => request.method === 'PUT');
  expect(JSON.parse(bankPut?.body ?? '{}')).toEqual({ bankBin: '970436', bankName: 'Vietcombank', accountNumber: '012345678902', accountHolderName: 'TRAN THU HA', isDefault: true, version: 6 });
  await expect(page.getByText('****7654')).toBeVisible();

  await page.getByRole('button', { name: 'Xóa tài khoản Vietcombank' }).click();
  await expect(page.getByText('Xác nhận xóa tài khoản này?')).toBeVisible();
  await page.getByRole('button', { name: 'Xóa', exact: true }).last().click();
  const bankDelete = fixture.requests.find(request => request.method === 'DELETE');
  expect(bankDelete?.headers['if-match']).toBe('"7"');
  await expect(page.getByText('****7654')).toHaveCount(0);
});

test('bank accounts separate loading, error, retry and empty states', async ({ page }) => {
  test.skip(variant === 'before', 'The before capture records the old route UI only.');
  const fixture = await installFixtures(page);
  fixture.setBankStatus(503);
  fixture.setResponseDelay(1000);
  await page.goto('/teacher/bank-accounts');
  await expect(page.getByRole('status', { name: 'Đang tải tài khoản ngân hàng' })).toBeVisible();
  await expect(page.getByText('Chưa tải được tài khoản ngân hàng')).toBeVisible({ timeout: 15000 });
  await expect(page.getByText('Bạn chưa liên kết tài khoản ngân hàng nào')).toHaveCount(0);
  fixture.setBankStatus(200);
  fixture.setResponseDelay(0);
  await page.getByRole('button', { name: 'Thử lại tài khoản' }).click();
  await expect(page.getByText('****6789')).toBeVisible();

  fixture.setAccounts([]);
  await page.reload();
  await expect(page.getByText('Bạn chưa liên kết tài khoản ngân hàng nào')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Thêm tài khoản' })).toBeVisible();
});
