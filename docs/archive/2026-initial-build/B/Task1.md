# Nhật ký Tuần 1 — Foundation (Thành viên B)

Tài liệu này chỉ phản ánh trạng thái đã có evidence. Checklist chi tiết và nguồn chuẩn nằm tại `PLANBE_B.md`.

## Đã hoàn thành và runtime-verified

- V1–V17 được giữ nguyên checksum; audit tĩnh phát hiện V17 tạo `deleted` nhưng `BaseEntity` map `is_deleted`.
- V18 đã sửa forward-only mismatch V17, commission/settings, schema drift, default và enum/status CHECK.
- Default configuration không còn chứa cloud credential hoặc fallback tới Supabase/Upstash; Hibernate dùng `validate`, không dùng `update`.
- Integration test chỉ nhận PostgreSQL/Redis từ Testcontainers và fail khi Docker không sẵn sàng; không còn skip/fallback.
- REST response đã chuẩn hóa thành `success/message/data/errors/meta`; security handler dùng cùng envelope.
- Facade nền tảng đã có cho Enrollment, Booking, PlatformSettings, PricingPackage, Teacher approval và TeacherStats.
- Soft-delete policy đã chốt; entity-specific implementation của B tiếp tục tại task tạo entity tương ứng.

## Việc còn lại ngoài runtime gate

- Docker engine và Compose đã xanh; V1–V18 đã runtime-verify trên PostgreSQL 16 Testcontainers.
- Credential Supabase/Upstash đã được xác nhận rotate và chỉ lưu trong `.env.cloud` bị ignore; profile cloud vẫn fail-fast cho tới khi điền Supabase password và JWT secret cục bộ.
- Git history chưa rewrite; cần maintenance window và GitHub authentication hợp lệ trước khi force-push.

## Evidence gần nhất

- `mvn test-compile`: thành công.
- Contract/architecture/facade focused tests: 19 run, 0 failure, 0 error, 0 skipped.
- Toàn bộ test không phụ thuộc Docker: 62 run, 0 failure, 0 error, 0 skipped.
- `FlywayMigrationTest`: 6 run, 0 failure, 0 error, 0 skipped; V1–V18 applied và validate thành công.
- Full `mvn test`: 88 run, 0 failure, 0 error, 0 skipped.
- Docker Engine 29.7.2/API 1.55 và Compose 5.4.0; PostgreSQL/Redis local đều healthy.
