# Kế hoạch Triển khai Frontend - Thành viên B

Dựa trên tài liệu `PLANFE.md`, `SPEC-FE.md` và `ERROR_CODES.md`, dưới đây là kế hoạch chi tiết cho **Thành viên B (Transaction & Operations)**.

---

## 1. Vai trò và Trách nhiệm

Thành viên B chịu trách nhiệm về **Transaction & Operations** và **Hạ tầng App Shell / Network Core**.

**Các feature sở hữu:**
- `payments` (Thanh toán qua PayOS, checkout, invoice polling, kết quả thanh toán...)
- `student-packages` (Gói học của học viên, chi tiết gói, thống kê buổi học, lịch sử giao dịch...)
- `bookings` (Đặt lịch học, lịch giáo viên/học viên, báo cáo buổi học SessionReport, yêu cầu học thử TrialRequest...)
- `finance` (Ví giáo viên, số dư pending/available, sổ cái Ledger, tài khoản ngân hàng, yêu cầu rút tiền Payout, hoàn tiền Refund, gia hạn Extension...)
- `admin` (Duyệt hồ sơ giáo viên, duyệt đề xuất môn học, kiểm duyệt tài chính, cấu hình hệ thống, nhật ký Audit Log, Dashboard tổng quan...)
- **App Shell & Providers**: TanStack Query, Axios Client (Refresh Token Queue), Error Envelope Mapping, Route Guards (`AuthGuard`, `RoleGuard`, `TeacherApprovalGuard`).

**Các route phụ trách:**

| Nhóm | Route |
|---|---|
| Payments & Checkout | `/checkout`, `/payment/success`, `/payment/cancel` |
| Student - Packages | `/student/packages`, `/student/packages/:id` |
| Bookings & Calendar | `/student/bookings`, `/teacher/bookings`, `/trials` |
| Teacher - Finance | `/teacher/wallet`, `/teacher/bank-account`, `/teacher/payouts` |
| Student - Finance | `/student/refunds`, `/student/extensions` |
| Admin | `/admin/dashboard`, `/admin/teachers`, `/admin/subjects`, `/admin/payouts`, `/admin/refunds`, `/admin/extensions`, `/admin/settings`, `/admin/audit-logs` |

---

## 2. Kế hoạch chi tiết theo từng tuần (8 Tuần)

### Tuần 1: Nền tảng App Shell & Network Core (Frontend Foundation)
- Thiết lập TanStack Query Provider & Global Error Boundary.
- Xây dựng Axios Client với **Refresh-Token Queue** (chống concurrency 401 khi nhiều request đồng thời) và tự động retry.
- Chuẩn hóa Response Envelope & Error Mapping theo `ERROR_CODES.md`.
- Xây dựng hệ thống Route Guards: `AuthGuard`, `RoleGuard`, `TeacherApprovalGuard`.
- Khởi tạo bộ khung 5 feature modules: `payments`, `student-packages`, `bookings`, `finance`, `admin`.

### Tuần 2: Quản trị Phê duyệt & Onboarding Admin
- Admin Teacher Approval Queue & Chi tiết thẩm định hồ sơ (xem văn bằng, chứng chỉ, duyệt/từ chối kèm lý do).
- Admin Subject Proposal Queue & Duyệt đề xuất môn học mới.
- Admin User Moderation Screen (khóa/mở khóa tài khoản).

### Tuần 3: Student Dashboard Shell & Package Integration
- Student Dashboard Shell (tích hợp layout, navigation).
- Hiển thị Package summary placeholder cho Marketplace CTA (giao tiếp qua public exports).

### Tuần 4: Checkout & Student Package
- Luồng PayOS checkout flow (tạo link thanh toán, redirect).
- Màn hình kết quả/hủy thanh toán (`/payment/success`, `/payment/cancel`) và Invoice polling liên tục kiểm tra trạng thái Backend (không dựa vào query params để xác nhận thanh toán).
- Quản lý danh sách và chi tiết StudentPackage, hiển thị bộ 4 chỉ số buổi học (`SessionCounter`).

### Tuần 5: Booking & Session Report
- Lịch học tương tác (Calendar view) cho Student và Teacher.
- Teacher tạo / hoàn thành / hủy Booking theo state machine.
- Biểu mẫu tạo SessionReport và xem chi tiết buổi học.
- Luồng gửi và xử lý TrialRequest (học thử không trừ lượt học).

### Tuần 6: Tích hợp Booking với Chat & Learning
- Cung cấp route context và booking links cho các module Chat & Learning của Thành viên A qua public interfaces.

### Tuần 7: Quản trị Tài chính (Finance & Admin)
- Teacher Wallet & Ledger (lịch sử biến động số dư pending/available).
- Quản lý tài khoản ngân hàng giáo viên & Tạo yêu cầu rút tiền (Payout Request).
- Học viên gửi yêu cầu hoàn tiền (Refund) và gia hạn gói học (Extension).
- Admin Finance Queues (phê duyệt rút tiền, hoàn tiền, gia hạn), cấu hình hoa hồng nền tảng (`PlatformSettings`), Audit Logs & biểu đồ Dashboard.

### Tuần 8: Hardening, E2E & Production Readiness
- Viết integration test/hook tests cho toàn bộ feature của B.
- Kiểm tra xử lý token hết hạn, concurrency optimistic locking (409 conflict).
- Phối hợp với A chạy E2E full flow: Onboarding → Mua gói → Đặt lịch → Học & Báo cáo → Quyết toán ví → Rút tiền.
