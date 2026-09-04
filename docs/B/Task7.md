# Kế hoạch & Bàn giao — Task 7 (Tuần 7): Finance, Admin Dashboard, Platform Settings, Audit Logs & Scheduled Jobs

## Tổng quan (Handoff Summary)
Tài liệu này ghi chú lại chi tiết tiến độ và kết quả thực hiện của Task 7: Phân hệ Tài chính (Ngân hàng, Rút tiền Payout, Hoàn tiền Refund, Gia hạn Extension, Sổ cái Ledger), Bảng điều khiển Quản trị viên (Dashboard), Cài đặt sàn (Platform Settings), Nhật ký kiểm toán (Audit Logs) và các Scheduled Jobs.

- **Trạng thái:** Đã hoàn thành (Done)
- **Kiểm thử:** 229/229 tests PASS 100%.
- **Đặc trưng:** Mã hóa số tài khoản AES-GCM, hạch toán sổ cái kép (double-entry ledger) chuẩn xác từng đồng, bảo vệ concurrency bằng pessimistic locking.

---

## Chi tiết những gì đã làm

### 1. Teacher BankAccount CRUD (Task 7.1)
- **Entity**: `TeacherBankAccount` (quản lý liên kết tài khoản ngân hàng của giáo viên).
- **Security**: Mã hóa số tài khoản qua `AccountNumberCipher` bằng thuật toán AES-256-GCM. View trả về luôn mask định dạng `******1234`.
- **Invariants**: Partial unique index `ux_bank_accounts_default` đảm bảo mỗi giáo viên chỉ có tối đa 1 tài khoản mặc định đang active.
- **Endpoints**: `TeacherBankAccountController` (`GET`, `POST 201`, `PUT`, `DELETE 204`).

### 2. Payout Request Flow (Task 7.2)
- **State Machine**: `PayoutRequest` (`PENDING` $\rightarrow$ `PROCESSING` $\rightarrow$ `SUCCEEDED` / `REJECTED` / `FAILED`).
- **Khóa & Sổ cái**:
  - Giáo viên tạo yêu cầu: Khóa ví `Wallet (PESSIMISTIC_WRITE)` $\rightarrow$ kiểm tra `availableBalance >= amount` $\rightarrow$ chuyển số dư từ `available` sang `reserved` $\rightarrow$ ghi 2 `LedgerEntry` (`PAYOUT_RESERVED`).
  - Admin duyệt & hoàn tất: Upload bằng chứng chuyển khoản $\rightarrow$ debit `reservedBalance` $\rightarrow$ ghi `PAYOUT_SUCCEEDED`.
  - Admin từ chối: Hoàn trả `reservedBalance` về `availableBalance` $\rightarrow$ ghi `PAYOUT_RELEASED`.

### 3. Refund Request Flow (Task 7.3)
- **State Machine**: `RefundRequest` (`PENDING` $\rightarrow$ `APPROVED` $\rightarrow$ `PROCESSING` $\rightarrow$ `REFUNDED` / `REJECTED` / `FAILED`).
- **Quy trình**:
  - Học sinh tạo yêu cầu: Khóa gói học, kiểm tra không có booking `SCHEDULED` $\rightarrow$ chuyển gói sang `REFUND_PENDING` (chặn đặt lịch mới ngay lập tức).
  - Tính toán số tiền hoàn trả tích lũy theo công thức số nguyên $\lfloor\dots\rfloor$ (zero-residual rounding):
    $$\text{refundAmount} = \lfloor \frac{(\text{resolvedBefore} + n) \times \text{price}}{\text{total}} \rfloor - \lfloor \frac{\text{resolvedBefore} \times \text{price}}{\text{total}} \rfloor$$
  - Khấu trừ ví `PENDING` của giáo viên qua `LedgerEntry` (`REFUND_DEBIT_PENDING`).
  - Phục hồi trạng thái gói nếu bị từ chối; hoàn tất gói sang `REFUNDED` nếu hoàn tiền toàn bộ.

### 4. Package Extension Request Flow (Task 7.4)
- **State Machine**: `PackageExtensionRequest` (`PENDING` $\rightarrow$ `APPROVED` / `REJECTED`).
- **Invariants**: Chỉ học sinh sở hữu gói `LOCKED_EXPIRED` mới được gửi yêu cầu; Admin duyệt bắt buộc kiểm tra `approvedExpiryDate > now` và kích hoạt lại gói thành `ACTIVE`.

### 5. Wallet & Ledger API (Task 7.5)
- `TeacherWalletController`:
  - `GET /api/teacher/wallet`: Xem chi tiết 3 bucket số dư (`pending`, `available`, `reserved`).
  - `GET /api/teacher/wallet/ledger`: Xem lịch sử biến động sổ cái phân trang, hỗ trợ bộ lọc theo bucket / entry type / direction.

### 6. Admin Dashboard, Settings & Audit Logs (Task 7.6)
- **PlatformSettings**: JPA entity `PlatformSettings` quản lý singleton cấu hình hệ thống: tỉ lệ hoa hồng sàn (`commissionRate`), giờ nhắc lịch (`bookingReminderHours`), giờ hủy lịch quá hạn (`bookingExpirationHours`).
- **AdminDashboard**: Thống kê tổng hợp số liệu kinh doanh: tổng GMV, doanh thu hoa hồng sàn, số lượng người dùng/giáo viên/học sinh, số buổi học, số lượng yêu cầu payout/refund đang chờ xử lý.
- **AuditLog**: Truy vấn phân trang nhật ký kiểm toán với bộ lọc actor / action / targetType.

### 7. Scheduled Jobs Hardening (Task 7.7)
- Rà soát toàn bộ 5 background schedulers: `BookingExpiryJob`, `BookingReminderJob`, `PackageExpiryJob`, `InvoiceExpiryJob`, `TeacherStatsJob` — đều được bảo vệ bởi `@SchedulerLock` (ShedLock), chạy idempotent, an toàn khi cluster đa node.

---

## Bằng chứng kiểm thử
- Finance Domain & Service Tests: 20 tests PASS.
- Finance & Admin Controllers: 24 tests PASS.
- Architecture & Boundary Tests: 14 tests PASS.
- Flyway & Persistence Integration Tests: 35 tests PASS.
- Full Maven Test Suite: 229/229 tests PASS 100%.

