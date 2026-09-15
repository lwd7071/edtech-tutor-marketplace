# Trạng thái dự án

> Cập nhật: 2026-09-15. Đây là ảnh chụp hiện tại, không phải nhật ký append-only.
> Phạm vi snapshot: code Student Journey đang có trong working tree; các thay đổi chưa được commit vẫn được đánh dấu là chưa nghiệm thu đầy đủ.

- Two-party booking settlement V41 đã hoàn tất rollout: tên ledger phù hợp `varchar(30)`, backfill settlement booking trả phí cũ, contract `netAmountVnd`/`bookingId` và admin actions đã đồng bộ. Full backend Testcontainers `474/474` pass; frontend Docker check `101/101` suites, `264/264` tests, typecheck/lint/build pass; Playwright E2E `4/4` pass. Supabase apply và post-validate thành công, schema version 41 `Success` ngày 2026-09-15.

- Regression hardening: request ID đã hợp nhất về `RequestLoggingFilter`; lỗi provider không còn lộ raw message; custom business messages đã được chuẩn hóa tiếng Việt. Frontend Docker Jest full pass `101/101` suites và `264/264` tests; typecheck, lint và Next.js production build pass. Full backend suite PostgreSQL/Redis Testcontainers pass `466/466` tests (`0` failure, `0` error, `0` skipped). Supabase schema giữ nguyên ở V40 `Success` (read-only count `evidence_format = ''` là 0, không cần migration làm sạch credential; V41 hiện dùng cho settlement).

## Mốc kỹ thuật

- Backend modular monolith đã có các seam chính cho auth, mail, payment, finance, booking, learning và communication.
- Migration mới nhất đã viết trong working tree là V41 (`booking_settlements` và `platform_ledger_entries`); V38–V40 đã apply trên Supabase, hiện ở V40 `Success`. Preflight Flyway read-only 2026-09-14 xác nhận V41 `Pending`. Không sửa các migration đã apply.
- Không dùng Flyway `repair()`, không sửa migration đã áp dụng và không reset database người dùng.
- Startup contract đã chuẩn hóa: cloud là mặc định và tự nạp `.env.cloud`, local được chọn rõ ràng, test có một file cấu hình canonical; Compose local đã khởi động healthy qua `/actuator/health`.

## Trạng thái theo luồng

| Luồng | Trạng thái hiện tại | Ghi chú |
|---|---|---|
| Auth/verification/logout | Đã sửa lỗi OAuth complete-registration và harden Redis serialization | Focused OAuth suite (13/13 pass), architecture (10/10 pass); full backend Testcontainers pass; luồng hoàn tất đăng ký đã sẵn sàng |
| Invoice snapshot/payment | Đã triển khai V26 | Flyway clean/upgrade và full backend đã pass; PayOS thật chưa xác minh |
| Trial/package/booking/review | Đã triển khai phần Student Journey, V35 RLS default-deny | Testcontainers full flow và RLS behavior pass; Supabase V35 `Success`; 7 bảng liên quan đã bật RLS và revoke TRUNCATE; baseline bảo toàn 100%. |
| Assignment/attachment | Đã triển khai endpoint, view mới, V35 RLS default-deny | Testcontainers flow pass; assignments và submissions đã bật RLS và revoke TRUNCATE trên Supabase V35; file thật chưa xác minh. |
| Chat/notification/events | Đã triển khai event, mở conversation và V36 RLS default-deny | Testcontainers flow và RLS behavior pass; Supabase V36 `Success`; 4 bảng liên quan (`conversations`, `messages`, `attachments`, `notifications`) đã bật RLS và revoke TRUNCATE; baseline 9 rows bảo toàn 100%. |
| Teacher search/catalog | Đã triển khai V28, query/count/cache/config | Focused tests và PostgreSQL Testcontainers pass; V28 đã áp dụng trên Supabase; cloud smoke và load test chưa xác minh |
| Finance/refund/payout/package | Đã harden ownership, version, server-owned proof, audit, V31 và V34 RLS default-deny | Full backend Maven/Testcontainers pass; Supabase V31 và V34 `Success`; 8 bảng Finance đã bật RLS và revoke TRUNCATE; baseline 32 rows bảo toàn 100%. |
| Parent contact/requests/reports | Đã triển khai UI/API liên quan | Cần smoke test theo role |
| Frontend Student Journey | Đã có route/component/API thay đổi | Playwright chưa chạy |

## Những gì đã có trong code hiện tại

### Architecture và Module Isolation

- Controller không trả về Entity (đã fix Invoice, TrialRequest bằng DTO/View).
- Controller được đặt đúng module (chuyển AdminRefund/Payout/Extension sang finance, TeacherSubjectProposal sang subject).
- Các vi phạm cấm gọi chéo module đã được sửa chữa.


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
- Frontend Docker verification: `101/101` suites, `264/264` tests, typecheck/lint/build pass; Playwright public navigation `4/4` pass.
- Teacher-search focused validation/cache/serialization + architecture guardrails: pass local (`15/15`).
- Teacher-search PostgreSQL 16 Testcontainers: repository regression + EXPLAIN/index assertions `4/4` pass; Flyway clean schema, metadata và V27 → V28 upgrade `9/9` pass.
- Supabase V28 migration bằng đúng Flyway connection/user: pass; Flyway validated 28 migrations, current version V27 và apply V28 thành công trên PostgreSQL 17.6. Migration DO preflight xác nhận `unaccent`/`pg_trgm` ở `public`.
- Supabase V29 preflight bằng đúng Flyway user: current version V28, extensions `unaccent`/`pg_trgm` ở `public`, không có duplicate active refund/extension/payout hoặc legacy status; cloud servlet startup đã validate 29 migrations và apply V29 thành công trên PostgreSQL 17.6, sau đó process đã được dừng.
- Supabase V30 preflight read-only xác nhận current version V29 và validate 30 migrations; Flyway đã apply `V30__fix_data_constraints_and_locking` thành công trên PostgreSQL 17.6 ngày 2026-09-13, post-migrate validate/info xác nhận schema version V30 và migration state `Success`. Không sửa migration cũ, không dùng `repair` hoặc `clean`.
- Thêm `scripts/update-supabase-schema.ps1` và Flyway Maven plugin guarded: code/docs-only không mutate Supabase; migration mới phải kiểm tra version, chặn operation phá hủy, preflight và post-validate.
- Finance idempotency executor, aspect wiring cho toàn bộ Finance POST và scheduled receipt cleanup đã compile; focused executor `2/2` pass. Frontend API tạo key và hook giữ key qua retry cùng command; focused finance/admin Jest `42/42` và typecheck pass.
- Full backend Maven/Testcontainers sau khi đồng bộ API, sửa lỗi Architecture, và fix lỗi Bảo mật/Phân quyền (Mục 5): `350/350` pass, `0` failure/error/skipped (Đã verify `SecurityIdorIntegrationTest` pass với RequireRoleAspect).
- Đợt optimistic-lock contract tiếp theo đã cập nhật DTO/service/controller cho trial, learning, subject proposal và bank account; backend focused và full integration đều pass. `If-Match` bank-account delete kiểm tra ownership trước khi parse để giữ IDOR `404` và malformed own-resource header `400`.
- Verification mới nhất sau regression review: backend focused `15/15` và full backend `366/366` pass (`0` failure/error/skipped) với Docker Desktop/Testcontainers thật. Frontend focused `31/31` và `npm run check` pass: typecheck, lint, `97/97` suites, `252/252` tests và Next build. Docs check pass.
- API contract focused backend sau đợt đồng bộ envelope/status/invoice/version: `26/26` pass (bao gồm `RestStatusContractTest`). Frontend typecheck pass; frontend Jest contract runner vẫn chưa xác minh vì bị treo trong môi trường hiện tại.
- Full frontend Jest sau khi cập nhật regression contract/version tests: `97/97` suites, `252/252` tests pass.
- Regression hardening 2026-09-14: full backend `370/370` pass; Flyway clean V1→V31 và upgrade V30→V31 pass; Supabase V31 apply/validate/info `Success`. Frontend typecheck pass; focused Jest finance API chạy qua `cmd` `7/7` pass; full Jest chưa chạy.
- Startup/config hardening 2026-09-14: config contract focused `9/9` pass; full `mvn clean verify` với PostgreSQL/Redis Testcontainers `374/374` pass, `0` failure/error/skipped; `docker compose config --quiet` pass; image build pass; Compose backend local đạt trạng thái `healthy` qua `/actuator/health`.
- Cloud config preflight trước đó từng bị chặn vì thiếu `EDTECH_ACCOUNT_ENCRYPTION_KEY`; lần kiểm tra 2026-09-14 hiện đã pass đầy đủ key mà không in giá trị secret. Cloud HTTP smoke vẫn chưa xác minh.
- Cloudinary verification (2026-09-14): `scripts/check-backend-config.ps1 -Profile cloud` hiện pass và không in giá trị secret; local profile có fallback Cloudinary giả cho môi trường dev. Chưa chạy được upload HTTP thật vì không có backend listener đang chạy và endpoint yêu cầu authenticated teacher/DB context. Code review xác nhận teacher-document delete gọi Cloudinary destroy trước soft-delete DB; attachment có upload qua Cloudinary nhưng chưa có luồng delete gọi storage, nên có nguy cơ orphan file.
- Local HTTP smoke attempt (2026-09-14): PostgreSQL/Redis local healthy, backend profile `local` khởi động và `/actuator/health` trả `200`; login `teacher20@edtech.vn` trả `401`, local DB có `0` user trùng email và `0` teacher profile. Vì vậy upload endpoint chưa thể chạy; backend đã được dừng sau kiểm tra.
- Cloud HTTP smoke attempt (2026-09-14): profile `cloud` kết nối Supabase PostgreSQL 17.6, Flyway validate 31 migrations và schema V31 không có migration cần apply; login teacher test thành công nhưng `POST /api/teacher/documents` trả `500`. Root cause trong log: `TeacherDocumentController` gọi `UUID.fromString(principal.getName())`, nhưng principal name không phải UUID (`UUID string too large`); chưa tới bước gọi Cloudinary, chưa phát sinh artifact.
- Teacher controllers user ID extraction fix (2026-09-14): Chuẩn hóa `TeacherDocumentController`, `TeacherProfileController`, `TeacherAvailabilityController`, `TeacherSubjectController`, `TeacherSubjectProposalController` sang `@AuthenticationPrincipal AuthenticatedUser user` và lấy `user.id()`. Bộ 6 test classes mới với `23/23` unit tests pass (`TeacherDocumentControllerTest` 3/3, `TeacherProfileControllerTest` 3/3, `TeacherAvailabilityControllerTest` 2/2, `TeacherSubjectControllerTest` 3/3, `TeacherSubjectProposalControllerTest` 2/2, `TeacherDocumentServiceTest` 10/10); toàn bộ teacher suite `mvn test -Dtest=*Teacher*` 44/44 pass; architecture suite `10/10` pass (`ArchitectureTest` 3/3, `SolidGuardrailsArchitectureTest` 7/7). Cloud HTTP upload smoke thật chưa chạy vì cần backend runtime có kết nối Cloudinary/Supabase.
- OAuth complete-registration & Redis serialization: focused suite `13/13` pass (`OAuthAccountServiceTest` 5/5, `OAuthIdentitySerializationTest` 2/2, `AuthControllerOAuthContractTest` 2/2, `OAuth2AuthenticationFailureHandlerTest` 1/1, `OAuth2AuthenticationSuccessHandlerTest` 3/3); architecture guardrails `10/10` pass (`ArchitectureTest` 3/3, `SolidGuardrailsArchitectureTest` 7/7).
- Supabase RLS V32 rollout (2026-09-14): Đông xác nhận rollout; Flyway clean-schema V1→V32 pass; upgrade tests V27→V32, V30→V32, V31→V32 pass; `FlywayMigrationTest` 16/16 pass; Flyway migration V32 (`ALTER TABLE ... ENABLE ROW LEVEL SECURITY` trên 8 bảng nghiệp vụ backend: `refresh_tokens`, `teacher_documents`, `subject_proposals`, `subjects`, `pricing_packages`, `teacher_profiles`, `teacher_subjects`, `teacher_availabilities`) apply thành công trên Supabase production, schema version đạt V32 (`State = Success`), post-migrate validation 32 migrations pass. Không in secret.
- Supabase RLS V33 rollout (2026-09-14): Đông xác nhận rollout; preflight read-only V32→V33 pass; Flyway clean-schema V1→V33 pass; upgrade tests V27→V33, V30→V33, V31→V33, V32→V33 pass; `FlywayMigrationTest` 17/17 pass; `RlsBehaviorVerificationTest` 3/3 pass; `UserRlsCrossTableFlowIntegrationTest` 1/1 pass (xác nhận luồng nghiệp vụ xuyên bảng `users` -> `teacher_profiles` -> `pricing_packages` -> `invoices` -> `student_packages` -> `bookings` hoạt động trơn tru qua Testcontainers). Flyway migration V33 (`ALTER TABLE users ENABLE ROW LEVEL SECURITY;`) apply thành công trên Supabase production (`State = Success`), schema version đạt V33, post-migrate validation 33 migrations pass. Baseline `users` đối soát trước và sau migration giữ nguyên `55` rows (0 data loss). Không in secret.
- Supabase RLS V34 rollout (2026-09-14): Đông xác nhận rollout; preflight read-only V33→V34 pass; Flyway clean-schema V1→V34 pass; upgrade tests V27→V34, V30→V34, V31→V34, V32→V34, V33→V34 pass; `FlywayMigrationTest` 18/18 pass; `RlsBehaviorVerificationTest` 5/5 pass (xác nhận unprivileged role bị chặn SELECT/INSERT trên users, wallets, teacher_bank_accounts và bị chặn TRUNCATE); `UserRlsCrossTableFlowIntegrationTest` 1/1 pass; `FinanceRlsFlowIntegrationTest` 1/1 pass (xác nhận luồng nạp ví, tạo invoice, chuyển tiền ví, ghi ledger và idempotency receipt chạy qua PostgreSQL superuser bypass RLS). Flyway migration V34 (`ALTER TABLE ... ENABLE ROW LEVEL SECURITY; REVOKE TRUNCATE ... FROM anon, authenticated;` trên 8 bảng Finance: `wallets`, `ledger_entries`, `invoices`, `payment_transactions`, `payout_requests`, `refund_requests`, `teacher_bank_accounts`, `finance_command_receipts`) apply thành công trên Supabase production ngày 2026-09-14 (`State = Success`), schema version đạt V34, post-migrate validation 34 migrations pass. Baseline đối soát trước và sau migration giữ nguyên chính xác 32 rows (`wallets`: 20, `ledger_entries`: 4, `invoices`: 4, `teacher_bank_accounts`: 4; 4 bảng còn lại: 0) (0 data loss). Không in secret.
- Supabase RLS V35 rollout (2026-09-14): Đông xác nhận rollout; preflight read-only V34→V35 pass; Flyway clean-schema V1→V35 pass; upgrade tests V27→V35, V30→V35, V31→V35, V32→V35, V33→V35, V34→V35 pass; `FlywayMigrationTest` 19/19 pass; `RlsBehaviorVerificationTest` 5/5 pass (xác nhận unprivileged role bị chặn SELECT/INSERT trên bookings, student_packages, reviews và bị chặn TRUNCATE); `BookingLearningRlsFlowIntegrationTest` 1/1 pass (xác nhận luồng mua gói, đặt lịch, session report, review & stats, assignment & submission qua PostgreSQL superuser); full suite 27/27 pass. Flyway migration V35 (`ALTER TABLE ... ENABLE ROW LEVEL SECURITY; REVOKE TRUNCATE ... FROM anon, authenticated;` trên 9 bảng: `student_packages`, `package_extension_requests`, `trial_requests`, `bookings`, `session_reports`, `reviews`, `teacher_stats`, `assignments`, `submissions`) apply thành công trên Supabase production ngày 2026-09-14 (`State = Success`), schema version đạt V35, post-migrate validation 35 migrations pass. Baseline đối soát trước và sau migration giữ nguyên chính xác 32 rows (`teacher_stats`: 20, `bookings`: 3, `student_packages`: 3, `trial_requests`: 2, `session_reports`: 2, `reviews`: 2, các bảng còn lại: 0) (0 data loss). Không in secret.
- Supabase RLS V36 rollout (2026-09-14): Đông xác nhận rollout; preflight read-only V35→V36 pass; Flyway clean-schema V1→V36 pass; upgrade tests V27→V36, V30→V36, V31→V36, V32→V36, V33→V36, V34→V36, V35→V36 pass; `FlywayMigrationTest` 20/20 pass; `RlsBehaviorVerificationTest` 6/6 pass (xác nhận unprivileged role bị chặn SELECT/INSERT trên conversations, messages, notifications và bị chặn TRUNCATE); `CommunicationRlsFlowIntegrationTest` 1/1 pass (xác nhận luồng hội thoại, tin nhắn văn bản, file attachment, thông báo và trạng thái đã đọc qua PostgreSQL superuser); full suite 27/27 pass. Flyway migration V36 (`ALTER TABLE ... ENABLE ROW LEVEL SECURITY; REVOKE TRUNCATE ... FROM anon, authenticated;` trên 4 bảng: `conversations`, `messages`, `attachments`, `notifications`) apply thành công trên Supabase production ngày 2026-09-14 (`State = Success`), schema version đạt V36, post-migrate validation 36 migrations pass. Baseline đối soát trước và sau migration giữ nguyên chính xác 9 rows (`conversations`: 3, `messages`: 3, `attachments`: 0, `notifications`: 3) (0 data loss). Không in secret.
- Supabase RLS V37 rollout (2026-09-14): Đông xác nhận rollout; preflight read-only V36→V37 pass; Flyway clean-schema V1→V37 pass; upgrade tests V27→V37, V30→V37, V31→V37, V32→V37, V33→V37, V34→V37, V35→V37, V36→V37 pass; `FlywayMigrationTest` 21/21 pass; `RlsBehaviorVerificationTest` 7/7 pass (xác nhận unprivileged role bị chặn SELECT/INSERT trên platform_settings, audit_logs, email_outbox và bị chặn TRUNCATE); `SystemOutboxRlsFlowIntegrationTest` 1/1 pass (xác nhận luồng cấu hình singleton, ghi audit log, đưa email vào outbox và worker gửi mail); full suite 29/29 pass. Flyway migration V37 (`ALTER TABLE ... ENABLE ROW LEVEL SECURITY; REVOKE ALL ... FROM anon, authenticated;` trên các bảng System, Outbox & Audit Logs: `platform_settings`, `audit_logs`, `email_outbox`, `modulebentity`) apply thành công trên Supabase production ngày 2026-09-14 (`State = Success`), schema version đạt V37, post-migrate validation 37 migrations pass. Baseline đối soát trước và sau migration giữ nguyên chính xác 100% dữ liệu (`platform_settings`: 1, `flyway_schema_history`: 37; toàn bộ 34 bảng nghiệp vụ trong schema public đã được bảo vệ bởi RLS) (0 data loss). Không in secret.
- Administrative reference V38 (2026-09-14): nguồn Open Admin Data đối soát theo Quyết định 19/2025/NSO với đúng 34 tỉnh/thành và 3.321 xã/phường; migration `V38__create_administrative_reference_tables.sql` tạo `provinces`/`wards`, seed đủ 34/3.321 bản ghi, FK/index/check constraints pass. Local Flyway clean V1→V38 và các upgrade V30..V37→V38 pass; Supabase production đã apply thành công V38 `Success`.
- Teacher residence V39 (2026-09-14): thêm nullable `province_code`/`ward_code` trên `teacher_profiles` với FK cùng tỉnh, composite index và check ward phải có province. Profile DTO/GET/PUT nhận mã reference; thêm `PUT /api/teacher/profile/residence` để đổi nơi ở mà không chuyển profile `APPROVED` về `DRAFT`. Local Flyway clean/upgrade V30..V37→V39 và teacher profile focused tests pass; Supabase production đã apply thành công V39 `Success`.
- Public location/search API (2026-09-14): thêm `GET /api/public/locations/provinces`, cascading wards endpoint và filter `provinceCode`/`wardCode` cho teacher search; card projection trả tên tỉnh/xã. Teacher search focused cache/controller/repository tests pass; Supabase production đã áp dụng thành công.
- Frontend location UX (2026-09-14): teacher profile có cascading tỉnh→xã và lưu qua endpoint residence riêng; public teacher search có filter tỉnh/xã và card hiển thị khu vực. Frontend typecheck, lint, build pass; Jest container Linux pass.
- Teacher credential badges V40 & regression hardening Luna (2026-09-14): migration `teacher_credentials` đã apply thành công trên Supabase production (V40 `Success`). Đã kiểm tra read-only trên Supabase: số bản ghi `evidence_format = ''` là 0, không cần migration V41. Đã chuẩn hóa `evidenceFormat` ở cả hai nhánh create và update theo MIME Apache Tika xác nhận (`jpg`, `png`, `pdf`), chặn file extension giả trước khi upload. Mở rộng bộ kiểm thử: `TeacherCredentialServiceTest` 15/15 unit tests pass, `TeacherCredentialControllerTest` 4/4 pass, `AdminCredentialControllerTest` 4/4 pass, `TeacherCredentialIntegrationTest` 5/5 pass trên PostgreSQL Testcontainers (xác nhận 409 CONCURRENT_MODIFICATION khi xung đột đồng thời, storage rollback delete, after-commit delete proof cũ, public detail chỉ trả approved badge ID và label không chứa URL/bytes), `ArchitectureTest` và `SolidGuardrailsArchitectureTest` 10/10 pass, `PublicCacheRevalidationClientTest` 3/3 pass. Full backend suite PostgreSQL/Redis Testcontainers: `466/466` tests pass, `0` failure, `0` error, `0` skipped.
- Next Data Cache On-demand Tag Revalidation (2026-09-14): Route Handler nội bộ `POST /api/internal/revalidate-public` xác thực qua shared secret header `x-internal-secret` (biến môi trường server `INTERNAL_REVALIDATE_SECRET`, không dùng `NEXT_PUBLIC_*`). Sử dụng `revalidateTag(tag, { expire: 0 })` chuẩn Next 16 cho các sự kiện duyệt/hủy duyệt credential (`public-teacher:{id}`) và đổi nơi ở (`public-teacher:{id}`, `public-teachers`). Backend kích hoạt gửi sự kiện sau commit (`afterCommit`) với timeout ngắn và fallback an toàn khi callback gặp lỗi, transaction rollback không bao giờ gửi sự kiện; frontend Docker verification: `101/101` test suites pass, `264/264` tests pass, typecheck, lint và Next.js production build pass (xác nhận dynamic route handler `/api/internal/revalidate-public` và toàn bộ 62 trang tĩnh/SSG). Cloudinary thật vẫn do Đông tự smoke.
- Student dashboard/public cache optimization (2026-09-14): full backend Maven/Testcontainers pass; frontend Docker verification Jest suites pass, typecheck/lint/Next production build pass. Build output confirms `/` ISR 300s, `/ranking` ISR 60s and `/teachers/[id]` on-demand ISR; Vercel Preview smoke chưa chạy. Host Windows Jest vẫn gặp `spawn EPERM`, nhưng không ảnh hưởng kết quả Docker verification. Migration diff trống, không kết nối hoặc mutate Supabase.

Các con số trên chỉ là bằng chứng gần nhất đã có; benchmark và cloud smoke chưa được gọi là pass khi chưa chạy thật.

## Việc đang chờ

1. Chạy smoke test các role Student, Teacher và Admin trên môi trường có dữ liệu đại diện.
2. Supabase production hiện ở schema V41 `Success`; không sửa migration đã apply.
3. Chạy smoke test các role Student, Teacher và Admin.
4. Cập nhật bảng này bằng số liệu thật sau mỗi lần chạy.
5. Có thể chạy lại `scripts/preflight-teacher-search-extensions.sql` bằng Flyway user để bổ sung bằng chứng standalone; không deploy lại V28/V29.
6. Finance same-key concurrency/rollback integration đã được xác minh `2/2`; nếu mở rộng reconciliation nghiệp vụ riêng thì thực hiện ở đợt finance tiếp theo.
7. Benchmark bằng `node scripts/benchmark-teacher-search.mjs` với từng pool candidate; ghi riêng cold-cache và warm-cache.

## Provider chưa xác minh

Google OAuth thật, PayOS thật, Cloudinary thật, SMTP tới người dùng thật và production deployment chưa được tính là pass trong trạng thái này.
