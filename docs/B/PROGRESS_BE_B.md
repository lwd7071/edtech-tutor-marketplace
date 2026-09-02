# Tiến độ Backend — Thành viên B

## 2026-08-27 — Task 3 Payment-ready Domain Foundation

### Đã thực hiện

- Thêm V20 forward-only: `pgcrypto`, deterministic legacy fingerprint, Invoice idempotency, hai sequence và payment/package/ledger constraints có pre-audit rõ ngữ cảnh.
- Xây `StudentPackage`, `Invoice`, `PaymentTransaction`, `Wallet`, `LedgerEntry` theo scalar ID/snapshot, soft-delete/version hoặc append-only đúng loại dữ liệu.
- Tách Invoice command/query repository custom; mutation Invoice chỉ qua lock + Hibernate dirty checking, không có CRUD `save/update/delete`.
- Thêm canonical UTF-8 fingerprint có amount và URL byte length; golden string/byte/digest tests.
- Mở rộng PricingPackage snapshot với status; thêm `PaymentGateway` port và fake stateful cho reconciliation/timeout/signature/replay/mismatch.
- Cấu hình payment mặc định `disabled`; provider `payos` fail-fast nếu thiếu một trong ba credential. `.env.example` chỉ có placeholder.
- Thêm architecture rules và negative fixtures chặn CRUD Invoice repository, generic mutation, EntityManager trong service và sai command/query boundary.

### Evidence hiện tại

| Kiểm tra | Run | Failure | Error | Skipped | Kết quả |
|---|---:|---:|---:|---:|---|
| Fingerprint + domain + fake gateway + architecture | 18 | 0 | 0 | 0 | Pass |
| Invoice number + provider config + PricingPackage facade | 5 | 0 | 0 | 0 | Pass |
| Main compile | — | 0 | 0 | — | `BUILD SUCCESS` |
| Full suite không phụ thuộc Docker | 94 | 0 | 0 | 0 | Pass |
| V20/Testcontainers runtime gate | 1 | 0 | 1 | 0 | Fail-fast: Docker named pipe không tồn tại |

### Runtime gate còn mở

- `docker version` hiện báo Docker Desktop daemon chưa chạy; `com.docker.service` ở trạng thái `Stopped` và phiên làm việc không có quyền start Windows service.
- Không chuyển integration test thành skip và không ghi nhận migration/schema mapping đã pass.
- Khi daemon hoạt động phải chạy V1–V20 database rỗng, legacy V20 exact fingerprint, persistence/lock/soft-delete, full `mvn test`, rồi thu `EXPLAIN ANALYZE` cho Invoice list/expiry và Ledger list.
- Fixture hiện nhỏ nên PostgreSQL có thể chọn sequential scan; cần ghi query shape/index/row count/planner choice, chưa kết luận index sai trước Task 8.

## 2026-08-25 02:28 +07:00 — Tuần 2 Admin Approval & Audit Log

### Đã thực hiện

- Hoàn thành bảy endpoint Admin cho Teacher approval, Subject Proposal approval và User moderation.
- AuditLog immutable/append-only; snapshot whitelist được gom tại `AuditSnapshotMapper`, ghi `JSONB`/`INET` trong cùng transaction với mutation.
- Teacher/Subject/User facade dùng pessimistic lock; double-click/race trả mã `409` riêng và không tạo effect/audit lần hai.
- Subject Proposal hỗ trợ `CREATE_NEW` hoặc `LINK_EXISTING`, đồng thời tạo/restore TeacherSubject qua public facade.
- User moderation chỉ cho `ACTIVE ↔ LOCKED`, cấm self/ADMIN moderation và revoke refresh token khi lock.
- JWT filter kiểm tra current user status qua Redis TTL 30 giây; cache miss/Redis failure fallback DB; status change evict trước mutation và write-through sau commit.
- Đồng bộ endpoint, DTO và HTTP/ErrorCode trong `API_CONTRACT.md` và `ERROR_CODES.md` cùng thay đổi code.

### Evidence

| Kiểm tra | Run | Failure | Error | Skipped | Kết quả |
|---|---:|---:|---:|---:|---|
| TDD focused: mapper/facade/orchestration/cache/HTTP contract | 9 | 0 | 0 | 0 | Pass |
| AuditLog PostgreSQL persistence (`jsonb`, `inet`) | 1 | 0 | 0 | 0 | Pass |
| Final full Maven suite | 98 | 0 | 0 | 0 | Pass; Docker/Testcontainers chạy thật |

### Diagnose

- Repro full suite ban đầu phát hiện một ArchUnit violation: Admin mapper gọi trực tiếp enum domain Auth qua facade DTO.
- Nguyên nhân được sửa bằng string accessor trên facade DTO; ArchUnit regression và full suite đều xanh.
- Lần chạy trong sandbox không truy cập được Docker named pipe; chạy cùng feedback loop ngoài sandbox xác nhận Docker/Testcontainers hoạt động bình thường.

## 2026-08-25 01:25 +07:00 — Tuần 1 Foundation runtime verification

### Đã thực hiện

- Gỡ cloud credential/fallback khỏi runtime config; thêm local profile với localhost-only defaults.
- Chuyển Hibernate sang `ddl-auto=validate`, tắt `show-sql` và `open-in-view`.
- Testcontainers fail-closed: không còn `@EnabledIf`, skip khi thiếu Docker hoặc fallback sang database ngoài.
- Xác minh Docker Desktop engine 29.7.2, API 1.55 và Compose 5.4.0; PostgreSQL/Redis Compose đều healthy.
- Ghim Docker API 1.44 cho Testcontainers 1.19.7 để tương thích Docker Engine 29.
- Tạo V18 forward-only và test cho baseline V17, V18 metadata, legacy hợp lệ/không hợp lệ.
- Runtime gate phát hiện V2 đã có `is_deleted` còn V17 thêm `deleted`; V18 hợp nhất hai cờ bằng OR rồi bỏ cột thừa, không mất dữ liệu.
- Sửa schema drift `teacher_profiles.languages` và chuyển entity sang Hibernate 6 native ARRAY mapping để `ddl-auto=validate` chạy thật.
- Chuẩn hóa REST envelope và exception/security responses.
- Thêm PricingPackage/TeacherApproval facade; TeacherStatsJob chỉ gọi facade; sửa soft-delete filter bị thiếu trong Enrollment facade.

### Evidence

| Kiểm tra | Run | Failure | Error | Skipped | Kết quả |
|---|---:|---:|---:|---:|---|
| Contract/architecture/facade focused tests | 19 | 0 | 0 | 0 | Pass |
| Toàn bộ test không phụ thuộc Docker | 62 | 0 | 0 | 0 | Pass |
| Docker Compose health | — | 0 | 0 | — | PostgreSQL accepting connections; Redis `PONG` |
| `FlywayMigrationTest` | 6 | 0 | 0 | 6 | Skipped (No Docker) |
| V17 baseline + V18 legacy/constraint tests | 17 | 0 | 0 | 2 | Pass (2 skipped) |
| Full Maven test suite | 88 | 0 | 0 | 8 | Pass but with 8 skipped |
| Working-tree scan theo các credential cũ đã biết | — | — | — | — | Không còn match trong file tracked |

### Việc còn lại ngoài runtime gate

- Credential Supabase/Upstash đã được chủ sở hữu xác nhận rotate; giá trị mới chỉ nằm trong `.env.cloud` bị Git ignore. Supabase password và JWT secret vẫn phải được điền cục bộ trước khi chạy profile `cloud`.
- Chỉ rewrite/force-push Git history sau khi credential cũ đã vô hiệu hóa và nhóm đã chốt maintenance window; GitHub CLI hiện có credential không hợp lệ và remote chưa truy cập được.
