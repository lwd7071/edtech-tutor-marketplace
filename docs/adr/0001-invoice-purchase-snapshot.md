# ADR-0001: Invoice giữ snapshot điều khoản mua

- **Trạng thái:** Accepted
- **Bối cảnh:** PricingPackage có thể đổi giá, số buổi, thời hạn hoặc commission trong lúc invoice còn chờ.
- **Quyết định:** Invoice lưu snapshot toàn bộ điều khoản tại thời điểm tạo. Webhook và fulfillment chỉ đọc snapshot trên invoice.
- **Hệ quả:** Cần migration bổ sung field snapshot; package bị sửa hoặc ẩn không làm thay đổi quyền lợi của invoice cũ.
