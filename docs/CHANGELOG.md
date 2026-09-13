# Changelog theo đợt hoàn thành

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
