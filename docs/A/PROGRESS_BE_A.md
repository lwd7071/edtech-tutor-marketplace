# Báo Cáo Tiến Độ - Thành Viên A (Identity & Experience)

Thành viên A chịu trách nhiệm phát triển các module thuộc phần Identity & Experience. Kiến trúc của A tuân thủ theo chuẩn Domain-Driven Design (DDD). Dưới đây là thống kê chi tiết các công việc A đã hoàn thiện dựa trên source code thực tế.

## Tóm tắt các Module đã hoàn thành (Code Foundation)

### 1. Module `common` (Nền tảng hệ thống)
- **Cơ sở hạ tầng**: Đã xây dựng `BaseEntity`, hệ thống `ResponseEnvelope` để chuẩn hóa response.
- **Xử lý ngoại lệ**: Xây dựng `GlobalExceptionHandler` ánh xạ lỗi theo `ERROR_CODES.md`.
- **Security & Validation**: Các tiện ích phân quyền `SecurityUtils`, `OwnershipValidator`.
- **Event-Driven**: Đã setup Domain Event contract để giao tiếp giữa các module.

### 2. Module `auth` và `user` (Định danh & Người dùng)
- Đầy đủ luồng đăng ký (Register), đăng nhập (Login JWT), Refresh Token, đăng xuất.
- Xác minh email, reset password và luồng Google OAuth2.
- Tổ chức thông tin User và Parent Contact.

### 3. Module `teacher` và `subject` (Hồ sơ & Môn học)
- Quản lý `TeacherProfile` (thông tin cá nhân, avatar, bio).
- Quản lý upload và duyệt giấy tờ `TeacherDocument`.
- Quản lý `TeacherAvailability` (Lịch rảnh) và cơ chế check trùng lặp thời gian.
- Xây dựng danh mục `Subject` và quy trình đề xuất `SubjectProposal`.

### 4. Module `catalog` (Marketplace)
- Xây dựng public API: `PublicTeacherController`, `PublicReviewController`.
- Cung cấp tính năng filter giáo viên (`TeacherSearchParams`).
- Quản lý `PricingPackage` (Gói giá).

### 5. Module `communication` (Chat & Thông báo)
- Setup WebSocket/STOMP cho chat real-time.
- Xây dựng `Conversation` và `Message`.
- Xây dựng hệ thống `Notification` bất đồng bộ và API đánh dấu đã đọc.

### 6. Module `learning` (Học tập)
- Xây dựng `Assignment` (Bài tập) và content blocks.
- Tính năng `Submission` (Nộp bài) và `GradeSubmissionRequest` (Chấm điểm).

### 7. Module `ranking` (Đánh giá & Xếp hạng)
- Chức năng đánh giá `Review` kèm validation.
- Xây dựng `TeacherStats` tự động tính sao trung bình, tổng giờ dạy qua sự kiện (Event Listener).

---
**Đánh giá chung**: 
Thành viên A đã triển khai **100% phần khung và logic chính** cho toàn bộ 7 module. Toàn bộ Controller, Service, DTO, Domain (Entity) và Repository đã được định nghĩa. Tuy nhiên, đi kèm với khối lượng lớn là một lượng nợ kỹ thuật (Technical Debt) nghiêm trọng (xem file `DIAGNOSE_BE_A.md`).
