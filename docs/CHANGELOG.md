# Changelog theo đợt hoàn thành

## 2026-09-17 — Sửa kiểm tra metadata Flyway trên CI

- Giới hạn ba truy vấn `pg_constraint`/`pg_trigger` trong `FlywayMigrationTest` vào đúng bảng thuộc schema `public`, tránh đếm đối tượng từ các schema thử nghiệm cùng database. Run `35169842151` trước sửa có 472/474 test pass; run `35170433438` sau sửa pass 474/474 test, Compose startup smoke và Docker image build. Không thay đổi migration hoặc schema production.

## 2026-09-15 — Hardening settlement V41 (working tree)

- Giữ `ledger_entries.entry_type` ở `varchar(30)` bằng tên `SESSION_ESCROW_HELD`/`SESSION_ESCROW_RELEASED`, thêm backfill settlement cho booking trả phí cũ, bật RLS cho `provinces`/`wards`, đồng bộ `netAmountVnd` và sửa admin queue dùng `bookingId`.
- Booking unit tests pass; migration/Testcontainers chưa xác minh do Docker engine không có socket hợp lệ. Supabase V41 vẫn Pending; chưa apply production.

## 2026-09-14 — Tiếp tục hoàn thiện xác nhận hai bên và escrow V41 (working tree)

- Tách trạng thái tiêu thụ lượt khỏi quyền nhận tiền; bổ sung xác nhận hai bên, timeout giữ hộ, khiếu nại/mở lại một lần và quyết định admin. Ghi debit/credit ở cả ledger ví và platform ledger; hiển thị số tiền đang giữ riêng khỏi số có thể rút.
- Bổ sung hàng đợi admin, trạng thái/hạn xác nhận trên booking và các kiểm tra migration V40→V41. Frontend typecheck, lint, production build và docs check pass; backend focused và PostgreSQL integration đang được kiểm tra lại sau sửa cuối. Supabase read-only xác nhận V40 `Success`, V41 `Pending`; chưa deploy V41.

## 2026-09-14 — Hoàn tất bốn phần hardening Luna & On-demand Next Data Cache Revalidation

- Supabase production đã áp dụng thành công toàn bộ migration V38–V40 (`State = Success`); kiểm tra read-only xác nhận 0 bản ghi `evidence_format = ''`, giữ nguyên schema V40 và không cần tạo V41.
- Sửa `evidence_format` đồng nhất cho cả create và update bằng MIME do Apache Tika xác nhận (`jpg`, `png`, `pdf`), không phụ thuộc extension hay Content-Type client gửi.
- Bổ sung bộ regression tests credential toàn diện:
  - Unit tests: `TeacherCredentialServiceTest` (15/15 pass) kiểm tra MIME mapping, chặn file rỗng/quá khổ/extension giả trước khi upload, audit trail và concurrency.
  - Controller contract tests: `TeacherCredentialControllerTest` (4/4 pass) và `AdminCredentialControllerTest` (4/4 pass) kiểm tra HTTP status, envelope, phân quyền admin và `Cache-Control: no-store`.
  - Integration tests: `TeacherCredentialIntegrationTest` (5/5 pass) trên PostgreSQL Testcontainers kiểm tra xử lý xung đột đồng thời trả 409 `CONCURRENT_MODIFICATION`, rollback storage cleanup xóa proof mới, after-commit cleanup xóa proof cũ, và public detail chỉ trả badge ID/label đã duyệt (không lộ URL/bytes).
  - Architecture guardrails: `ArchitectureTest` và `SolidGuardrailsArchitectureTest` (10/10 pass).
- Kích hoạt Next Data Cache on-demand tag revalidation:
  - Route Handler nội bộ `POST /api/internal/revalidate-public` với shared secret header `x-internal-secret` (`INTERNAL_REVALIDATE_SECRET`, không dùng `NEXT_PUBLIC_*`) và `revalidateTag(tag, { expire: 0 })` chuẩn Next 16.
  - Mapping tag: duyệt hoặc sửa/xóa credential đã duyệt invalidate `public-teacher:{id}`; đổi nơi ở invalidate `public-teacher:{id}` và `public-teachers`.
  - Backend gửi callback sau commit (`afterCommit`) qua `PublicCacheRevalidationClient` (3/3 pass), lỗi callback được log an toàn không làm hỏng command, transaction rollback không gửi sự kiện; giữ nguyên TTL 60–300s làm fallback.
  - Next.js production build pass 62 static/SSG pages và dynamic route handler.
- Kết quả kiểm thử nghiệm thu toàn diện:
  - Backend: `mvn test` pass `466/466` tests (`0` failure, `0` error, `0` skipped) trên PostgreSQL 16 và Redis Testcontainers.
  - Frontend: `npm run check` trong Docker pass toàn bộ (typecheck, lint, `101/101` test suites, `264/264` tests và Next.js production build).

## 2026-09-14 — Credential badges V40 (local)

- Thêm credential teacher với proof Cloudinary authenticated, workflow teacher CRUD và admin approve/reject; public chỉ hiển thị label đã duyệt.
- Giới hạn 10 credential/teacher, file JPG/PNG/PDF tối đa 10MB; local Flyway validate/clean tới V40 pass. Chưa preflight/apply Supabase.

## 2026-09-14 — Thêm reference địa giới hành chính V38 (local)

- Thêm `provinces` và `wards` theo cấu trúc hành chính Việt Nam 2025 (34 tỉnh/thành, 3.321 xã/phường), seed từ Open Admin Data.
- Local Flyway clean/upgrade tới V38 và kiểm tra cardinality/FK/code format đã pass; chưa triển khai Supabase production.

## 2026-09-14 — Thêm nơi ở một tỉnh/xã cho teacher profile V39 (local)

- Thêm `province_code`/`ward_code` nullable và FK composite bảo đảm xã thuộc đúng tỉnh.
- Thêm endpoint `PUT /api/teacher/profile/residence`; đổi nơi ở không làm profile `APPROVED` quay về `DRAFT`.
- Local Flyway clean/upgrade tới V39 và focused teacher profile/Flyway tests pass; chưa triển khai Supabase production.

## 2026-09-14 — Public location API và teacher search filter (local)

- Thêm API cascading tỉnh/xã và filter search trực tiếp trên `teacher_profiles.province_code`/`ward_code`.
- Cache key teacher search bao gồm location; card projection trả tên tỉnh/xã; focused search tests pass.

## 2026-09-14 — Frontend cascading location filter (local)

- Thêm dropdown tỉnh → xã cho hồ sơ teacher và trang tìm kiếm public; card hiển thị khu vực tổng quát.
- Typecheck pass; Jest Windows host còn bị `spawn EPERM`, chưa xác nhận test pass trong lượt này.

## 2026-09-14 — Triển khai RLS V37 Cụm System, Outbox & Audit Logs lên Supabase Production

- Thêm migration `V37__enable_rls_system_and_cleanup.sql` kích hoạt Row Level Security default-deny cho các bảng System, Outbox & Audit Logs: `platform_settings`, `audit_logs`, `email_outbox`, `modulebentity`.
- Thu hồi toàn bộ quyền (REVOKE ALL bao gồm TRUNCATE) từ `anon` và `authenticated` trên toàn bộ các bảng này.
- Bổ sung kiểm thử TDD: mở rộng `FlywayMigrationTest` (21 tests, clean V1->V37, upgrade V27..V36->V37, assert 33 bảng nghiệp vụ có RLS), mở rộng `RlsBehaviorVerificationTest` (chặn TRUNCATE và truy cập unprivileged trên `platform_settings`, `audit_logs`, `email_outbox`), tạo mới `SystemOutboxRlsFlowIntegrationTest` (xác nhận luồng cấu hình singleton, ghi audit log, đưa email vào outbox và worker gửi mail qua PostgreSQL superuser).
- Verification: 29/29 tests local Testcontainers pass; preflight Supabase read-only validate 37 migrations pass; apply V37 thành công trên Supabase production (`State = Success`), post-migrate validation pass.
- Đối soát dữ liệu trên Supabase: 100% dữ liệu được bảo toàn (`platform_settings`: 1 row, `flyway_schema_history`: 37 rows); toàn bộ 34 bảng nghiệp vụ trong schema `public` đã được bảo vệ bởi RLS (0 data loss). Hoàn tất trọn vẹn toàn bộ 5 nhóm RLS migration!

## 2026-09-14 — Triển khai RLS V36 Cụm Communication & Notifications lên Supabase Production

- Thêm migration `V36__enable_rls_communication_tables.sql` kích hoạt Row Level Security default-deny cho 4 bảng: `conversations`, `messages`, `attachments`, `notifications`.
- Khắc phục lỗ hổng RLS bypass qua `TRUNCATE` bằng khối `REVOKE TRUNCATE ... FROM anon, authenticated`.
- Bổ sung kiểm thử TDD: mở rộng `FlywayMigrationTest` (20 tests, clean V1->V36, upgrade V27..V35->V36, assert 30 bảng có RLS), mở rộng `RlsBehaviorVerificationTest` (chặn TRUNCATE và truy cập unprivileged trên `conversations`, `messages`, `notifications`), tạo mới `CommunicationRlsFlowIntegrationTest` (xác nhận luồng hội thoại, tin nhắn văn bản, file attachment, thông báo và trạng thái đã đọc trơn tru qua PostgreSQL superuser).
- Verification: 27/27 tests local Testcontainers pass; preflight Supabase read-only validate 36 migrations pass; apply V36 thành công trên Supabase production (`State = Success`), post-migrate validation pass.
- Đối soát dữ liệu trên Supabase: baseline 9 rows được bảo toàn 100% (`conversations`: 3, `messages`: 3, `attachments`: 0, `notifications`: 3) (0 data loss).

## 2026-09-14 — Triển khai RLS V35 Cụm Booking & Learning lên Supabase Production

- Thêm migration `V35__enable_rls_booking_learning_tables.sql` kích hoạt Row Level Security default-deny cho 9 bảng: `student_packages`, `package_extension_requests`, `trial_requests`, `bookings`, `session_reports`, `reviews`, `teacher_stats`, `assignments`, `submissions`.
- Khắc phục lỗ hổng RLS bypass qua `TRUNCATE` bằng khối `REVOKE TRUNCATE ... FROM anon, authenticated`.
- Bổ sung kiểm thử TDD: mở rộng `FlywayMigrationTest` (19 tests, clean V1->V35, upgrade V27..V34->V35, assert 26 bảng có RLS), mở rộng `RlsBehaviorVerificationTest` (chặn TRUNCATE và truy cập unprivileged trên `bookings`, `student_packages`, `reviews`), tạo mới `BookingLearningRlsFlowIntegrationTest` (xác nhận luồng mua gói, đặt lịch, session report, review/stats, assignment/submission chạy trơn tru qua PostgreSQL superuser).
- Verification: 27/27 tests local Testcontainers pass; preflight Supabase read-only validate 35 migrations pass; apply V35 thành công trên Supabase production (`State = Success`), post-migrate validation pass.
- Đối soát dữ liệu trên Supabase: baseline 32 rows được bảo toàn 100% (teacher_stats: 20, bookings: 3, student_packages: 3, trial_requests: 2, session_reports: 2, reviews: 2, các bảng còn lại: 0) (0 data loss).

## 2026-09-14 — Triển khai RLS V34 Cụm Finance & Payments lên Supabase Production

- Thêm migration `V34__enable_rls_finance_tables.sql` bật Row Level Security default-deny cho 8 bảng: `wallets`, `ledger_entries`, `invoices`, `payment_transactions`, `payout_requests`, `refund_requests`, `teacher_bank_accounts`, `finance_command_receipts`.
- Khắc phục lỗ hổng RLS bypass qua `TRUNCATE` bằng khối an toàn `REVOKE TRUNCATE ... FROM anon, authenticated`.
- Bổ sung kiểm thử TDD: mở rộng `FlywayMigrationTest` (18 tests, clean V1->V34, upgrade V27..V33->V34), mở rộng `RlsBehaviorVerificationTest` (chặn TRUNCATE và truy cập unprivileged trên `teacher_bank_accounts`), tạo mới `FinanceRlsFlowIntegrationTest` (xác nhận luồng ví, invoice, ledger và idempotency receipt chạy qua PostgreSQL superuser bypass RLS).
- Verification: 24/24 tests local Testcontainers pass; preflight Supabase read-only validate 34 migrations pass; apply V34 thành công trên Supabase production (`State = Success`), post-migrate validation pass.
- Đối soát dữ liệu trên Supabase: baseline 32 rows được bảo toàn 100% (wallets: 20, ledger_entries: 4, invoices: 4, teacher_bank_accounts: 4; 4 bảng còn lại: 0) (0 data loss).

## 2026-09-14 — Triển khai RLS V33 Bảng users lên Supabase Production

- Thêm migration `V33__enable_rls_users.sql` kích hoạt Row Level Security default-deny cho bảng gốc `users`.
- Bổ sung test tích hợp xuyên bảng `UserRlsCrossTableFlowIntegrationTest` (luồng `users` -> `teacher_profiles` -> `pricing_packages` -> `invoices` -> `student_packages` -> `bookings` trên Testcontainers).
- Verification: clean/upgrade Flyway tests và test luồng pass 100%; apply V33 thành công trên Supabase production (`State = Success`), post-migrate validation pass.
- Đối soát dữ liệu: baseline 55 users được bảo toàn 100% (0 data loss).

## 2026-09-14 — Triển khai RLS V32 Bảng Backend-owned lên Supabase Production

- Thêm preflight read-only kiểm kê grants/role/RLS cho 9 bảng và migration `V32__enable_rls_public_tables.sql` theo mô hình default-deny, không policy public và không `FORCE ROW LEVEL SECURITY`.
- Cập nhật Flyway tests lên V32, thêm kiểm tra 9 bảng bật RLS và test local Backend/unprivileged role với cleanup sau mỗi test.
- Script Supabase yêu cầu xác nhận backup và quyền apply riêng của Đông trước khi chạy production. Chưa kết nối hoặc mutate Supabase production.
- Verification focused: Flyway `14/14`, RLS behavior `2/2` pass; Supabase read-only preflight V31→V32 pass, không apply. Backup/snapshot, production apply và Security Advisor verification còn chờ Đông; full backend sau V32 chưa hoàn tất do Maven test runner treo trên Windows.

## 2026-09-14 — Sửa lỗi 500 khi hoàn tất đăng ký Google OAuth

- Khắc phục lỗi `SerializationException` khi hoàn tất đăng ký OAuth (`POST /api/auth/oauth2/complete-registration`): chuyển payload lưu trong Redis từ `Map` sang `OAuthIdentity` record để `GenericJackson2JsonRedisSerializer` gắn `@class` và deserialize an toàn.
- Thêm unit test và edge case cho `OAuthAccountService` (5 ca kiểm thử).
- Thêm regression test cho Redis serialization của `OAuthIdentity` (2 ca kiểm thử).
- Thêm controller contract test cho `complete-registration` (2 ca kiểm thử).
- Verification: 13/13 OAuth focused tests pass, 10/10 Architecture guardrails pass. Không thay đổi schema DB.

## 2026-09-14 — Tối ưu Student dashboard và public cache

- Thêm `GET /api/student/dashboard` dùng một SQL projection, thay cho bảy request riêng ở Student dashboard; React Query giữ cache 30 giây và invalidate sau mutation liên quan.
- Public landing, catalog, teacher detail và ranking chuyển sang server-only fetch với Next ISR/Data Cache TTL 60–300 giây; ranking initial data không còn tải lại bằng hai `useEffect`.
- Verification: full backend Maven/Testcontainers `385/385` pass, frontend Docker `100/100` Jest suites và `255/255` tests pass, typecheck/lint/Next production build pass. Build xác nhận `/` ISR 300s, `/ranking` ISR 60s và `/teachers/[id]` on-demand ISR; Vercel Preview smoke chưa chạy. Host Windows Jest gặp `spawn EPERM`, không ảnh hưởng Docker verification.

## 2026-09-14 — Ổn định startup contract Backend

- Sửa regression CI của `FlywayMigrationTest`: giới hạn truy vấn constraint V31 vào schema hiện tại để các schema tạm không tạo kết quả trùng trên Linux runner.
- Giữ `cloud` làm profile mặc định và tự nạp `.env.cloud`; tách OAuth, Cloudinary và account-encryption theo local/test/cloud để từng môi trường có contract rõ ràng.
- Cloudinary cloud nhận ba credential rời; thêm preflight báo key thiếu mà không in secret; hợp nhất cấu hình test về một nguồn canonical.
- Đồng bộ Docker image/Compose profile, sửa healthcheck sang `/actuator/health`, sửa tên biến trong script test và thêm Compose startup smoke vào Backend CI.
- Verification: config contract `9/9`, full backend Maven/Testcontainers `374/374`, Compose syntax/image build/startup health đều pass. Cloud smoke chưa chạy vì còn thiếu `EDTECH_ACCOUNT_ENCRYPTION_KEY` trong `.env.cloud`.

## 2026-09-14 — Hoàn tất regression hardening Luna

- Khóa ownership package bằng query `id + student_id`, map unknown/foreign ID thành `404 RESOURCE_NOT_FOUND`; stale version dùng `409 CONCURRENT_MODIFICATION`.
- Complete refund/payout nhận multipart metadata + proof server-owned, kiểm tra MIME/size và fingerprint file cho idempotency; thêm cleanup khi rollback.
- Bổ sung audit atomic cho refund/payout/extension/platform settings và migration append-only V31 yêu cầu chứng từ ở terminal state.
- Verification: full backend Maven/Testcontainers `370/370` pass; Flyway clean V1→V31 và upgrade V30→V31 pass; Supabase preflight sạch, V31 apply/validate/info báo `Success`. Frontend typecheck pass; focused finance API Jest chạy qua `cmd` `7/7` pass; full Jest chưa chạy.

## 2026-09-13 — Apply V30 lên Supabase và khóa quy trình migration

- Thêm Flyway Maven plugin và `scripts/update-supabase-schema.ps1` với preflight version/checksum, guard migration phá hủy và post-migrate validation; secret chỉ đọc từ `.env.cloud`, không in ra log.
- Bổ sung rule Supabase vào `AGENTS.md`: code/docs-only không mutate database; migration tương thích ngược phải test, preflight, apply và ghi evidence.
- Flyway cloud preflight xác nhận V29; `V30__fix_data_constraints_and_locking` đã apply thành công, Supabase hiện ở V30 và validate 30 migrations pass.
- Flyway Testcontainers focused run trong lượt này bị blocked vì Docker daemon không có `dockerDesktopLinuxEngine`; không tính blocked run là pass.

## 2026-09-13 — Sửa regression contract/version sau review

- Khôi phục request contract cho teacher rejection và cho phép `ApiResponse<Void>` thành công đi qua Axios interceptor.
- Siết version bắt buộc, phân biệt đúng `CONCURRENT_MODIFICATION`, refetch dữ liệu stale, validate chặt `If-Match` và HTTP/HTTPS profile URL.
- Bổ sung regression/config tests và đồng bộ API contract/ERD; full backend `366/366`, frontend `97/97` suites và `252/252` tests cùng Next build đều pass.

## 2026-09-13 — Clean verification

- `mvn clean test` với Docker/Testcontainers thật pass `361/361`, không sửa migration V1–V30; docs check vẫn pass.

## 2026-09-12 — Verification optimistic-lock và frontend contract

- Hoàn tất narrowing `ApiResponse.data` nullable qua `requireApiData`; frontend gửi version cho các public mutation và xử lý conflict không retry.
- Frontend full check pass (`97/97` suites, `248/248` tests, build); backend focused non-container `29/29` pass.
- Ghi rõ các test integration/Flyway bị chặn do Docker Desktop Linux engine chưa cung cấp socket; không coi compile/static check là integration pass.

## 2026-09-12 — Full verification sau khi Docker hoạt động

- Sửa test profile Cloudinary để integration context dùng credential giả chỉ trong test; giữ base/cloud fail-fast.
- Sửa bank-account delete kiểm tra ownership trước `If-Match`, cập nhật fixture trial/bank contract và xác minh IDOR.
- Thêm test validation biên cho Teacher Profile (`3/3`), kiểm tra giới hạn/nullability/XSS/URL theo contract.
- Siết architecture contract để kiểm tra generic `ApiResponse`, allowlist đúng từng webhook/health method, và loại hai error code token-expired không thể phát sinh.
- Full backend `361/361`, finance idempotency `2/2`, Flyway `12/12`, frontend `248/248` và docs check đều pass.

Các entry dưới đây ghi hành vi và bằng chứng quan trọng. Danh sách file đầy đủ nằm trong Git history.

## 2026-09-12 — Optimistic-lock contract và zero-trust storage

- Thêm version contract cho các public mutation của trial, learning, subject proposal và teacher bank account; stale version trả `409 CONCURRENT_MODIFICATION`.
- Thêm guardrail controller không phụ thuộc JPA entity, validation teacher profile và Cloudinary fail-fast ở base/cloud.

## 2026-09-12 — Fix lỗi bảo mật và phân quyền (Mục 5)

- Sửa lỗi Authorization bằng cách thiết lập lại `RequireRoleAspect.java` sử dụng cả `@within` và `@annotation` pointcut, giúp đảm bảo việc bảo vệ endpoint khi annotation `@RequireRole` được đánh dấu ở cấp độ Class.
- Xóa bỏ toàn bộ các default secrets cứng (hardcode) trong `application.yml` (như JWT, Encryption key, Google Client Secret) để bắt buộc ứng dụng phải crash (fail-fast) nếu DevOps quên cấu hình biến môi trường production.
- Áp dụng Zero-Trust Secrets policy và ghi nhận quyết định này tại ADR `0007-enforce-zero-trust-secrets.md`.
- Thêm cơ chế filter XSS bằng `@Pattern` Regex để ngăn ngừa chèn mã độc HTML tại các text request nhạy cảm (`CreateReviewRequest`, `UpdateTeacherProfileRequest`, `CreateSubjectProposalRequest`).
- Xác minh bằng cách chạy lại toàn bộ integration test, `SecurityIdorIntegrationTest` và Testcontainers: `350/350` tests pass thành công.

## 2026-09-12 — Sửa lỗi dữ liệu/migration

- Thêm `@Version` (Optimistic Locking) cho các entity: `Invoice`, `TrialRequest`, `Assignment`, `Submission`, `SubjectProposal`, `TeacherBankAccount` để chống race condition.
- Cập nhật `@SQLDelete` cho các entity có version để tương thích với soft-delete.
- Tạo file migration `V30__fix_data_constraints_and_locking.sql` chỉ chứa cấu trúc thêm cột `version` cho các bảng cần thiết, tuân thủ không sửa file `V1`-`V29`.
- Toàn bộ suite test 350/350 (bao gồm Testcontainers kiểm thử migration) đều pass.

## 2026-09-12 — Siết chặt architecture boundaries và module isolation

- Sửa lỗi Entity Leakage ở module Payment và Booking bằng cách sử dụng `InvoiceDetail` và `TrialRequestView` thay vì trả về domain entity từ Controller.
- Sửa lỗi Cross-Module Coupling bằng cách chuyển `AdminExtensionController`, `AdminPayoutController`, `AdminRefundController` sang module `finance`, và `TeacherSubjectProposalController` sang module `subject`.
- Đảm bảo Abstraction đúng chuẩn: Đóng gói SDK qua `PaymentGateway` port thay vì gọi thẳng SDK từ Service.
- Thêm ADR `0006-strict-layer-and-module-isolation.md` để ghi nhận quyết định cấm Controller truy cập Entity và cấm gọi chéo module.
- Các test backend liên quan đã pass.

## 2026-09-12 — Đồng bộ API contract Backend/Frontend

- Chuẩn hóa invoice DTO/request, availability `items` (giữ alias cũ), pricing package optimistic locking và error-code mapping.
- Siết architecture envelope/status contract, bổ sung parity check `ErrorCode` ↔ `ERROR_CODES.md`.
- Focused backend contract suite `26/26` pass; full backend Maven/Testcontainers `350/350` pass; frontend typecheck pass; Jest contract runner chưa xác minh do bị treo.

## 2026-09-11 — Finance state, contract và V29 hardening

- 2026-09-14: Residence/credential lifecycle hardening — credential version fields and 409 contract, audit hooks, MIME+extension validation, rollback/after-commit asset cleanup, and UI version propagation added. Full backend `mvn clean verify` 438/438 pass; admin pagination, Next tag revalidation and real Cloudinary upload smoke remain unverified.

- Đơn giản hóa transition Refund/Payout và siết invariant counter của StudentPackage; focused backend finance `20/20` pass.
- Đồng bộ payload/version và endpoint Admin Refund/Payout ở frontend; focused Jest finance/UI `6 suites, 22 tests` và TypeScript typecheck pass.
- Thêm V29 fail-fast preflight, `transferred_at`, active-request partial unique indexes và bảng receipt idempotency; Flyway Testcontainers clean V1→V29 và V27→V29 pass.
- V29 đã được preflight và áp dụng trên Supabase bằng đúng cloud Flyway user; không có dữ liệu xung đột.
- Thêm `FinanceCommandExecutor`, receipt cleanup có ShedLock và wiring `Idempotency-Key` cho Finance POST; focused executor `2/2`, frontend finance/admin `42/42`, full backend `348/348` pass.
- Full frontend Jest `97/97` suites, `248/248` tests và typecheck pass. Concurrency/rollback/reconciliation finance integration và load benchmark chưa xác minh; xem `STATUS.md`.

## 2026-09-11 — Re-check sau khi Docker Engine hoạt động

- Chạy lại vòng diagnose cho scheduler toggle, Teacher Search và CI hardening.
- Full backend suite với PostgreSQL/Redis Testcontainers: `341/341` pass; scheduler config `3/3`; Teacher Search repository `4/4`.
- Xác nhận container Testcontainers được Ryuk tự dọn sau test; cloud smoke và load benchmark vẫn chưa chạy.

## 2026-09-11 — Hardening cloud smoke và CI runtime

- Thêm `APP_SCHEDULING_ENABLED`, mặc định `true`, để chạy cloud servlet smoke mà không kích hoạt scheduled jobs; focused config test `3/3` pass.
- Thêm `scripts/smoke-teacher-search.ps1` kiểm tra health, accent/case/whitespace normalization và unrated-last ordering.
- Nâng GitHub Actions lên các major dùng Node.js 24 và thêm Dependabot cho GitHub Actions.
- Cloud smoke thật và cold/warm load test chưa xác minh; một lần full verify sau thay đổi kết thúc exit code `1`, lần test Testcontainers tiếp theo bị chặn do Docker Desktop mất socket.

## 2026-09-11 — Tối ưu teacher search/catalog

- Thêm V28 với immutable `unaccent` wrapper, partial GIN trigram indexes và package price index; ghi ADR về dictionary/reindex invariant.
- Refactor teacher search để tính min price một lần, dùng count query tối giản, giữ batch subject query và unrated-last semantics.
- Thêm cache search TTL 5 phút có normalization/page cap, giảm profile TTL còn 30 phút, Redis lỗi fallback PostgreSQL và cấu hình Hikari qua environment.
- Focused validation/cache/serialization và architecture guardrails `15/15` pass local. PostgreSQL 16 Testcontainers repository/EXPLAIN/index assertions `4/4`; Flyway clean schema và V27 → V28 upgrade `9/9` pass. Full backend suite `338/338` pass. Supabase Flyway connection validated 28 migrations and applied V28 successfully; cloud HTTP smoke stopped on the existing missing `ClientRegistrationRepository` OAuth boot configuration. Load test 100 concurrent chưa chạy.

## 2026-09-11 — Sửa cấu hình WebSocket local cho chat

- Thêm `NEXT_PUBLIC_WS_URL=ws://localhost:8080/ws` vào overlay local frontend để STOMP kết nối trực tiếp backend thay vì fallback sang Next.js tại cổng `3000`.
- Bổ sung `frontend/.env.example` và cập nhật hướng dẫn frontend cho trường hợp Next.js và backend chạy khác origin/port.
- Smoke test thủ công: gửi/nhận tin nhắn thành công giữa Student và Teacher sau khi restart Next.js. Cảnh báo do browser extension không thuộc ứng dụng.

## 2026-09-11 — Sửa regression CI Student Journey

- Loại mapping trùng `GET /api/student/assignments/{id}` để Spring context khởi động được.
- Chuyển endpoint đọc StudentPackage về controller của module enrollment, loại dependency payment → enrollment domain.
- Sửa controller test dùng Spring Security context và assertion đúng response envelope.
- Focused suite: `9/9` pass. GitHub Actions full Maven/Testcontainers: `325/325` pass; Docker image build validation pass.

## 2026-09-11 — Đồng bộ context với working tree

- Cập nhật `STATUS.md` để liệt kê các endpoint, route và invariant đã có trong Student Journey.
- Xác nhận rõ phần đã triển khai với phần chưa nghiệm thu: full Testcontainers, Jest/Playwright, DB sạch/nâng cấp và provider thật.
- API contract tiếp tục là nguồn wire format; architecture docs là nguồn module/seam; archive không dùng để suy ra trạng thái hiện tại.

## 2026-09-11 — Chuẩn bị Student Journey và context docs

- Bổ sung luồng xác minh email, invoice purchase snapshot, trial requests, assignment detail/attachment, conversation, notification, parent contact, session reports và review state.
- Thêm migration V26/V27 và script kiểm thử Student Journey; các thay đổi code vẫn đang ở working tree.
- Đã xác nhận backend focused tests `36/36`, frontend typecheck/lint/build pass.
- Chưa xác minh full Testcontainers, Jest Student Journey và Playwright vì Docker chưa chạy.

## 2026-09-10 — Bảo vệ tài chính và vòng đời package

- Tập trung phép tính phân bổ tiền theo cumulative allocation.
- Refund giữ invariant Wallet/Ledger khi pending không đủ.
- Chặn bán package của teacher chưa approved hoặc đang hidden.
- Focused financial và package lifecycle tests đã được chạy trong đợt tương ứng.

## 2026-09-09 — Frontend architecture refactor

- Chuẩn hóa feature query boundaries, typed critical boundaries, UI component usage và public finance contracts.
- Cập nhật tài liệu frontend architecture và test lifecycle.

## Quy tắc ghi entry mới

Mỗi đợt thêm một entry gồm ngày, hành vi thay đổi, contract/schema/config liên quan, lệnh kiểm thử và phần chưa xác minh. Không ghi secret hoặc token.
- 2026-09-15: Hoàn tất verification và rollout booking settlement V41, đồng bộ E2E title assertion với metadata `Edtech Tutor Marketplace`. Backend Testcontainers `474/474` pass; frontend Docker check `101/101` suites, `264/264` tests, typecheck/lint/build pass; Playwright `4/4` pass. Supabase preflight V40→V41, apply và post-validate đều pass; schema V41 `Success`.
- 2026-09-16: Đồng bộ lại STATUS sau đối soát commit/push. V38–V41 đều đã nằm trong `origin/dev` (`c2de235`, `8308b62`, `5c4e1a1`); cập nhật các dòng stale còn ghi V41 Pending/V40 hiện tại và ghi nhận GitHub CI run `34943918942` fail ở Maven Testcontainers với log public chỉ có exit code tổng quát.
- 2026-09-14: Bổ sung xác nhận hai bên cho booking trả phí, settlement 24 giờ, escrow ledger V41 và admin reopen/release/retain actions; frontend đã có xác nhận học viên, khiếu nại gia sư và hiển thị trạng thái giữ tiền. Backend compile pass; full workflow, Supabase apply và frontend Jest chưa xác minh.
- 2026-09-14: Cloudinary flow audit — cloud key preflight pass without exposing values; real endpoint smoke remains unverified; teacher-document cleanup calls Cloudinary, generic attachment cleanup is missing.
- 2026-09-14: Local HTTP smoke attempt — backend health `200`, nhưng tài khoản test chưa có trong local DB (`0` user/profile), login `401`; không phát sinh file test trên Cloudinary.
- 2026-09-14: Cloud HTTP smoke attempt — Supabase/Flyway cloud startup pass và teacher login pass; upload bị `500` do `TeacherDocumentController` parse sai `principal.name` thành UUID trước khi gọi Cloudinary. Chưa phát sinh artifact.
- 2026-09-14: Teacher controllers fix — chuẩn hóa `TeacherDocumentController`, `TeacherProfileController`, `TeacherAvailabilityController`, `TeacherSubjectController`, `TeacherSubjectProposalController` dùng `@AuthenticationPrincipal AuthenticatedUser` thay `Principal.getName()`. Bổ sung 6 test classes với 23/23 unit tests pass (`TeacherDocumentControllerTest`, `TeacherProfileControllerTest`, `TeacherAvailabilityControllerTest`, `TeacherSubjectControllerTest`, `TeacherSubjectProposalControllerTest`, `TeacherDocumentServiceTest`) và architecture tests (10/10) pass.
- 2026-09-14: Supabase schema V32 — migration V32 (`ALTER TABLE ... ENABLE ROW LEVEL SECURITY` trên 8 bảng backend-owned: `refresh_tokens`, `teacher_documents`, `subject_proposals`, `subjects`, `pricing_packages`, `teacher_profiles`, `teacher_subjects`, `teacher_availabilities`) apply thành công lên Supabase production (`State = Success`, schema version 32); clean V1→V32 và upgrade tests V27→V32, V30→V32, V31→V32 pass (`FlywayMigrationTest` 16/16). ADR 0011 accepted and applied.
- 2026-09-14: Supabase schema V33 — migration V33 (`ALTER TABLE users ENABLE ROW LEVEL SECURITY;`) apply thành công lên Supabase production (`State = Success`, schema version 33); đối soát baseline `users` giữ nguyên `55` rows; clean V1→V33 và upgrade tests V27→V33, V30→V33, V31→V33, V32→V33 pass (`FlywayMigrationTest` 17/17); RLS behavior tests pass (`RlsBehaviorVerificationTest` 3/3); cross-table business flow test pass (`UserRlsCrossTableFlowIntegrationTest` 1/1). ADR 0011 updated.
- 2026-09-14: Regression hardening — hợp nhất request ID vào `RequestLoggingFilter`, expose `X-Request-Id` qua CORS, FE hiển thị mã tra cứu cho lỗi `5xx`, và chặn raw provider message trong ba nhánh tạo payment link của `InvoiceServiceImpl`; frontend Jest còn blocked bởi `spawn EPERM` trong môi trường hiện tại.
- 2026-09-14: API error messages — `BusinessException` sử dụng custom user message an toàn; Việt hóa các message còn sót ở auth, booking, teacher, learning, enrollment, attachment, bank account và invoice. Focused backend/AuthRegister tests pass; frontend Jest vẫn blocked bởi `spawn EPERM`.
- 2026-09-14: Hoàn thiện đợt message payment/webhook — loại bỏ các thông báo cấu hình/provider bằng tiếng Anh khỏi invoice và webhook, giữ log kỹ thuật ở server và contract ErrorCode ổn định; payment/webhook regression pass.
- 2026-09-14: Frontend verification — Jest full pass `100` suites / `256` tests; xác nhận lại sau khi chạy đúng từ `D:\EdTech\frontend`. Typecheck và lint pass; lỗi `spawn EPERM` trước đó được xác định là do môi trường/lệnh chạy, không phải test failure.
