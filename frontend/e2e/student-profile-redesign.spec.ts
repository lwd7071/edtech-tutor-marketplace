import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const student = { id: 'profile-student', email: 'student@example.test', fullName: 'Nguyễn Minh An', role: 'STUDENT', status: 'ACTIVE', avatarUrl: null };
type ParentContactFixture = { parentFullName: string | null; parentPhone: string | null; parentEmail: string | null; notifyParent: boolean; updatedAt: string };
const contact: ParentContactFixture = { parentFullName: 'Nguyễn Văn Bình', parentPhone: '0901234567', parentEmail: 'parent@example.test', notifyParent: true, updatedAt: '2026-10-15T08:30:00Z' };
const envelope = (data: unknown) => ({ success: true, message: 'ok', data, errors: null, meta: null });
const rejected = { success: false, message: 'unavailable', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null };

test('student profile redesign preserves account identity and parent-contact contract', async ({ page }) => {
  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  let getStatus = 200;
  let putStatus = 200;
  let contactResponse: ParentContactFixture | null = contact;
  let holdInitialGet = true;
  let releaseInitialGet!: () => void;
  const updates: unknown[] = [];
  await page.route('**/api/auth/login', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope({ accessToken: 'visual-profile-access', refreshToken: 'visual-profile-refresh', user: student })) }));
  await page.route('**/api/student/parent-contact', async route => {
    if (route.request().method() === 'GET') {
      if (holdInitialGet) {
        holdInitialGet = false;
        await new Promise<void>(resolveGet => { releaseInitialGet = resolveGet; });
      }
      return route.fulfill({ status: getStatus, contentType: 'application/json', body: JSON.stringify(getStatus === 200 ? envelope(contactResponse) : rejected) });
    }
    const body = route.request().postDataJSON();
    updates.push(body);
    return route.fulfill({ status: putStatus, contentType: 'application/json', body: JSON.stringify(putStatus === 200 ? envelope({ ...body, updatedAt: '2026-10-16T08:30:00Z' }) : rejected) });
  });

  await page.goto('/auth/login');
  await page.getByPlaceholder('Nhập email của bạn').fill(student.email);
  await page.getByPlaceholder('Nhập mật khẩu').fill('Password123!');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page).toHaveURL(/\/student$/);
  await page.goto('/student/profile');
  await expect(page.getByRole('status', { name: 'Đang tải thông tin phụ huynh' })).toBeVisible();
  releaseInitialGet();
  await expect(page.getByRole('heading', { name: 'Hồ sơ cá nhân' })).toBeVisible();
  await expect(page.getByRole('heading', { name: student.fullName })).toBeVisible();
  await expect(page.getByText(student.email, { exact: true })).toBeVisible();
  await expect(page.getByRole('textbox', { name: 'Email', exact: true })).toHaveCount(0);
  await expect(page.getByRole('textbox', { name: 'Email phụ huynh' })).toHaveValue('parent@example.test');
  await expect(page.getByRole('switch', { name: 'Gửi thông báo qua email phụ huynh' })).toBeChecked();

  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    if (variant === 'after') {
      const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
      expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    }
    await page.screenshot({ path: resolve(evidence, `student-profile-${variant}-${width}.png`), fullPage: true });
  }
  if (variant === 'before') return;

  await page.setViewportSize({ width: 1280, height: 900 });
  await page.getByLabel('Họ và tên phụ huynh').fill('A');
  await page.getByRole('button', { name: 'Lưu thông tin' }).click();
  await expect(page.getByText('Tên quá ngắn')).toBeVisible();
  await page.getByLabel('Họ và tên phụ huynh').fill('Nguyễn Văn Bình');
  await page.getByLabel('Số điện thoại phụ huynh').fill('123');
  await page.getByRole('button', { name: 'Lưu thông tin' }).click();
  await expect(page.getByText('Số điện thoại không hợp lệ')).toBeVisible();
  await page.getByLabel('Số điện thoại phụ huynh').fill('0901234567');
  await page.getByRole('textbox', { name: 'Email phụ huynh' }).fill('invalid-email');
  await page.getByRole('button', { name: 'Lưu thông tin' }).click();
  await expect(page.getByText('Email không hợp lệ')).toBeVisible();

  await page.getByRole('textbox', { name: 'Email phụ huynh' }).fill('parent@example.test');
  await page.getByLabel('Họ và tên phụ huynh').fill('  Nguyễn Thị Hoa  ');
  putStatus = 503;
  await page.getByRole('button', { name: 'Lưu thông tin' }).click();
  await expect(page.getByText('Không thể cập nhật thông tin phụ huynh. Vui lòng thử lại.')).toBeVisible();
  expect(updates.at(-1)).toEqual({ parentFullName: 'Nguyễn Thị Hoa', parentPhone: '0901234567', parentEmail: 'parent@example.test', notifyParent: true });
  putStatus = 200;
  await page.getByRole('button', { name: 'Lưu thông tin' }).click();
  await expect(page.getByText('Cập nhật thông tin phụ huynh thành công!')).toBeVisible();

  await page.getByRole('textbox', { name: 'Email phụ huynh' }).fill('');
  await expect(page.getByRole('switch', { name: 'Gửi thông báo qua email phụ huynh' })).toBeDisabled();
  await page.getByRole('button', { name: 'Lưu thông tin' }).click();
  await expect.poll(() => updates.at(-1)).toEqual({ parentFullName: 'Nguyễn Thị Hoa', parentPhone: '0901234567', parentEmail: null, notifyParent: false });
  await expect(page.getByRole('switch', { name: 'Gửi thông báo qua email phụ huynh' })).not.toBeChecked();
  await expect(page.getByRole('heading', { name: student.fullName })).toBeVisible();

  getStatus = 503;
  await page.reload();
  await expect(page.getByText('Chưa tải được thông tin phụ huynh')).toBeVisible();
  getStatus = 200;
  contactResponse = null;
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByLabel('Họ và tên phụ huynh')).toHaveValue('');
  await expect(page.getByRole('switch', { name: 'Gửi thông báo qua email phụ huynh' })).toBeDisabled();
  await page.setViewportSize({ width: 375, height: 812 });
  const saveButton = page.getByRole('button', { name: 'Lưu thông tin' });
  await saveButton.scrollIntoViewIfNeeded();
  const saveBottom = await saveButton.evaluate(element => element.getBoundingClientRect().bottom);
  const bottomNavigationTop = await page.getByRole('navigation', { name: 'Điều hướng nhanh' }).evaluate(element => element.getBoundingClientRect().top);
  expect(saveBottom).toBeLessThanOrEqual(bottomNavigationTop);
  await page.keyboard.press('Tab');
  expect(await page.evaluate(() => document.activeElement?.matches(':focus-visible') ?? false)).toBe(true);
});
