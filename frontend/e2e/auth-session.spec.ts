import { expect, Page, test } from '@playwright/test';

const users = {
  student: { id: 'student-1', email: 'student@example.test', fullName: 'Student', role: 'STUDENT', status: 'ACTIVE' },
  teacher: { id: 'teacher-1', email: 'teacher@example.test', fullName: 'Teacher', role: 'TEACHER', status: 'APPROVED' },
};

async function mockAuth(page: Page) {
  await page.route('**/api/auth/login', async (route) => {
    const body = route.request().postDataJSON() as { email: string };
    const user = body.email.startsWith('teacher') ? users.teacher : users.student;
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { accessToken: `${user.role}-access`, refreshToken: `${user.role}-refresh`, user } }) });
  });
  await page.route('**/api/auth/logout', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: null }) });
  });
}

async function login(page: Page, email: string) {
  await page.getByPlaceholder('Nhập email của bạn').fill(email);
  await page.getByPlaceholder('Nhập mật khẩu').fill('Password123!');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
}

test('same tab can switch from student to teacher without forbidden redirect', async ({ page }) => {
  await mockAuth(page);
  await page.goto('/auth/login');
  await login(page, users.student.email);
  await expect(page).toHaveURL(/\/student$/);

  await page.getByRole('button', { name: 'Đăng xuất' }).click();
  await expect(page).toHaveURL(/\/auth\/login$/);
  await login(page, users.teacher.email);
  await expect(page).toHaveURL(/\/teacher$/);
  await expect(page).not.toHaveURL(/forbidden/);
});

test('two pages in one browser context synchronize the account change', async ({ page, context }) => {
  const secondPage = await context.newPage();
  await mockAuth(page);
  await mockAuth(secondPage);

  await page.goto('/auth/login');
  await login(page, users.student.email);
  await expect(page).toHaveURL(/\/student$/);
  await secondPage.goto('/student');
  await expect(secondPage).toHaveURL(/\/student$/);

  await page.getByRole('button', { name: 'Đăng xuất' }).click();
  await expect(page).toHaveURL(/\/auth\/login$/);
  await expect(secondPage).toHaveURL(/\/auth\/login$/);

  await login(page, users.teacher.email);
  await expect(page).toHaveURL(/\/teacher$/);
  await expect(secondPage).toHaveURL(/\/auth\/login$/);
});
