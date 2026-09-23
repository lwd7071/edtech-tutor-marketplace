# Kiến trúc Backend Tutor Match

Backend là modular monolith Spring Boot. Mỗi module trong `com.edtech.platform` sở hữu domain, repository và nghiệp vụ của chính nó. Module khác chỉ gọi qua `facade`, snapshot DTO, command hoặc event công khai; controller không truy cập repository.

## Các seam chính

| Module | Trách nhiệm | Điểm mở rộng |
|---|---|---|
| `auth` | Đăng ký, phiên đăng nhập, xác minh, khôi phục mật khẩu, OAuth và liên hệ phụ huynh | `OAuthAuthorizationPort`, `IdentityFacade` |
| `mail` | Template, transactional outbox, lease/retry và vận chuyển email | `MailTransport`: Brevo HTTPS hoặc logging |
| `payment` | Invoice, webhook và cổng thanh toán | `PaymentGateway` |
| `finance` | Wallet, ledger, refund, payout và mã hóa tài khoản | `AccountNumberProtector`, finance commands |
| `booking` | Tạo, hoàn thành, hủy và đọc lịch học | command service và `BookingReadService` |
| `learning` | Assignment, submission, attachment và mapping view | `AssignmentAttachmentBinder`, `AssignmentViewMapper` |
| `communication` | Chat, notification và realtime | command/query service, event `AFTER_COMMIT` |
| `catalog` | Public teacher search/profile và subject catalog | SQL projection, `TeacherSearchCache`, cache configuration contributor |
| `dashboard` | Read-model tổng hợp cho các màn hình cá nhân | `StudentDashboardRepository` dùng một SQL projection, không sở hữu mutation/invariant |

Các side effect ra email provider, WebSocket và notification chỉ chạy sau khi transaction nghiệp vụ commit. Các invariant về tiền, ledger, lượt học và lock order vẫn nằm trong transaction của module sở hữu.

## Identity và OAuth

`AuthService` tổng đã được thay bằng các lifecycle riêng:

- `RegistrationService`: đăng ký tài khoản.
- `SessionService` và `SessionIssuer`: login, refresh rotation, logout và JWT.
- `AccountVerificationService`: xác minh/resend email.
- `PasswordRecoveryService`: forgot/reset password và thu hồi phiên.
- `OAuthAccountService`: authorize Google identity, exchange code và hoàn tất đăng ký.
- `ParentContactService`: thông tin liên hệ phụ huynh.

Google redirect có hai URL khác nhau:

- `GOOGLE_OAUTH_REDIRECT_URI`: Google gọi về Backend, mặc định `http://localhost:8080/login/oauth2/code/google`.
- `APP_OAUTH2_FRONTEND_CALLBACK_URI`: Backend chuyển trình duyệt về FE, mặc định `http://localhost:3000/oauth2/callback`.

`APP_OAUTH2_REDIRECT_URI` chỉ là alias tương thích tạm thời cho callback FE. Profile `cloud` yêu cầu `GOOGLE_CLIENT_ID` và `GOOGLE_CLIENT_SECRET`; không có dummy credential trong profile này.

## Cấu hình

Cấu hình ứng dụng được bind qua các `@ConfigurationProperties` có validation: JWT, mail, OAuth, payment, mã hóa tài khoản, CORS và Cloudinary. Domain/application code không đọc trực tiếp environment hoặc system property. Secret chỉ đặt trong `.env.cloud` hoặc secret store và không commit.

Ba profile có contract riêng: `cloud` là mặc định, tự nạp `.env.cloud` và fail-fast với secret thật; `local` phải được chọn rõ ràng và tự chứa các giá trị phát triển an toàn; `test` là nguồn cấu hình canonical cho Testcontainers/fake provider. Base config chỉ chứa thuộc tính dùng chung. Docker image mặc định `cloud`, còn `docker-compose.yml` phát triển ghi đè sang `local`.

Spring runtime nạp `.env.cloud` qua `spring.config.import`; Flyway Maven không tự nạp file này và chỉ được gọi qua script migration có guard. Cloudinary cloud nhận ba credential rời và ghép URL trong profile. Preflight `scripts/check-backend-config.ps1` chỉ báo tên key thiếu, không in giá trị.

Mail dùng `APP_EMAIL_PROVIDER=logging` ở local/test và `brevo` trên cloud. Cloud yêu cầu `BREVO_API_KEY`, `BREVO_SENDER_EMAIL` và `BREVO_SENDER_NAME`; API URL và timeout có default nội bộ. Delivery gọi Brevo Transactional Email API qua HTTPS, dùng `email_outbox.id` làm idempotency key; lỗi mạng, `429` và `5xx` được retry, response idempotency trùng được coi là request đã nhận, còn request bị từ chối vĩnh viễn chuyển `FAILED`. Outbox luôn được ghi trong transaction; delivery job claim bằng lease, gửi ngoài transaction giữ database lock rồi mới đánh dấu kết quả.

Teacher search dùng SQL projection và batch query cho subject, không hydrate entity graph. Keyword được normalize bằng functional indexes PostgreSQL theo quyết định tại [ADR-0004](../adr/0004-accent-insensitive-teacher-search.md). Cache search là tối ưu tùy chọn: Redis lỗi phải fallback về PostgreSQL, chỉ cache page 0–2 với size tối đa 50 và không thay đổi response contract.

Student dashboard là read-model cross-domain chỉ đọc. Controller chỉ gọi service; service gọi `StudentDashboardRepository` đúng một lần. Repository dùng một SQL projection có các scalar subquery trên bảng booking, enrollment, learning, communication và finance, lọc theo `studentId` từ principal. Module này không expose entity và không được dùng cho command/mutation.

Admin user directory thuộc read side của `auth`, expose qua `IdentityDirectoryFacade` và snapshot DTO; `admin` không truy cập `UserRepository`. Booking settlement admin view thuộc `booking`; service batch-load page bookings bằng một `findAllById`, rồi gọi teacher/identity facade tối đa một lần mỗi bên, giữ thứ tự page và không cache ngoài invocation.

## Audit snapshot và thay đổi nơi ở

AuditLog shallow-copy map snapshot ở boundary persistence: map ngoài được copy thành LinkedHashMap không sửa được, giữ nguyên value null để Hibernate ghi thành JSONB null, còn key null bị từ chối trước khi tạo map đích. Nested object không deep-copy. AuditTrailFacade.append dùng transaction hiện tại (Propagation.MANDATORY), vì vậy audit của các command như cập nhật nơi ở phải commit hoặc rollback cùng nghiệp vụ.

PUT /api/teacher/profile/residence chỉ ghi audit khi một trong hai field thực sự thay đổi. DTO dùng String; field thiếu và field gửi null đều bind thành Java null, còn invariant “đã chọn xã thì phải có tỉnh” được kiểm tra bởi Bean Validation trước service.

## Kiểm thử và guardrail

Từ thư mục `backend`:

```powershell
mvn clean test -B --no-transfer-progress -Dfile.encoding=UTF-8
```

ArchUnit kiểm tra dependency module, controller/repository, domain/layer, vị trí vendor SDK, nguồn cấu hình và interface của external adapter. Integration tests dùng Testcontainers PostgreSQL/Redis; cần Docker daemon hoạt động. Không dùng `flyway repair`, không sửa migration đã áp dụng và không reset database để làm test xanh.
