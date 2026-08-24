# Tiến độ Backend — Thành viên B

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
| `FlywayMigrationTest` | 6 | 0 | 0 | 0 | Pass; V1–V18 applied, Flyway validate thành công |
| V17 baseline + V18 legacy/constraint tests | 17 | 0 | 0 | 0 | Pass |
| Full Maven test suite | 88 | 0 | 0 | 0 | Pass |
| Working-tree scan theo các credential cũ đã biết | — | — | — | — | Không còn match trong file tracked |

### Việc còn lại ngoài runtime gate

- Credential Supabase/Upstash đã được chủ sở hữu xác nhận rotate; giá trị mới chỉ nằm trong `.env.cloud` bị Git ignore. Supabase password và JWT secret vẫn phải được điền cục bộ trước khi chạy profile `cloud`.
- Chỉ rewrite/force-push Git history sau khi credential cũ đã vô hiệu hóa và nhóm đã chốt maintenance window; GitHub CLI hiện có credential không hợp lệ và remote chưa truy cập được.
