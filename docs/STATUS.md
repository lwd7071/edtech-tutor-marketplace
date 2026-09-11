# Trạng thái dự án

> Cập nhật: 2026-09-11. Đây là ảnh chụp hiện tại, không phải nhật ký append-only.
> Phạm vi snapshot: code Student Journey đang có trong working tree; các thay đổi chưa được commit vẫn được đánh dấu là chưa nghiệm thu đầy đủ.

## Mốc kỹ thuật

- Backend modular monolith đã có các seam chính cho auth, mail, payment, finance, booking, learning và communication.
- Migration mới nhất trong working tree: `V27__backfill_learning_conversations.sql` (sau `V26__snapshot_invoice_purchase_terms.sql`).
- Không dùng Flyway `repair()`, không sửa migration đã áp dụng và không reset database người dùng.

## Trạng thái theo luồng

| Luồng | Trạng thái hiện tại | Ghi chú |
|---|---|---|
| Auth/verification/logout | Đã triển khai thay đổi Student Journey | Cần chạy full integration với Docker |
| Invoice snapshot/payment | Đã triển khai V26 | Chưa nghiệm thu DB sạch và DB nâng cấp |
| Trial/package/booking/review | Đã triển khai phần Student Journey | Cần kiểm thử xuyên luồng |
| Assignment/attachment | Đã triển khai endpoint và view mới | Cần kiểm thử file thật |
| Chat/notification/events | Đã triển khai event và mở conversation | Cần kiểm tra realtime sau commit |
| Parent contact/requests/reports | Đã triển khai UI/API liên quan | Cần smoke test theo role |
| Frontend Student Journey | Đã có route/component/API thay đổi | Playwright chưa chạy |

## Những gì đã có trong code hiện tại

### Auth và hồ sơ Student

- Registration trả `RegistrationResult { email, verificationRequired }` và không cấp session cho tài khoản chưa xác minh.
- Login/refresh áp dụng policy tài khoản đã xác minh; logout gọi revoke refresh token rồi xóa session local.
- Parent contact có đọc và cập nhật nullable tại `/api/student/parent-contact`; `notifyParent` tự tắt khi không còn email.

### Payment và enrollment

- Invoice lưu snapshot subject, tên gói, số buổi, thời hạn, thời lượng buổi, commission, return/cancel URL và fingerprint version.
- Webhook fulfillment dùng snapshot trên invoice, idempotency vẫn được giữ khi package bị ẩn hoặc ngừng bán.
- Student package view trả teacher/subject snapshot và counters để FE không phải enrich bằng public API.

### Trial, booking và review

- Student xem trial requests có filter trạng thái và pagination server.
- Trial validate teacher/subject/thời gian/note và chặn trial trùng theo cặp Student–Teacher.
- Booking detail đọc review hiện tại; FE chỉ hiện nút đánh giá khi booking completed và chưa có review.
- Student có trang session reports dùng API phân trang hiện có.

### Assignment và attachment

- Student có list assignment theo `progress=TODO|SUBMITTED|GRADED` và detail riêng theo ownership.
- Detail trả một `submission` của student, attachment đã kiểm tra quyền và metadata file thống nhất.
- Teacher assignment có lifecycle draft → publish → close; assignment draft hoặc resource khác owner bị che giấu.

### Chat và notification

- Student mở conversation idempotent qua `PUT /api/student/conversations/teachers/{teacherId}` khi có quan hệ hợp lệ.
- CTA nhắn tin đã được nối từ hồ sơ gia sư, package đã mua, booking và payment success.
- Lifecycle event cho payment/trial/booking/assignment/refund/extension tạo notification sau commit; notification hỗ trợ lọc `referenceType`.
- Chat có query phân trang, duplicate protection bằng `clientMessageId` và tải lại khi realtime gặp conversation mới.

### Frontend route và navigation

- Student dashboard hiển thị booking sắp tới, session còn lại, assignment cần làm, notification chưa đọc và request đang chờ.
- Có các trang `/student/requests`, `/student/session-reports`, `/terms`, `/privacy` và `/support`.
- Workspace guard, typed API boundary, feature query keys và file viewer/upload đã được chuẩn hóa theo frontend architecture.

## Bằng chứng kiểm thử gần nhất

- Backend focused Student tests: `36/36` pass.
- Frontend typecheck: pass.
- Frontend lint: pass.
- Frontend build: pass.
- CI regression focused suite (`StudentInvoiceControllerTest`, `ArchitectureTest`, `SolidGuardrailsArchitectureTest`): `9/9` pass sau khi bỏ mapping assignment trùng, sửa principal test và chuyển package read controller về module enrollment.
- GitHub Backend CI full Maven/Testcontainers: `325/325` pass, `0` failure, `0` error, `0` skipped tại run `34558136980`.
- Docker image build validation trong cùng run: pass.
- Full suite local: chưa chạy vì Docker Desktop trên máy đang tắt; bằng chứng CI dùng PostgreSQL 16/Testcontainers.
- Jest/Playwright cho Student Journey: **chưa xác minh trong lượt này**.

Các con số trên chỉ là bằng chứng gần nhất đã có; không suy ra full suite xanh.

## Việc đang chờ

1. Khi cần đối chiếu local, mở Docker và chạy `scripts/test-student-journey-docker.ps1`.
2. Xác nhận V26/V27 trên bản sao Supabase test; DB sạch đã được CI áp dụng tới V27.
3. Chạy smoke test các role Student, Teacher và Admin.
4. Cập nhật bảng này bằng số liệu thật sau mỗi lần chạy.

## Provider chưa xác minh

Google OAuth thật, PayOS thật, Cloudinary thật, SMTP tới người dùng thật và production deployment chưa được tính là pass trong trạng thái này.
