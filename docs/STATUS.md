# Trạng thái dự án

> Cập nhật: 2026-09-21. Đây là ảnh chụp hiện tại, không phải nhật ký append-only.
> Phạm vi snapshot: code Student Journey đang có trong working tree; các thay đổi chưa được commit vẫn được đánh dấu là chưa nghiệm thu đầy đủ.

## Vercel prerender hardening (working tree, 2026-09-21)

- Deployment `dpl_7HmovQAREVxDrcr1BQVq7dYnEqjP` fail vì `/` và `/ranking` server-side fetch vượt 60 giây trong cả 3 lần retry; compile và TypeScript đều pass.
- Đã thêm timeout 10 giây cho public server fetch và giữ fallback `Promise.allSettled`; public pages không còn làm Vercel build treo khi backend cold start/outage.
- Production smoke sau khi Render warm xác nhận `/health`, subjects và teachers đều trả `200` trong dưới 1 giây; Vercel từng giữ landing fallback lỗi được tạo trong lúc backend deploy. Landing output nay revalidate 30 giây và client tự refresh sau 5 giây/mỗi 15 giây khi public data lỗi để phục hồi mà không cần người dùng reload.
- Frontend CI run `35600022117` trên commit `794ddb2` pass typecheck, lint, toàn bộ unit tests, production build và Playwright E2E trong `2m24s`. Vercel deploy thành công; production smoke xác nhận landing không còn fallback lỗi và hiển thị dữ liệu môn học/gia sư. Local Next production build pass đủ 63 route; Windows Jest vẫn bị `spawn EPERM`, Docker daemon local không chạy.

## Brevo email transport (working tree, 2026-09-21)

- Backend đã thay SMTP bằng Brevo Transactional Email API qua HTTPS, giữ transactional outbox và dùng `email_outbox.id` làm idempotency key ổn định qua retry.
- Local/test dùng logging transport; cloud fail-fast khi thiếu `BREVO_API_KEY`, `BREVO_SENDER_EMAIL` hoặc `BREVO_SENDER_NAME`; endpoint và timeout dùng default nội bộ. Focused mail và architecture tests pass; full backend suite và gửi email thật chưa chạy.
- Render deploy mới nhất bị chặn ở startup vì `BREVO_SENDER_EMAIL` trên service đang nhận literal placeholder `${BREVO_SENDER_EMAIL}`, không phải địa chỉ email. Đã bổ sung preflight nhận diện placeholder/email sai và khai báo ba biến Brevo trong `render.yaml` với `sync: false`; cần sửa giá trị trong Render Environment rồi redeploy.
- Không có migration mới; không kết nối hoặc mutate Supabase.

## Minh chứng năng lực (working tree, 2026-09-21)

- Trang gia sư đã có form dọc, vùng kéo thả JPG/PNG/PDF, preview ảnh/tên PDF, danh sách trạng thái tiếng Việt và thao tác xem/sửa/xóa qua API credential hiện có. Trang admin cũng lấy proof qua API có xác thực để xem trong modal. Public detail vẫn chỉ chứa tên badge đã duyệt.
- Giới hạn upload credential mới và file thay thế là 5MB ở FE/backend. Schema V40 vẫn có trần 10MB để dữ liệu cũ hợp lệ; không có migration mới và không kết nối hoặc mutate Supabase.
- Backend focused `TeacherCredentialServiceTest` 15/15 pass; frontend focused Jest 2/2 pass, typecheck, lint và Next production build pass. Full frontend/backend suite, Cloudinary thật và production smoke chưa chạy trong đợt này.

## Xác minh danh tính (working tree, 2026-09-21)

- Khu vực `/teacher/documents` đã đổi tên thành “Xác minh danh tính”, upload chỉ gửi `documentType=IDENTITY`; bằng cấp/chứng chỉ không còn thuộc luồng tài liệu này.
- `TeacherDocumentView` không serialize `secureUrl` cho teacher; admin vẫn xem giấy tờ trong luồng duyệt hồ sơ. Không có migration mới và chưa kết nối/mutate Supabase.
- Focused test cho document và full frontend chưa chạy lại sau thay đổi này.

## Đợt sửa auth đổi vai trò và đồng bộ session (working tree, chưa nghiệm thu production)

- Đã triển khai: redirect sau login được chọn sau khi biết `AuthResult.user.role`; chỉ giữ các query/hash trong allowlist; Admin chỉ về `/admin` theo contract hiện tại.
- Đã triển khai: logout gọi revoke best-effort tối đa 2 giây, sau đó luôn clear local session + toàn bộ React Query cache và `router.replace('/auth/login')` ngay tại tab nguồn.
- Đã triển khai: `sessionId = crypto.randomUUID()` cho mỗi lần establish, revision không chứa token qua `localStorage`, đồng bộ tab bằng `storage` + `visibilitychange`, và snapshot guard cho refresh Axios. Giả định hiện tại là một origin chỉ có một danh tính; multi-account switch thực sự chưa được hỗ trợ.
- Đã thêm regression tests cho cùng tab, hai page trong cùng Playwright context, redirect lồng, Admin, cache clear và refresh response về muộn; thêm frontend CI chạy Jest/typecheck/lint/build/E2E mock-auth.
- Bằng chứng kiểm thử đã hoàn tất trong Docker Linux engine: frontend lint pass, Jest `103/103` suites và `268/268` tests pass, Next production build pass, Chromium Playwright E2E `6/6` pass (bao gồm cùng tab và hai page trong cùng context). Lỗi Windows `spawn EPERM`/treo không còn là blocker khi chạy qua Docker.
- GitHub Frontend CI run `35581295953` trên `main` đã pass toàn bộ typecheck, lint, Jest, production build và E2E trong `3m38s`.
- Workspace loading hardening: production bundle đã xác nhận dùng đúng Render API URL và CORS preflight trả `200`; nguyên nhân skeleton kéo dài là Axios browser không có timeout khi Render cold-start/redeploy. Client nay timeout sau 15 giây và React Query chỉ retry timeout một lần, sau đó hiển thị error state thay vì loading vô hạn. Chưa redeploy/smoke production thay đổi này.
- Migration diff không có thay đổi; không kết nối hoặc mutate Supabase. Production smoke chưa đạt vì backend Render vẫn cần xác minh healthy; frontend chưa được coi là đã rollout production cho tới khi deploy và smoke thành công.

- Google OAuth complete-registration: đã sửa lỗi tạo user mới thiếu `notifyParent`, khiến PostgreSQL từ chối `NULL` trên cột `users.notify_parent` và API trả `INTERNAL_SERVER_ERROR`. Backend đặt giá trị `false` cho tài khoản OAuth; regression test nằm trong `OAuthAccountServiceTest`. OAuth focused suite `13/13` pass. Không có migration mới; chưa redeploy production.

- Teacher search HTTP binding: đã tái hiện lỗi Render bằng `MockMvc` với `GET /api/public/teachers?sort=rating_desc&page=0&size=6`; trước sửa Spring MVC ném `No primary or single unique constructor found` vì `TeacherSearchParams` có canonical constructor 14 tham số và constructor phụ 12 tham số. Đã xóa constructor phụ, chuyển test sang canonical constructor và thêm regression test qua MVC binder. Focused teacher-search `12/12` pass; full backend PostgreSQL 16/Redis Testcontainers `478/478` pass (`0` failure, `0` error, `0` skipped). Không có migration mới; không kết nối hoặc mutate Supabase. Deploy Render sau commit vẫn cần smoke test endpoint production trước khi gọi là đã xác minh.

- Render deploy hardening (working tree): log production cho thấy app đã start ở port 10000 nhưng Render vẫn timeout port scan; health check dùng endpoint nhẹ `/health` (không phụ thuộc trạng thái DB/Redis), Docker runtime đồng bộ `PORT=10000`, `EXPOSE 10000` và bind rõ `0.0.0.0`, đồng thời giữ giới hạn heap 128–256 MiB, metaspace 128 MiB, code cache 64 MiB, Serial GC, stack 512 KiB và pool DB mặc định 5. `render.yaml` cố định root `backend`, nhánh `main`; local vẫn nhận `${PORT:8080}`. Contract tests cấu hình `7/7` pass và Maven package pass; deploy Render sau thay đổi chưa xác minh. Không có migration mới; không kết nối hoặc mutate Supabase.

- Two-party booking settlement V41 đã hoàn tất rollout: tên ledger phù hợp `varchar(30)`, backfill settlement booking trả phí cũ, contract `netAmountVnd`/`bookingId` và admin actions đã đồng bộ. Full backend Testcontainers `474/474` pass; frontend Docker check `101/101` suites, `264/264` tests, typecheck/lint/build pass; Playwright E2E `4/4` pass. Supabase apply và post-validate thành công, schema version 41 `Success` ngày 2026-09-15.

- GitHub Backend CI trên `main` commit `2c0f7b4` (run `35169842151`) đã chạy 474 test: 472 pass, 1 failure, 1 error. Cả hai lỗi ở `FlywayMigrationTest`: truy vấn catalog PostgreSQL chỉ lọc tên constraint, nên đếm lẫn đối tượng trong schema thử nghiệm; truy vấn ledger nhận nhiều dòng. Commit sửa `2845332` giới hạn truy vấn theo bảng trong `public`; run `35170433438` đã pass `474/474` test, Compose startup smoke và Docker image build. Docker Desktop local chưa có daemon, nên chưa chạy lại Testcontainers tại máy này. Không có migration mới và không kết nối Supabase trong đợt sửa test.

- Regression hardening: request ID đã hợp nhất về `RequestLoggingFilter`; lỗi provider không còn lộ raw message; custom business messages đã được chuẩn hóa tiếng Việt. Mốc credential/cache riêng đạt `466/466`; mốc tích hợp cuối sau V41 đạt `474/474` tests (`0` failure, `0` error, `0` skipped). Frontend Docker Jest full pass `101/101` suites và `264/264` tests; typecheck, lint, Next.js production build và Playwright E2E `4/4` pass. Supabase hiện ở V41 `Success`.

## Mốc kỹ thuật

- Backend modular monolith đã có các seam chính cho auth, mail, payment, finance, booking, learning và communication.
- Migration mới nhất là V41 (`booking_settlements` và `platform_ledger_entries`); V38–V41 đã commit trong `c2de235`, các kiểm thử bổ sung ở `8308b62`, tài liệu rollout ở `5c4e1a1`, và toàn bộ đã push lên `origin/dev`. Supabase production hiện ở V41 `Success`; không sửa các migration đã apply.
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
| Frontend Student Journey | Đã có route/component/API thay đổi | Frontend Docker checks pass; Playwright public navigation `4/4` pass |

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
- Rollout verification 2026-09-15: Flyway preflight V40→V41, apply và post-validate đều pass; production schema version 41 ở trạng thái `Success`. GitHub run `34943918942` của chuỗi push cũ fail tại bước Maven Testcontainers với annotation tổng quát `Process completed with exit code 1`; không có log chi tiết public để kết luận test cụ thể. Local Docker run trước rollout đã pass `474/474`.
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
- Next Data Cache On-demand Tag Revalidation (2026-09-14): Route Handler nội bộ `POST /api/internal/revalidate-public` xác thực qua shared secret header `x-internal-secret` (biến môi trường server `APP_INTERNAL_REVALIDATE_SECRET`, không dùng `NEXT_PUBLIC_*`). Sử dụng `revalidateTag(tag, { expire: 0 })` chuẩn Next 16 cho các sự kiện duyệt/hủy duyệt credential (`public-teacher:{id}`) và đổi nơi ở (`public-teacher:{id}`, `public-teachers`). Backend kích hoạt gửi sự kiện sau commit (`afterCommit`) với timeout ngắn và fallback an toàn khi callback gặp lỗi, transaction rollback không bao giờ gửi sự kiện; frontend Docker verification: `101/101` test suites pass, `264/264` tests pass, typecheck, lint và Next.js production build pass (xác nhận dynamic route handler `/api/internal/revalidate-public` và toàn bộ 62 trang tĩnh/SSG). Cloudinary thật vẫn do Đông tự smoke.
- Student dashboard/public cache optimization (2026-09-14): full backend Maven/Testcontainers pass; frontend Docker verification Jest suites pass, typecheck/lint/Next production build pass. Build output confirms `/` ISR 300s, `/ranking` ISR 60s and `/teachers/[id]` on-demand ISR; Vercel Preview smoke chưa chạy. Host Windows Jest vẫn gặp `spawn EPERM`, nhưng không ảnh hưởng kết quả Docker verification. Migration diff trống, không kết nối hoặc mutate Supabase.

Các con số trên chỉ là bằng chứng gần nhất đã có; benchmark và cloud smoke chưa được gọi là pass khi chưa chạy thật.

## Việc đang chờ

1. Chạy smoke test các role Student, Teacher và Admin trên môi trường có dữ liệu đại diện.
2. Supabase production hiện ở schema V41 `Success`; không sửa migration đã apply.
3. Cập nhật bảng này bằng số liệu thật sau mỗi lần chạy.
4. Có thể chạy lại `scripts/preflight-teacher-search-extensions.sql` bằng Flyway user để bổ sung bằng chứng standalone; không deploy lại V28/V29.
5. Finance same-key concurrency/rollback integration đã được xác minh `2/2`; nếu mở rộng reconciliation nghiệp vụ riêng thì thực hiện ở đợt finance tiếp theo.
6. Benchmark bằng `node scripts/benchmark-teacher-search.mjs` với từng pool candidate; ghi riêng cold-cache và warm-cache.

## Provider chưa xác minh

Google OAuth thật, PayOS thật, Cloudinary thật, Brevo tới người dùng thật và application deployment production chưa được tính là pass trong trạng thái này. Database schema production đã rollout tới V41 `Success`.
