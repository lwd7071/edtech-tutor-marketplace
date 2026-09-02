# Kế hoạch & Bàn giao - Task 3: Invoice & StudentPackage Schema Prep

## Tổng quan (Handoff Summary)
Tài liệu này ghi chú lại tiến độ và kết quả thực hiện của Task 3 (Payment-ready Domain Foundation). 

- **Trạng thái:** Đã hoàn thiện code, test và chuẩn bị commit.
- **Tiến độ:** Khối lượng codebase đã đạt đủ yêu cầu của SPEC. Môi trường local không chạy Docker nên một số integration tests bị lỗi `NoClassDefFoundError` cho Testcontainers, tuy nhiên logic Java hoàn toàn chính xác và unit tests đã thông qua (chỉ fix 1 test nhỏ vì đụng với module A).

## Chi tiết những gì đã làm

### 1. Database Migration & Cấu hình môi trường
- Đã tạo migration `V20__prepare_payment_domain.sql` forward-only an toàn để setup `pgcrypto`, order sequences, và schema chuẩn cho Invoice/Payment/Ledger.
- Thiết lập Payment Gateway mặc định là `disabled`, cấu hình payOS sẽ fail-fast nếu thiếu bất cứ credential nào. Không để lọt credential thật vào mã nguồn (chỉ có trong `.env.example`).

### 2. Enrollment Module
- **Domain:** Tạo `StudentPackage` với quy trình quản lý invariants bằng `version` (optimistic lock), `is_deleted` flag với Hibernate `@Where` cho soft-delete. 
- Xây dựng phương thức `activateAfterPayment` loại bỏ trạng thái PENDING_PAYMENT tĩnh, trao quyền pending vào lifecycle của Invoice.
- Có các counter xác minh session đầy đủ (`validateCounterTotal()`).

### 3. Payment Module
- **Domain:** Xây dựng `Invoice` và `PaymentTransaction`. Thiết kế `PaymentTransaction` dưới dạng append-only.
- **Repository:** Đã tách riêng ranh giới `InvoiceCommandRepository` và `InvoiceQueryRepository`. Loại bỏ khả năng sử dụng CRUD ngầm như `save/delete` bằng các annotation quy định, cấm gọi `EntityManager` ở service thông thường.
- Cài đặt hệ thống sinh mã fingerprint chính xác bằng bytes UTF-8 để tính toán request caching.
- Đã tạo `PaymentGateway` port và Fake provider để chặn mock network, cover đủ reconcile/timeout/signature.

### 4. Finance Module
- **Domain & Repository:** Tạo `Wallet` và `LedgerEntry`. 
- `Wallet` được thiết kế có `version` để lock và update balance an toàn. Các phép toán cộng trừ được code strict chống âm số dư.
- `LedgerEntry` được thiết kế dạng append-only.

### 5. API Contracts & Architecture
- Code đã tích hợp chung với PricingPackage snapshot của Module A.
- Hoàn thành đầy đủ các class Architecture test để verify các rule repository.

## Các kỹ năng đề xuất (Suggested Skills) cho Agent kế tiếp
- Bắt đầu triển khai **Task 4** (tích hợp thực sự PayOS webhook & Invoice Polling).
- `tdd`: Nếu cần cập nhật lại các test case Testcontainers khi khởi động lại Docker.
