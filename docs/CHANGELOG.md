# Changelog theo đợt hoàn thành

## 2026-09-14 — Chuẩn bị triển khai RLS V32

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
