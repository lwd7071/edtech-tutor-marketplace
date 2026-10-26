import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const variant = process.env.UI_CAPTURE_VARIANT === 'before' ? 'before' : 'after';

test('privacy page preserves the current policy and links to its related terms page', async ({ page }) => {
  const evidenceDirectory = resolve(process.cwd(), '../docs/redesign/evidence');
  mkdirSync(evidenceDirectory, { recursive: true });

  for (const width of [375, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.goto('/privacy');
    await page.addStyleTag({ content: 'nextjs-portal{display:none!important}' });
    await expect(page.locator('.tm-header .tm-brand')).toHaveText('tutormatch');
    await expect(page.getByRole('heading', { name: 'Chính sách riêng tư', level: 1 })).toBeVisible();
    await expect(page.getByText('Tutor Match lưu thông tin tài khoản, hồ sơ, giao dịch và hoạt động học tập cần thiết để cung cấp dịch vụ.')).toBeVisible();
    await expect(page.getByText('Thông tin liên hệ phụ huynh là tùy chọn và chỉ được dùng để gửi email khi học viên bật tính năng thông báo. Bạn có thể cập nhật hoặc xóa thông tin này trong hồ sơ.')).toBeVisible();
    await expect(page.getByText('Dữ liệu xác thực và thanh toán được giới hạn theo vai trò; thông tin nhạy cảm không được hiển thị trong hồ sơ công khai.')).toBeVisible();
    const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
    expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
    await page.screenshot({ path: resolve(evidenceDirectory, `privacy-${variant}-${width}.png`), fullPage: true });
  }

  if (variant === 'before') return;

  const relatedLink = page.getByRole('navigation', { name: 'Tài liệu liên quan' }).getByRole('link', { name: 'Điều khoản sử dụng' });
  await expect(relatedLink).toHaveAttribute('href', '/terms');
  for (let tab = 0; tab < 40 && !(await relatedLink.evaluate(element => element === document.activeElement)); tab += 1) {
    await page.keyboard.press('Tab');
  }
  await expect(relatedLink).toBeFocused();
  expect(await relatedLink.evaluate(element => element.matches(':focus-visible'))).toBe(true);
});
