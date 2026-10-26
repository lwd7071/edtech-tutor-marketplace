import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';
const teacher = { id: 'stats-teacher', email: 'stats@example.test', fullName: 'Trần Thu Hà', role: 'TEACHER', status: 'ACTIVE' };
const stats = {
  teacherId: teacher.id,
  averageRating: 4.8,
  bayesianRating: 4.7,
  reviewCount: 17,
  completedSessionCount: 42,
  completionRate: 0.92,
  trialSessionCount: 8,
  trialConversionRate: 0.25,
  globalRank: 7,
  calculatedAt: '2026-10-16T08:30:00Z',
};
const envelope = (data: unknown) => ({ success: true, message: 'ok', data, errors: null, meta: null });

test('teacher stats show only API metrics and keep loading, empty reviews, and error states distinct', async ({ page }) => {
  const evidence = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidence, { recursive: true });
  let responseStatus = 200;
  let responseStats: typeof stats | null = stats;
  let holdInitial = true;
  let releaseInitial!: () => void;
  const initialRequest = new Promise<void>(resolveRequest => { releaseInitial = resolveRequest; });
  let requestCount = 0;
  const methods: string[] = [];

  await page.addInitScript(user => {
    localStorage.setItem('tutor-match.session', JSON.stringify({ user, remember: false, sessionId: 'teacher-stats-session' }));
    document.cookie = 'accessToken=teacher-stats-access; path=/; SameSite=Lax';
  }, teacher);
  await page.route('**/api/teacher/stats', async route => {
    requestCount += 1;
    methods.push(route.request().method());
    if (holdInitial) await initialRequest;
    await route.fulfill({
      status: responseStatus,
      contentType: 'application/json',
      body: JSON.stringify(responseStatus === 200
        ? envelope(responseStats)
        : { success: false, message: 'unavailable', data: null, errors: [{ code: 'INTERNAL_ERROR' }], meta: null }),
    });
  });

  await page.goto('/teacher/stats');
  await expect(page.locator('.ant-skeleton').first()).toBeVisible();
  holdInitial = false;
  releaseInitial();
  await expect(page.getByRole('heading', { name: 'Thống kê hoạt động' })).toBeVisible();
  if (variant === 'after') {
    await expect(page.getByText('42', { exact: true })).toBeVisible();
    await expect(page.getByText('92%', { exact: true })).toBeVisible();
    await expect(page.getByText('25%', { exact: true })).toBeVisible();
    await expect(page.getByText('4.8', { exact: true })).toBeVisible();
    await expect(page.getByText('#7', { exact: true })).toBeVisible();
    await expect(page.getByText('Điểm xếp hạng đã điều chỉnh: 4.7', { exact: true })).toBeVisible();
    await expect(page.getByText('Cập nhật')).toBeVisible();
  } else {
    await expect(page.getByText('Tỷ lệ hoàn thành: 0.92%')).toBeVisible();
  }

  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    if (variant === 'after') {
      const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
      expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
      if (width <= 768) {
        const lastMetric = await page.locator('[aria-labelledby="teacher-stats-activity-heading"] dl > div').last().boundingBox();
        const bottomNavigation = await page.locator('.tm-workspace-bottom').boundingBox();
        expect(lastMetric).not.toBeNull();
        expect(bottomNavigation).not.toBeNull();
        expect(lastMetric!.y + lastMetric!.height).toBeLessThanOrEqual(bottomNavigation!.y + 1);
      }
    }
    await page.screenshot({ path: resolve(evidence, `teacher-stats-${variant}-${width}.png`), fullPage: true });
  }
  if (variant === 'before') return;

  responseStats = null;
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Chưa có dữ liệu thống kê' })).toBeVisible();
  responseStats = { ...stats, averageRating: 0, bayesianRating: 0, reviewCount: 0, globalRank: 0 };
  await page.reload();
  await expect(page.getByText('Chưa có đánh giá', { exact: true })).toBeVisible();
  await expect(page.getByText('Chưa đủ dữ liệu xếp hạng', { exact: true })).toBeVisible();

  responseStatus = 503;
  await page.reload();
  await expect(page.getByText('Chưa tải được thống kê')).toBeVisible();
  await expect(page.getByText('42', { exact: true })).toHaveCount(0);
  await page.keyboard.press('Tab');
  await page.getByRole('button', { name: 'Thử lại' }).focus();
  expect(await page.evaluate(() => document.activeElement?.matches(':focus-visible') ?? false)).toBe(true);
  responseStatus = 200;
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByText('42', { exact: true })).toBeVisible();
  expect(requestCount).toBeGreaterThanOrEqual(5);
  expect(methods.every(method => method === 'GET')).toBe(true);
});
