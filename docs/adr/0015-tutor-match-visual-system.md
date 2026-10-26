# ADR 0015: Hệ thiết kế giao diện Tutor Match

## Status

Accepted — 2026-10-15

## Context

Tutor Match có nhiều nhóm route công khai và workspace theo vai trò. Các trang dùng Ant Design cùng CSS riêng, dẫn tới màu nhấn và bề mặt chưa đồng nhất. Người dùng đã duyệt hướng vàng nổi bật với chữ đen trong prototype tìm gia sư và yêu cầu áp dụng cho website thật.

## Decision

- Dùng vàng sáng `#FFD43B` và mực `#171714` làm nhận diện chính; CTA chính có nền vàng/chữ mực, public hero dùng vàng, workspace dùng nền trung tính ấm và vàng làm điểm nhấn.
- Giữ Be Vietnam Pro cho tiêu đề và Inter cho nội dung.
- `shared/design-system/tokens.ts` là nguồn màu dùng chung; CSS variables và Ant Design map từ token này. Trạng thái thành công/cảnh báo/lỗi giữ màu ngữ nghĩa riêng.
- Dữ liệu marketplace lấy từ API backend; không đưa số liệu hoặc hồ sơ thành tích mẫu của prototype vào website.
- Concept “Bàn học mở” được duyệt cho toàn website: public dùng hero ngà với mảng vàng hình học và minh họa flat; workspace dùng nền ấm, lưới dữ liệu gọn và điều hướng ưu tiên tác vụ. Trên mobile, workspace có thanh điều hướng nhanh và mục “Thêm” vẫn giữ mọi route.
- CTA chính dùng token `brandYellow`/`actionPrimary`, chữ `ink` và viền mực; `primary600` cũ không còn là nguồn để suy ra màu hành động mới.

## Consequences

- Thay đổi palette chung cần đi qua token nguồn thay vì thêm giá trị riêng cho component.
- Trang thao tác ưu tiên khả năng đọc, trạng thái và mật độ phù hợp nhiệm vụ; tính biểu cảm vàng tập trung ở public surfaces.
- Route mới phải theo token, nhãn accessible, focus bàn phím, responsive layout và reduced motion hiện có.
