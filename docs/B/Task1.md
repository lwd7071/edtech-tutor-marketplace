# Nhật ký công việc: Tuần 1 - Foundation (Thành viên B)

Dựa theo kế hoạch trong `PLANBE.md` và `PLANBE_B.md`, đây là những gì Thành viên B đã thực hiện trong Task 1:

## 1. Cơ sở dữ liệu và Flyway Migrations (Baseline & Bug Fixes)
- Đã chia file `schema.sql` (toàn bộ cấu trúc hệ thống) thành 15 file migration baseline (từ `V1` đến `V15`) theo chuẩn Flyway.
- Tuân thủ chặt chẽ `CODING_CONVENTION.md`: Không sửa đổi các file `V1-V15` đã được apply.
- Tạo mới file migration **`V16__fix_schema_bugs.sql`** để sửa một số lỗi đã phát hiện khi review schema:
  - Sửa kiểu của `commission_rate` trong bảng `student_packages` từ `NUMERIC(5,4)` thành `NUMERIC(5,2)` để không bị tràn số khi giá trị vượt trên 9.99 (Ví dụ 10.00).
  - Thêm `CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED', 'EXPIRED'))` cho bảng `bookings`.
  - Thay vì cố định UUID, đã thiết kế cột `is_singleton BOOLEAN NOT NULL DEFAULT true` kèm các ràng buộc `UNIQUE(is_singleton)` và `CHECK(is_singleton = true)` cho bảng `platform_settings` để đảm bảo nó luôn chỉ chứa 1 bản ghi duy nhất.
- Tạo mới file migration **`V17__add_deleted_to_refresh_tokens.sql`** theo yêu cầu từ remediation của Thành viên A (A-02/A-03):
  - Thêm cột `deleted BOOLEAN NOT NULL DEFAULT false` vào bảng `refresh_tokens`.
  - Lý do: entity `RefreshToken` kế thừa `BaseEntity` (có field `isDeleted`) nên bảng cần cột này để Hibernate mapping đúng.

## 2. Test-Driven Development (TDD)
- Cập nhật class `FlywayMigrationTest.java` để expect đủ 17 file migration (`V1-V17`).
- Thêm các unit test truy vấn `information_schema.columns` và `pg_constraint` nhằm đảm bảo V16 hoạt động đúng như mong đợi.
- Test đã compile thành công qua lệnh `mvn clean test`.
  *(Lưu ý: Do máy trạm hiện tại không cài đặt Docker hợp lệ nên Testcontainers tự động bỏ qua phần chạy test database thật, tuy nhiên code đã sẵn sàng)*. 
- Xác nhận `mvn test` ngày 2026-08-24: Flyway validate 17 migrations thành công trên Supabase PostgreSQL 17.6.

## 3. Cấu hình ứng dụng
- Đã tạo ra file template biến môi trường `.env.example`.
- Phân tách cấu hình `application.yml` cho các môi trường (local chạy bằng docker-compose, test chạy bằng Testcontainers, và cloud/production chạy độc lập).

## 4. Facade Stubs cho Thành viên A
Tạo 3 facade để A có thể migrate module của mình mà không cần domain entity của B:

- **`enrollment.facade.EnrollmentFacade`** — `hasValidRelationship()`, `hasStudentPackage()` (dùng JdbcTemplate truy vấn `student_packages`)
- **`booking.facade.BookingEligibilityFacade`** — `getTeacherIdForReviewableBooking()`, `getTeacherBookingStats()` (dùng JdbcTemplate truy vấn `bookings`)
- **`admin.facade.PlatformSettingsFacade`** — `getBayesianMinimumReviews()` (dùng JdbcTemplate truy vấn `platform_settings`)

Ba impl này được inventory trong `archunit_ignore_patterns.txt` (approved exception — B module đọc bảng B sở hữu).

## 5. Module Skeletons & Scheduler
- Tạo đầy đủ `package-info.java` cho tất cả sub-packages của `booking`, `enrollment`, `payment`, `finance`, `admin`.
- Implement `scheduler.TeacherStatsJob`: job 02:00 mỗi ngày, ShedLock qua Redis, tái tính stats và global rank cho tất cả teacher APPROVED.

## 6. Tài liệu hoá
- Khởi tạo file `CODING_CONVENTION.md` với nguyên tắc bất biến cho Flyway và quy tắc triển khai chung.
- Log liên tục tiến trình triển khai.

---
**Trạng thái hiện tại:** Hoàn thành toàn bộ foundation (V1–V17, facades, skeletons, TeacherStatsJob). Sẵn sàng để B tiếp tục Task 2+ (Admin Approval, Payment, Booking...).
