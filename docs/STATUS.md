# Trạng thái dự án

> Cập nhật: 2026-09-11. Đây là ảnh chụp hiện tại, không phải nhật ký append-only.
> Phạm vi snapshot: code Student Journey đang có trong working tree; các thay đổi chưa được commit vẫn được đánh dấu là chưa nghiệm thu đầy đủ.

## Mốc kỹ thuật

- Backend modular monolith đã có các seam chính cho auth, mail, payment, finance, booking, learning và communication.
- Migration mới nhất trong working tree: `V28__optimize_teacher_marketplace_search.sql` (V26/V27 thuộc Student Journey).
- Không dùng Flyway `repair()`, không sửa migration đã áp dụng và không reset database người dùng.

## Trạng thái theo luồng

| Luồng | Trạng thái hiện tại | Ghi chú |
|---|---|---|
| Auth/verification/logout | Đã triển khai thay đổi Student Journey | Cần chạy full integration với Docker |
| Invoice snapshot/payment | Đã triển khai V26 | Chưa nghiệm thu DB sạch và DB nâng cấp |
| Trial/package/booking/review | Đã triển khai phần Student Journey | Cần kiểm thử xuyên luồng |
| Assignment/attachment | Đã triển khai endpoint và view mới | Cần kiểm thử file thật |
| Chat/notification/events | Đã triển khai event và mở conversation | Smoke test local đã xác nhận gửi/nhận giữa Student và Teacher qua STOMP tới backend `:8080`; cảnh báo browser extension không thuộc ứng dụng |
| Teacher search/catalog | Đã triển khai V28, query/count/cache/config | Focused tests và PostgreSQL Testcontainers pass; V28 đã áp dụng trên Supabase; cloud smoke và load test chưa xác minh |
| Finance/refund/payout/package | Đã harden domain, contract/UI và V29 local | Domain/backend focused `20/20`, frontend finance Jest `6 suites/22 tests`, typecheck pass; Flyway V1→V29 và V27→V29 pass trên PostgreSQL Testcontainers; V29 Supabase, idempotency executor, concurrency/rollback và full suite chưa xác minh |
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

### Teacher search/catalog

- Keyword public search được trim, giới hạn 100 ký tự và normalize không phân biệt dấu/hoa thường.
- Data query tính `min_price` một lần bằng lateral aggregate; count query bỏ projection, subject hydration và sort.
- V28 thêm immutable unaccent wrapper, hai partial GIN trigram indexes và partial package-price index.
- Search cache TTL 5 phút, chỉ áp dụng page 0–2 và `size <= 50`; profile cache TTL 30 phút; Redis lỗi fallback PostgreSQL.
- Hikari có các timeout cố định và `DB_POOL_MAX_SIZE` cấu hình được. Giá trị mặc định 10 chỉ là candidate, chưa phải kết quả capacity planning.
- Scheduler có thể tắt riêng bằng `APP_SCHEDULING_ENABLED=false` cho cloud smoke; mặc định vẫn bật.
- Cloud smoke script dùng servlet mode bình thường và không đọc hoặc in credential.

## Bằng chứng kiểm thử gần nhất

- Backend focused Student tests: `36/36` pass.
- Frontend typecheck: pass.
- Frontend lint: pass.
- Frontend build: pass.
- CI regression focused suite (`StudentInvoiceControllerTest`, `ArchitectureTest`, `SolidGuardrailsArchitectureTest`): `9/9` pass sau khi bỏ mapping assignment trùng, sửa principal test và chuyển package read controller về module enrollment.
- GitHub Backend CI full Maven/Testcontainers: `325/325` pass, `0` failure, `0` error, `0` skipped tại run `34558136980`.
- Docker image build validation trong cùng run: pass.
- Full backend suite local với PostgreSQL 16/Redis Testcontainers: `341/341` pass, `0` failure, `0` error, `0` skipped; đã re-check sau khi Docker Engine hoạt động.
- Scheduler toggle focused test: `3/3` pass. Lần `mvn clean verify` sau thay đổi không được ghi nhận là pass do Maven kết thúc exit code `1`; lần chạy lại các test Testcontainers bị chặn vì Docker Desktop mất Docker socket.
- Jest/Playwright cho Student Journey: **chưa xác minh trong lượt này**.
- Teacher-search focused validation/cache/serialization + architecture guardrails: pass local (`15/15`).
- Teacher-search PostgreSQL 16 Testcontainers: repository regression + EXPLAIN/index assertions `4/4` pass; Flyway clean schema, metadata và V27 → V28 upgrade `9/9` pass.
- Supabase V28 migration bằng đúng Flyway connection/user: pass; Flyway validated 28 migrations, current version V27 và apply V28 thành công trên PostgreSQL 17.6. Migration DO preflight xác nhận `unaccent`/`pg_trgm` ở `public`.
- Cloud application HTTP smoke sau migration: **chưa xác minh**. Cách chạy `web-application-type=none` trước đây không hợp lệ cho OAuth servlet; smoke script mới yêu cầu chạy web mode với `APP_SCHEDULING_ENABLED=false`.
- Cold-cache load test 25/50/80/100 users với pool 5/8/10 và warm-cache benchmark: **chưa chạy**; chưa có bằng chứng đạt các p95 mục tiêu.

Các con số trên chỉ là bằng chứng gần nhất đã có; benchmark và cloud smoke chưa được gọi là pass khi chưa chạy thật.

## Việc đang chờ

1. Khi cần đối chiếu local, mở Docker và chạy `scripts/test-student-journey-docker.ps1`.
2. Nếu cần kiểm thử nâng cấp riêng, xác nhận V26/V27 trên bản sao Supabase test; Supabase chính đã ở V28.
3. Chạy smoke test các role Student, Teacher và Admin.
4. Cập nhật bảng này bằng số liệu thật sau mỗi lần chạy.
5. Có thể chạy lại `scripts/preflight-teacher-search-extensions.sql` bằng Flyway user để bổ sung bằng chứng standalone; không deploy lại V28.
6. Benchmark bằng `node scripts/benchmark-teacher-search.mjs` với từng pool candidate; ghi riêng cold-cache và warm-cache.

## Provider chưa xác minh

Google OAuth thật, PayOS thật, Cloudinary thật, SMTP tới người dùng thật và production deployment chưa được tính là pass trong trạng thái này.
