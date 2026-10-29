# Hệ thiết kế

## Nguồn nhận diện

Tuân theo [ADR 0015](../adr/0015-tutor-match-visual-system.md): vàng `#FFD43B`, mực `#171714`, nền trung tính ấm; Be Vietnam Pro cho heading, Inter cho nội dung; semantic colors cho success/warning/error. Không tạo palette độc lập hoặc lấy token cũ `primary600` làm CTA.

Thiết kế lại bố cục/điều hướng được phép nhưng phải giữ nhận diện Tutor Match: hero public ấm, workspace tập trung tác vụ, menu rõ role, mobile điều hướng gọn.

## Tham khảo bên ngoài

Preply chỉ là tham khảo về cách trình bày nhu cầu tìm kiếm, bộ lọc, thông tin giúp so sánh tutor và CTA từ hồ sơ. Không sao chép layout, nội dung, hình ảnh, icon hay brand assets. Data/proof trong Tutor Match chỉ lấy từ backend thật.

## Tokens và layout

- Tạo CSS custom properties trong `static/css/tokens.css`, ánh xạ từ token ADR; không tự đặt giá trị màu tùy ý trong component.
- Palette ADR: `--brand-yellow: #FFD43B`, `--ink: #171714`. Token bổ sung đề xuất: `--surface: #FFFDF7`, `--surface-warm: #F7F3E8`, `--border: #DED9CC`, `--muted: #625F56`, `--success: #176B45`, `--warning: #8A4B08`, `--danger: #B42318`, `--focus: #155EEF`; kiểm tra WCAG AA trước khi ship.
- Type scale: page title 32/40px 700; section title 24/32px 650; body 16/24px 400; caption/label 13/18px 500. Mobile page title 28/36px. Be Vietnam Pro cho heading, Inter cho body/data, system sans fallback.
- Spacing: `4, 8, 12, 16, 24, 32, 48, 64px`. Radius 4px control, 8px card, 12px dialog/panel; pill chỉ cho badge. Dùng border trước shadow; elevation rõ chỉ cho menu/dialog.
- Content container tối đa 1200px; breakpoint 640/768/1024px.
- Public/Student mobile-first; Teacher/Admin desktop-first nhưng usable ở màn hình hẹp.

## Components

Button primary/secondary/ghost, input/select/textarea, label/help/error, badge, tutor card, package card, booking row, pagination, breadcrumb, alert, toast (chỉ khi có JS thật), confirmation dialog, empty state, inline validation, file picker, table responsive, loading indicator.

Modal, drawer, hover-only action và toast phải có fallback truy cập được qua link/form/inline region khi JS tắt. Không dùng skeleton nếu server đã render đồng bộ và không có request async.

## Hình ảnh

Ảnh tutor dùng URL được backend public DTO cho phép. Thiếu ảnh thì dùng placeholder/illustration tự tạo. Illustration trang trí dùng alt rỗng; ảnh nội dung có alt diễn đạt mục đích. Không dùng URL proof riêng tư hoặc ảnh stock như dữ liệu tutor thật.

## Chống mẫu giao diện rập khuôn

Tránh hero gradient tím/xanh, glassmorphism, dashboard KPI cards đồng dạng chỉ để lấp chỗ, pill cho mọi control, icon emoji, stock-photo hero, CTA cùng màu/kích thước ở mọi vùng, chart/dữ liệu giả, radius quá lớn trên mọi phần tử, stagger/reveal lặp khắp trang, modal cho tác vụ đơn giản và copy marketing sáo rỗng. Cấu trúc mỗi trang phải xuất phát từ tác vụ thật (tìm, học, duyệt, đối soát).

## Skill và trang duyệt đầu

Trước khi dựng UI, đọc skill `frontend-design` và `impeccable` trong skill catalog hiện hành; brief dự án và ADR 0015 ưu tiên hơn gợi ý thẩm mỹ chung. Dựng `/teachers` trước làm trang mẫu, duyệt desktop/mobile, token, card, filter, empty/error và no-JS trước khi nhân rộng component.
