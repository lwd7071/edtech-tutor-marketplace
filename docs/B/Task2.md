# Kế hoạch & Bàn giao - Task 2: Admin Approval & Audit Log

## Tổng quan (Handoff Summary)
Tài liệu này ghi chú lại tiến độ và kết quả thực hiện của Task 2 để phục vụ việc bàn giao hoặc tiếp tục công việc.

- **Trạng thái:** Đã hoàn thành (Done)
- **Commit:** `3734059 feat(admin): implement approval and audit workflows`

## Chi tiết những gì đã làm

### 1. Audit Log Module
- **Domain & Repository:** Đã tạo `AuditLog` entity là immutable (append-only, không có soft delete hay update). Khởi tạo `AuditLogRepository` chỉ cho phép append thông qua `EntityManager.persist()`.
- **Service:** Hoàn thiện `AuditLogService.append()` để ghi nhận toàn bộ action thay đổi trạng thái trong hệ thống. Đã xử lý `AuditSnapshotMapper` với cơ chế whitelist snapshot cho Teacher, Subject Proposal và User. Không lưu lộ email/mật khẩu hay các thông tin nhạy cảm.
- **Testing:** Xác minh kiểu dữ liệu `JSONB` và `INET` hoạt động chính xác khi persistence với Testcontainers (PostgreSQL).

### 2. Admin Teacher Approval
- **API & Controller:** Xây dựng `AdminApprovalController` cho quy trình kiểm duyệt giáo viên (các endpoints `GET` danh sách, `POST` approve/reject).
- **Service & DTO:** Sử dụng pessimistic lock để chặn lỗi double-click/race condition (trả về lỗi `409 TEACHER_APPROVAL_ALREADY_PROCESSED`). Chỉ gọi public facade thay vì gọi vào trực tiếp module khác.
- Gắn đầy đủ AuditLog cho từng action kiểm duyệt.

### 3. Admin Subject Proposal
- Tương tự Teacher Approval, hoàn thiện `AdminApprovalController` cho Subject Proposals.
- Xử lý các logic tạo môn học mới (`CREATE_NEW`) hoặc link môn học hiện có (`LINK_EXISTING`) an toàn bằng `SubjectApprovalFacade`. Cài đặt pessimistic lock tương tự để trả về `409 SUBJECT_PROPOSAL_ALREADY_PROCESSED`.

### 4. User Moderation
- Xây dựng luồng khóa và mở khóa tài khoản thông qua endpoint `PATCH /api/admin/users/{id}/status`.
- Chỉ cho phép chuyển đổi giữa `ACTIVE` và `LOCKED`, cấm tự khóa tài khoản của chính admin. Xóa refresh token ngay khi khóa.
- Xử lý JWT filter kết hợp Redis caching (TTL 30s) để từ chối ngay lập tức các user vừa bị khóa.

## Các kỹ năng đề xuất (Suggested Skills) cho Agent kế tiếp
- `triage`: Nếu cần audit thêm lỗi hệ thống sau kiểm duyệt.
- `tdd`: Nếu có thay đổi thêm yêu cầu validation đối với DTO.
