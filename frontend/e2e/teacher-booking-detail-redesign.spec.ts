import { expect, test, type Page } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const teacher = { id: 'teacher-booking-detail-fixture', email: 'teacher@example.test', fullName: 'Trần Thu Hà', role: 'TEACHER', status: 'ACTIVE' };
const initialBooking = {
  id: 'booking-teacher-detail', teacher: { id: 'teacher-1', fullName: 'Trần Thu Hà' }, student: { id: 'student-1', fullName: 'Nguyễn Minh An' },
  studentPackageId: 'package-1', subject: { id: 'subject-math', name: 'Toán 11' }, startTime: '2026-10-20T10:00:00Z', endTime: '2026-10-20T11:00:00Z',
  deliveryMode: 'ONLINE', meetingLink: 'https://meet.example.test/session', status: 'SCHEDULED', trial: false, outsideAvailabilityWarning: true,
  version: 7, canDispute: false, canConfirm: false, cancelReason: 'Chỉ hiển thị khi buổi đã hủy.', settlementStatus: 'AWAITING_CONFIRMATION',
  settlement: { status: 'AWAITING_CONFIRMATION', teacherConfirmedAt: null, studentConfirmedAt: null, confirmationDeadline: '2026-10-21T11:00:00Z', version: 2 },
  sessionReport: { content: 'Ôn phương trình bậc hai\nLuyện thêm dạng có tham số.', feedback: 'Nắm kiến thức tốt.', followUpNote: 'Làm bài tập trang 42.', teacherSelfRating: 5 },
};
const envelope = (data: unknown) => ({ success: true, message: 'ok', data, errors: null, meta: null });

async function openPage(page: Page) {
  await page.addInitScript(user => {
    localStorage.setItem('tutor-match.session', JSON.stringify({ user, remember: false, sessionId: 'teacher-booking-detail-session' }));
    document.cookie = 'accessToken=teacher-booking-detail-access; path=/; SameSite=Lax';
  }, teacher);
  await page.goto('/teacher/bookings/booking-teacher-detail');
}

async function routeBooking(page: Page, state: { item?: Record<string, any>; mode?: 'success' | 'error' | 'empty'; hold?: boolean } = {}) {
  let item = state.item ?? { ...initialBooking };
  let mode = state.mode ?? 'success';
  let releaseGet: (() => void) | undefined;
  const gate = state.hold ? new Promise<void>(resolve => { releaseGet = resolve; }) : null;
  const requests: Array<{ method: string; url: string; body?: unknown }> = [];
  await page.route(/\/api\/teacher\/bookings\/booking-teacher-detail(?:\/.*)?$/, async route => {
    const request = route.request();
    requests.push({ method: request.method(), url: request.url(), body: request.method() === 'POST' ? request.postDataJSON() : undefined });
    if (request.method() === 'GET') {
      if (gate) await gate;
      if (mode === 'error') return route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ success: false, message: 'unavailable', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null }) });
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(mode === 'empty' ? null : item)) });
    }
    const body = request.postDataJSON() as Record<string, unknown>;
    if (request.url().endsWith('/cancel')) {
      item = { ...item, status: 'CANCELLED', cancelReason: body.reason, version: 8 };
    } else if (request.url().endsWith('/complete')) {
      item = { ...item, status: 'COMPLETED', sessionReport: body.report, version: 8 };
    } else if (request.url().endsWith('/dispute')) {
      item = { ...item, canDispute: false, settlementStatus: 'DISPUTE_PENDING', settlement: { ...item.settlement, status: 'DISPUTE_PENDING', disputeReason: body.reason } };
    }
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(envelope(item)) });
  });
  return { requests, releaseGet: () => releaseGet?.(), setMode: (next: 'success' | 'error' | 'empty') => { mode = next; } };
}

test('teacher booking detail follows approved hierarchy and fits 375/768/1280', async ({ page }) => {
  const { requests } = await routeBooking(page);
  await openPage(page);
  await expect(page.getByRole('heading', { name: 'Toán 11' })).toBeVisible();
  expect(requests).toContainEqual(expect.objectContaining({ method: 'GET', url: expect.stringContaining('/api/teacher/bookings/booking-teacher-detail') }));
  if (variant !== 'before') {
    await expect(page.locator('.tm-back-link')).toHaveAttribute('href', '/teacher/bookings');
    await expect(page.getByRole('heading', { name: 'Thông tin buổi dạy' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Quyết toán buổi học' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Báo cáo buổi học' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Mở phòng học' })).toHaveAttribute('href', initialBooking.meetingLink);
    await expect(page.getByRole('heading', { name: 'Lý do hủy' })).toHaveCount(0);
  }
  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    if (variant !== 'before') expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidence, `teacher-booking-detail-${variant}-${width}.png`), fullPage: true });
    if (width === 375 && variant !== 'before') {
      await page.evaluate(() => window.scrollTo(0, document.documentElement.scrollHeight));
      const clearance = await page.evaluate(() => {
        const action = [...document.querySelectorAll('a,button')].find(element => ['Mở phòng học', 'Hủy buổi học'].includes(element.textContent?.trim() ?? ''));
        const nav = [...document.querySelectorAll('nav')].map(node => ({ rect: node.getBoundingClientRect(), style: getComputedStyle(node) })).find(value => value.style.position === 'fixed' && value.rect.bottom >= window.innerHeight - 1);
        return { bottom: action?.getBoundingClientRect().bottom, navTop: nav?.rect.top };
      });
      expect(clearance.bottom).toBeLessThan(clearance.navTop! - 8);
    }
  }
  if (variant === 'before') return;

  await expect(page.getByText('Buổi học nằm ngoài lịch rảnh đã công bố')).toBeVisible();
  await expect(page.getByText('Đang giữ tiền')).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Khiếu nại khoản tiền đang giữ' })).toHaveCount(0);
  const cancelButton = page.getByRole('button', { name: 'Hủy buổi học' });
  await cancelButton.focus();
  await expect(cancelButton).toBeFocused();
  await page.keyboard.press('Enter');
  await expect(page.getByRole('dialog', { name: 'Hủy lịch học' })).toBeVisible();
});

test('cancel action keeps its existing role and version payload', async ({ page }) => {
  const { requests } = await routeBooking(page);
  await openPage(page);
  await page.getByRole('button', { name: 'Hủy buổi học' }).click();
  await expect(page.getByRole('dialog', { name: 'Hủy lịch học' })).toBeVisible();
  await page.getByRole('radio', { name: 'Gia sư có việc đột xuất' }).check();
  await page.getByPlaceholder('Nhập lý do hủy lịch học chi tiết...').fill('  Gia sư có việc đột xuất.  ');
  await page.getByRole('button', { name: 'Xác nhận hủy buổi học' }).click();
  await expect(page.getByText('Đã hủy lịch học thành công')).toBeVisible();
  expect(requests).toContainEqual(expect.objectContaining({ method: 'POST', url: expect.stringMatching(/\/api\/teacher\/bookings\/booking-teacher-detail\/cancel$/), body: { version: 7, reason: 'Gia sư có việc đột xuất.', initiatedBy: 'TEACHER_EMERGENCY' } }));
  await expect(page.getByRole('heading', { name: 'Lý do hủy' })).toBeVisible();
});

test('completion report and dispute remain gated by current state and preserve request payloads', async ({ page }) => {
  const pastBooking = { ...initialBooking, startTime: '2026-10-14T10:00:00Z', endTime: '2026-10-14T11:00:00Z', sessionReport: null };
  const routed = await routeBooking(page, { item: pastBooking });
  await openPage(page);
  await expect(page.getByRole('button', { name: 'Hoàn thành và viết báo cáo' })).toBeVisible();
  await page.getByRole('button', { name: 'Hoàn thành và viết báo cáo' }).click();
  await page.getByLabel('Nội dung bài dạy').fill('Đã ôn chương phương trình.');
  await page.getByLabel('Nhận xét học viên').fill('Tiếp thu tốt.');
  await page.getByRole('button', { name: 'Hoàn thành & Gửi báo cáo' }).click();
  await expect(page.getByRole('heading', { name: 'Báo cáo buổi học' })).toBeVisible();
  expect(routed.requests).toContainEqual(expect.objectContaining({ method: 'POST', body: { version: 7, report: { content: 'Đã ôn chương phương trình.', feedback: 'Tiếp thu tốt.', teacherSelfRating: 5 } } }));

  const heldBooking = { ...initialBooking, status: 'COMPLETED', canDispute: true, settlementStatus: 'HELD', settlement: { ...initialBooking.settlement, status: 'HELD' }, sessionReport: null };
  await page.unrouteAll({ behavior: 'wait' });
  const dispute = await routeBooking(page, { item: heldBooking });
  await page.goto('/teacher/bookings/booking-teacher-detail');
  await page.getByRole('button', { name: 'Khiếu nại khoản tiền đang giữ' }).click();
  await page.getByLabel('Lý do').fill('Buổi học chưa được xác nhận đúng nội dung.');
  await page.getByRole('button', { name: 'Gửi khiếu nại' }).click();
  await expect(page.getByText('Đã gửi khiếu nại.')).toBeVisible();
  expect(dispute.requests).toContainEqual(expect.objectContaining({ method: 'POST', url: expect.stringMatching(/\/api\/teacher\/bookings\/booking-teacher-detail\/dispute$/), body: { version: 7, reason: 'Buổi học chưa được xác nhận đúng nội dung.' } }));
  await expect(page.getByRole('region', { name: 'Quyết toán buổi học' }).getByText('Chờ xử lý khiếu nại')).toBeVisible();
  expect(await page.getByRole('button', { name: 'Khiếu nại khoản tiền đang giữ' }).count()).toBe(0);

  await page.unrouteAll({ behavior: 'wait' });
  const reopened = { ...initialBooking, status: 'COMPLETED', settlementStatus: 'REOPENED', canConfirm: true, canDispute: false, sessionReport: null, endTime: '2026-10-20T11:00:00Z', settlement: { ...initialBooking.settlement, status: 'REOPENED' } };
  await routeBooking(page, { item: reopened });
  await page.goto('/teacher/bookings/booking-teacher-detail');
  await expect(page.getByRole('button', { name: 'Xác nhận lại và nộp báo cáo' })).toBeVisible();
});

test('loading, GET error/retry, empty response and booking-specific display rules remain intact', async ({ page }) => {
  const routed = await routeBooking(page, { hold: true });
  await openPage(page);
  await expect(page.getByRole('status', { name: 'Đang tải chi tiết buổi học' })).toBeVisible();
  routed.releaseGet();
  await expect(page.getByRole('heading', { name: 'Toán 11' })).toBeVisible();

  routed.setMode('error');
  await page.reload();
  await expect(page.getByText('Chưa đọc được buổi học')).toBeVisible();
  routed.setMode('success');
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByRole('heading', { name: 'Toán 11' })).toBeVisible();

  routed.setMode('empty');
  await page.reload();
  await expect(page.getByText('Chưa đọc được buổi học')).toBeVisible();

  const cancelled = { ...initialBooking, status: 'CANCELLED', cancelReason: 'Đổi lịch đã thống nhất.\nGiữ nguyên dòng.' };
  await page.unrouteAll({ behavior: 'wait' });
  await routeBooking(page, { item: cancelled });
  await page.reload();
  await expect(page.getByText('Đổi lịch đã thống nhất.\nGiữ nguyên dòng.')).toBeVisible();

  const unsafeLink = { ...initialBooking, meetingLink: 'javascript:alert(1)' };
  await page.unrouteAll({ behavior: 'wait' });
  await routeBooking(page, { item: unsafeLink });
  await page.reload();
  await expect(page.getByRole('link', { name: 'Mở phòng học' })).toHaveCount(0);
});
