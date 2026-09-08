# Kiến trúc Backend Tutor Match

Backend là modular monolith Spring Boot. Mỗi module trong `com.edtech.platform` sở hữu domain, repository và nghiệp vụ của chính nó. Module khác chỉ gọi qua `facade`, snapshot DTO, command hoặc event công khai; controller không truy cập repository.

## Các seam chính

| Module | Trách nhiệm | Điểm mở rộng |
|---|---|---|
| `auth` | Đăng ký, phiên đăng nhập, xác minh, khôi phục mật khẩu, OAuth và liên hệ phụ huynh | `OAuthAuthorizationPort`, `IdentityFacade` |
| `mail` | Template, transactional outbox, lease/retry và vận chuyển email | `MailTransport`: SMTP hoặc logging |
| `payment` | Invoice, webhook và cổng thanh toán | `PaymentGateway` |
| `finance` | Wallet, ledger, refund, payout và mã hóa tài khoản | `AccountNumberProtector`, finance commands |
| `booking` | Tạo, hoàn thành, hủy và đọc lịch học | command service và `BookingReadService` |
| `learning` | Assignment, submission, attachment và mapping view | `AssignmentAttachmentBinder`, `AssignmentViewMapper` |
| `communication` | Chat, notification và realtime | command/query service, event `AFTER_COMMIT` |

Các side effect ra SMTP, WebSocket và notification chỉ chạy sau khi transaction nghiệp vụ commit. Các invariant về tiền, ledger, lượt học và lock order vẫn nằm trong transaction của module sở hữu.

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

Mail dùng `APP_EMAIL_PROVIDER=logging` khi phát triển không cần SMTP và `smtp` khi kiểm thử Mailpit/Gmail test. Outbox luôn được ghi trong transaction; delivery job claim bằng lease, gửi ngoài transaction giữ database lock rồi đánh dấu thành công hoặc retry.

## Kiểm thử và guardrail

Từ thư mục `backend`:

```powershell
mvn clean test -B --no-transfer-progress -Dfile.encoding=UTF-8
```

ArchUnit kiểm tra dependency module, controller/repository, domain/layer, vị trí vendor SDK, nguồn cấu hình và interface của external adapter. Integration tests dùng Testcontainers PostgreSQL/Redis; cần Docker daemon hoạt động. Không dùng `flyway repair`, không sửa migration đã áp dụng và không reset database để làm test xanh.
