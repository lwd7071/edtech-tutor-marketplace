# Backend Implementation Plan

Kế hoạch triển khai Backend trong 8 tuần cho hai thành viên Full-stack, bám theo `SPEC.md`, `API_CONTRACT.md`, `ERD.md` và `ERROR_CODES.md`.

## 1. Phân chia ownership

### Thành viên A — Identity & Experience

Sở hữu các module:

- `common`: response envelope, exception, validation, security utilities và domain event contract.
- `auth`, `user`: xác thực, tài khoản và thông tin Student.
- `teacher`, `subject`: onboarding, hồ sơ Teacher, môn học và lịch rảnh.
- `catalog`: PricingPackage và public marketplace.
- `learning`: Assignment, Submission và Attachment.
- `communication`: Conversation, Message và Notification.
- `ranking`: Review, TeacherStats và Bayesian ranking.

### Thành viên B — Transaction & Operations

Sở hữu các module:

- `enrollment`: StudentPackage và session counters.
- `payment`: Invoice, payOS và webhook.
- `booking`: Booking, TrialRequest và SessionReport.
- `finance`: Wallet, Ledger, BankAccount, Payout, Refund và Extension.
- `admin`: approval queues, moderation, settings, dashboard và audit log.
- `scheduler`: reminder, expiration và scheduled jobs.
- Flyway migration, seed data, application config, Docker và Testcontainers.

### Quy tắc chống giẫm chân

- Không sửa package thuộc ownership của người còn lại.
- PricingPackage thuộc A; Invoice và StudentPackage thuộc B.
- Module chỉ giao tiếp qua public facade, DTO hoặc domain event; không gọi repository của module khác.
- B là người duy nhất tạo hoặc sửa Flyway migration để không trùng version và thứ tự dependency.
- Thay đổi `common`, API contract hoặc schema phải tách thành PR riêng và merge trước feature PR.
- Mỗi feature dùng một branch ngắn, ví dụ `feat/auth-login`, `feat/booking-create`.
- Người sở hữu feature chịu trách nhiệm controller, DTO, service, repository và test của feature đó.

## 2. Kế hoạch 8 tuần

### Tuần 1 — Foundation

**A**

- Chuẩn hóa root package và cấu trúc module theo domain.
- Xây dựng BaseEntity, response envelope, error handler và validation.
- Tạo security skeleton, role/ownership utilities và event interface.

**B**

- Hoàn thành Docker Desktop/WSL, PostgreSQL, Redis và Docker Compose v2.
- Tạo baseline Flyway từ ERD, gồm constraint và index.
- Thiết lập Testcontainers, seed profile và CI build/test.
- Chuyển secret và cấu hình môi trường sang environment variables.

**Checkpoint:** `docker compose up` thành công; migration chạy được trên database rỗng; context test xanh.

### Tuần 2 — Auth và Teacher Onboarding

**A**

- Register, login, refresh, logout, verify email và reset password.
- Google OAuth2 completion flow.
- User/Student parent contact.
- TeacherProfile, TeacherDocument, TeacherSubject và SubjectProposal.
- Subject catalog và trạng thái onboarding.

**B**

- Admin approve/reject Teacher và SubjectProposal.
- User moderation cơ bản.
- Audit log cho mọi action thay đổi trạng thái.

**Checkpoint:** Teacher đăng ký → gửi hồ sơ → Admin duyệt → Teacher được phép bán gói.

### Tuần 3 — Marketplace

**A**

- TeacherAvailability và kiểm tra overlap.
- PricingPackage CRUD và state transition.
- Public subject, teacher detail, package, availability và review API.
- Search/filter theo môn, giá, rating, delivery mode và thời gian rảnh.
- Review, Bayesian ranking foundation và Redis cache.

**B**

- Chuẩn bị Invoice, StudentPackage và Wallet schema/service boundary.
- Chỉ sử dụng catalog facade của A để đọc snapshot PricingPackage.

**Checkpoint:** Student tìm được Teacher và xem đầy đủ hồ sơ, lịch rảnh và gói học.

### Tuần 4 — Payment và StudentPackage

**B**

- Invoice state machine và tạo payOS checkout link.
- Xác minh webhook signature, amount và idempotency.
- Kích hoạt StudentPackage đúng một lần.
- Tạo Wallet/Ledger pending balance và commission snapshot.
- API polling trạng thái Invoice.

**A**

- Consumer gửi notification/email khi thanh toán thành công.
- Cache invalidation khi package hoặc trạng thái Teacher thay đổi.

**Checkpoint:** Invoice `PAID` → StudentPackage `ACTIVE` → Wallet/Ledger cân bằng.

### Tuần 5 — Booking

**B**

- Booking, TrialRequest và SessionReport.
- PostgreSQL conflict constraint và pessimistic locking.
- Tạo, hủy và hoàn thành Booking.
- Bảo vệ StudentPackage counter invariant.
- Auto-expire, package expiry và reminder scheduler.
- Settlement pending → available sau khi hoàn thành buổi học.

**A**

- Notification/email cho Booking và phụ huynh.
- Chỉ cho Review sau Booking `COMPLETED`.
- Cập nhật TeacherStats/ranking từ domain event.

**Checkpoint:** không double-booking hoặc trừ buổi hai lần; cancelled/expired hoàn lượt chính xác.

### Tuần 6 — Learning và Communication

**A**

- Assignment, Submission, grading và content blocks.
- Attachment upload, MIME validation và ownership authorization.
- Conversation, Message và WebSocket/STOMP.
- Notification center, read/read-all và membership checks.

**B**

- Cung cấp Booking/StudentPackage authorization facade cho Learning và Chat.
- Không chỉnh implementation nội bộ của `learning` hoặc `communication`.

**Checkpoint:** hoàn chỉnh luồng giao/nộp/chấm bài; chat chỉ mở cho cặp Student–Teacher hợp lệ.

### Tuần 7 — Finance, Ranking và Admin

**B**

- BankAccount, Payout, Refund và Extension state machine.
- Reserve/release Wallet và upload chứng từ chuyển khoản.
- Admin queues, settings, dashboard và audit logs.
- Hoàn thiện các scheduled job còn lại.

**A**

- TeacherStats, Bayesian ranking và Teacher dashboard statistics.
- Public global ranking và cache invalidation.

**Checkpoint:** payout/refund không làm âm Wallet; dashboard đối soát được GMV, commission và Booking.

### Tuần 8 — Hardening

**A**

- Unit/security test cho auth, RBAC, ownership và ranking.
- Kiểm tra IDOR cho Attachment, Conversation và public/private DTO.

**B**

- Integration test cho migration, locking, webhook idempotency và ledger.
- Kiểm tra migration từ database rỗng và đường nâng cấp schema.

**Cả hai**

- Seed dữ liệu demo.
- Tối ưu query, index và Redis cache.
- Docker hóa backend và chạy acceptance test end-to-end.

## 3. Definition of Done

Một Backend feature chỉ hoàn thành khi:

- Endpoint và DTO khớp `API_CONTRACT.md`.
- Validation, error code, RBAC và ownership đúng đặc tả.
- State transition và transaction boundary được kiểm thử.
- Không truy cập repository xuyên module.
- Có unit test và integration test phù hợp rủi ro.
- Migration chạy được từ database rỗng.
- Build và test toàn bộ backend thành công.

## 4. Thứ tự tích hợp

1. Foundation và database baseline.
2. Auth/onboarding.
3. Marketplace/PricingPackage.
4. Payment/StudentPackage.
5. Booking/SessionReport.
6. Learning/Communication.
7. Finance/Ranking/Admin.
8. Hardening và demo.

Tích hợp vào nhánh chính cuối mỗi tuần; không giữ feature branch đến tuần 8.
