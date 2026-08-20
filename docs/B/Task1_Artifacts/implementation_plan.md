# Implementation Plan - Task 1: Foundation (Infra, Config & Migration) - Member B (v3 - Canonical Schema)

Kế hoạch triển khai chuẩn xác cho **Task 1 (Tuần 1 — Foundation)** do Thành viên B phụ trách, tích hợp **100% schema gốc từ `schema.sql`** (chuẩn hóa đầy đủ extensions, trigger function `set_updated_at()`, toàn bộ triggers `trg_..._updated`, partial unique indexes và GiST exclusion constraints).

---

## User Review Required & Design Decisions

> [!IMPORTANT]
> **1. Nguồn chân lý duy nhất (Single Source of Truth) cho Flyway DDL:**
> - Toàn bộ DDL của V1–V14 được cắt trực tiếp từ `schema.sql` gốc do User cung cấp, giữ nguyên 100%:
>   - Tên cột, kiểu dữ liệu, default values (`gen_random_uuid()`, `now()`).
>   - Extension `pgcrypto`, `btree_gist` và function `set_updated_at()` đặt tại đầu `V1`.
>   - Toàn bộ triggers `trg_<table_name>_updated` gắn theo từng bảng.
>   - GiST Exclusion Constraints: `ex_booking_teacher_overlap`, `ex_booking_student_overlap`.
>   - Toàn bộ Partial Unique Indexes (`ux_users_email`, `ux_users_oauth`, `ux_invoices_payos_order_code`, `ux_invoices_payos_link_id`, `ux_trial_requests_pending_pair`, `ux_bank_accounts_default`, `ux_refund_requests_pending_package`, `ux_extension_requests_pending_package`).
> - **Tuyệt đối không sửa lại V1–V15** sau khi đã migrate thành công; các thay đổi phát sinh ở tuần 2–8 bắt buộc tạo từ `V16+`. (Sẽ được ghi chú vào CODING_CONVENTION.md)

> [!WARNING]
> **Kết quả Diagnose (Bug được phát hiện qua tĩnh phân tích):**
> Do môi trường local không bật Docker (Lỗi: `Could not find a valid Docker environment`), agent không thể dựng Testcontainers để chạy programmatic loop cho Phase 1 của `/diagnose`. Tuy nhiên, qua phân tích tĩnh (Static Analysis) `schema.sql`, agent phát hiện các rủi ro sau:
> 1. **Bug Precision `commission_rate` (Nghiêm trọng):** Trong `platform_settings` định nghĩa `NUMERIC(5,2)` (VD: 10.00 = 10%), nhưng bảng `student_packages` lại khai báo `NUMERIC(5,4)`. `NUMERIC(5,4)` chỉ cho phép 1 chữ số trước dấu phẩy (max 9.9999). Việc lưu `10.00` vào `student_packages` sẽ gây lỗi tràn số (`numeric field overflow`).
> 2. **Rủi ro Constraint Trùng lịch `bookings`:** GiST Constraint `ex_booking_teacher_overlap` chỉ bắt `status = 'SCHEDULED'`. Nếu booking có status khác (VD: `IN_PROGRESS`), constraint này sẽ bỏ qua, có thể dẫn đến việc học sinh khác book đè vào lịch đang dạy. Bảng `bookings` cũng đang thiếu `CHECK` constraint để giới hạn tập giá trị của `status`.
> 3. **Rủi ro Singleton `platform_settings`:** Bảng cài đặt hệ thống dùng `UUID` nhưng không có constraint giới hạn chỉ có duy nhất 1 row (VD: `CHECK (id = ...)` hoặc tương đương).

## Open Questions

> [!CAUTION]
> **Về việc fix các bug vừa tìm được (Đã xác minh qua Document):**
> Dựa theo phản hồi của bạn, tôi đã tra cứu lại các file đặc tả và cập nhật kế hoạch cho `V16` như sau:
> 
> 1. **Commission Rate:** Trong `SPEC.md` và `API_CONTRACT.md`, `commissionRate` được định nghĩa là phần trăm (VD: `5.00` = 5%, `10.00` = 10%). Việc dùng `NUMERIC(5,4)` ở bảng `student_packages` sẽ gây lỗi tràn số khi giá trị là `10.00` (vì chỉ cho phép 1 chữ số trước dấu phẩy). Cần sửa thành `NUMERIC(5,2)` giống với bảng `platform_settings`.
> 2. **Booking Status Enum:** `SPEC.md` định nghĩa `bookings.status` gồm 4 giá trị: `SCHEDULED`, `COMPLETED`, `CANCELLED`, `EXPIRED`.
> 3. **Singleton cho Platform Settings:** Cột giả `is_singleton BOOLEAN NOT NULL DEFAULT true` kèm `UNIQUE` và `CHECK` sẽ được thêm vào thay vì cố định ID.

> [!IMPORTANT]
> **Bạn đã đồng ý tạo `V16`. Hãy nhấn PROCEED để tôi bắt đầu thực thi việc tạo file V16 và `CODING_CONVENTION.md`!**

> [!IMPORTANT]
> **2. Bảng mới `platform_settings` (V15):**
> - Schema structured table khớp chuẩn [API_CONTRACT.md](file:///d:/EdTech/API_CONTRACT.md) và [SPEC.md](file:///d:/EdTech/SPEC.md):
>   ```sql
>   CREATE TABLE platform_settings (
>       id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
>       commission_rate           NUMERIC(5,2) NOT NULL DEFAULT 10.00,
>       bayesian_minimum_reviews  INT NOT NULL DEFAULT 10,
>       booking_reminder_hours    INT NOT NULL DEFAULT 11,
>       booking_expiration_hours  INT NOT NULL DEFAULT 12,
>       updated_at                TIMESTAMPTZ NOT NULL DEFAULT now()
>   );
>   CREATE TRIGGER trg_platform_settings_updated BEFORE UPDATE ON platform_settings FOR EACH ROW EXECUTE FUNCTION set_updated_at();
>   ```

> [!IMPORTANT]
> **3. Phân tách ranh giới Test Properties & Testcontainers:**
> - `application-test.yml`: Chỉ chứa static properties (JWT secret test, tắt auto scheduled jobs, logging).
> - `AbstractIntegrationTest`: Inject động 100% properties kết nối (`spring.datasource.*`, `spring.data.redis.*`) từ Singleton PostgreSQL 16 & Redis 7 container qua `@DynamicPropertySource`.

---

## Proposed Changes

### 1. Hạ tầng Local & CI Pipeline

#### [MODIFY] [docker-compose.yml](file:///d:/EdTech/docker-compose.yml)
- Cấu hình `postgres:16-alpine` và `redis:7-alpine`.
- Thêm `healthcheck`:
  - `postgres`: `pg_isready -U edtech_user -d edtech_db`
  - `redis`: `redis-cli ping`
- Volumes và network chuẩn hóa.

#### [NEW] `.github/workflows/ci.yml`
- Setup JDK 21 (Temurin), Maven dependency cache.
- Tự động chạy `mvn clean test` kích hoạt Testcontainers trên runner của GitHub Actions.

#### [NEW] `.env.example`
- Biến môi trường mẫu cho local/cloud dev.

#### [NEW] `CODING_CONVENTION.md`
- Tài liệu quy ước code cho dự án.
- Ghi rõ rule: Tuyệt đối không sửa file migration `V1-V15` (hoặc các file Flyway đã apply), mọi thay đổi schema schema phải được thực hiện bằng cách tạo file migration mới (V16+).

---

### 2. Cấu hình Ứng dụng Backend (`backend/src/main/resources/` & `src/test/resources/`)

#### [MODIFY] [application.yml](file:///d:/EdTech/backend/src/main/resources/application.yml)
- Cấu hình chung cho Spring Boot, Flyway location `classpath:db/migration`.
- Mặc định: `spring.profiles.active: ${SPRING_PROFILES_ACTIVE:local}`.

#### [NEW] `backend/src/main/resources/application-local.yml`
- Profile `local` trỏ tới Docker Compose:
  - DataSource: `jdbc:postgresql://localhost:5432/edtech_db`
  - Redis: `redis://localhost:6379`
  - Flyway enabled: true

#### [NEW] `backend/src/main/resources/application-cloud.yml`
- Profile `cloud` trỏ tới Supabase PostgreSQL & Upstash Redis qua ENV vars.

#### [NEW] `backend/src/test/resources/application-test.yml`
- Profile tĩnh cho Test:
  - `spring.task.scheduling.enabled: false`
  - `app.jwt.secret: test-jwt-secret-minimum-256-bit-key-for-unit-and-integration-tests`

---

### 3. Bộ 15 File Flyway Baseline Migrations (`backend/src/main/resources/db/migration/`)

Cắt nguyên khối từ `schema.sql` phân bổ vào 15 file:

| File | Nội dung chính từ `schema.sql` | Bảng tạo |
|---|---|---|
| `V1__create_extension_and_users.sql` | `pgcrypto`, `btree_gist`, function `set_updated_at()`, bảng `users`, indexes `ux_users_email`, `ux_users_oauth`, `ix_users_role_status`, trigger `trg_users_updated` | `users` |
| `V2__create_refresh_tokens.sql` | Bảng `refresh_tokens`, indexes `ux_refresh_tokens_hash`, `ix_refresh_tokens_user`, trigger `trg_refresh_tokens_updated` | `refresh_tokens` |
| `V3__create_teacher_tables.sql` | Bảng `teacher_profiles` (kèm GIN index `ix_teacher_profiles_languages`), `teacher_documents`, `teacher_availabilities` (check range `ck_availability_time`), cùng toàn bộ triggers | `teacher_profiles`, `teacher_documents`, `teacher_availabilities` |
| `V4__create_subject_tables.sql` | Bảng `subjects`, `subject_proposals`, `teacher_subjects` (unique `uq_teacher_subjects`), cùng toàn bộ triggers | `subjects`, `subject_proposals`, `teacher_subjects` |
| `V5__create_pricing_packages.sql` | Bảng `pricing_packages` (check `ck_pricing_packages_positive`, `version`), trigger | `pricing_packages` |
| `V6__create_invoice_payment_tables.sql` | Bảng `invoices` (partial indexes payOS), bảng `payment_transactions` (append-only), trigger cho `invoices` | `invoices`, `payment_transactions` |
| `V7__create_student_packages.sql` | Bảng `student_packages` (checks counters `ck_student_packages_counters`, dates `ck_student_packages_dates`), trigger | `student_packages` |
| `V8__create_booking_tables.sql` | Bảng `bookings` (checks time & trial package, 2 exclusion constraints GiST `ex_booking_teacher_overlap` & `ex_booking_student_overlap`), `trial_requests` (partial unique `ux_trial_requests_pending_pair`), `session_reports`, cùng toàn bộ triggers | `bookings`, `trial_requests`, `session_reports` |
| `V9__create_review_stats_tables.sql` | Bảng `reviews` (check rating 1-5), `teacher_stats`, trigger cho `reviews` | `reviews`, `teacher_stats` |
| `V10__create_wallet_finance_tables.sql` | Bảng `wallets`, `ledger_entries` (append-only, check amount > 0), `teacher_bank_accounts` (partial unique `ux_bank_accounts_default`), cùng toàn bộ triggers | `wallets`, `ledger_entries`, `teacher_bank_accounts` |
| `V11__create_payout_refund_extension_tables.sql` | Bảng `payout_requests`, `refund_requests` (partial unique `ux_refund_requests_pending_package`), `package_extension_requests` (partial unique `ux_extension_requests_pending_package`), cùng toàn bộ triggers | `payout_requests`, `refund_requests`, `package_extension_requests` |
| `V12__create_learning_tables.sql` | Bảng `assignments`, `submissions`, cùng toàn bộ triggers | `assignments`, `submissions` |
| `V13__create_communication_tables.sql` | Bảng `attachments`, `conversations`, `messages`, `notifications`, cùng toàn bộ triggers | `attachments`, `conversations`, `messages`, `notifications` |
| `V14__create_audit_logs.sql` | Bảng `audit_logs` (append-only) | `audit_logs` |
| `V15__create_platform_settings.sql` | Bảng `platform_settings` (structured) + trigger `trg_platform_settings_updated` | `platform_settings` |

#### [NEW] `backend/src/main/resources/db/migration/V16__fix_schema_bugs.sql`
- **Fix Precision:** Sửa kiểu dữ liệu `commission_rate` trong `student_packages` từ `NUMERIC(5,4)` thành `NUMERIC(5,2)`.
- **Fix Constraints:** Thêm `CHECK` constraint cho cột `status` trong `bookings`: `CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED', 'EXPIRED'))`.
- **Fix Singleton:** Thêm cột giả `is_singleton BOOLEAN NOT NULL DEFAULT true` vào bảng `platform_settings` kèm theo `UNIQUE (is_singleton)` và `CHECK (is_singleton)`.

---

### 4. Testing Infrastructure (`backend/src/test/java/com/edtech/platform/`)

#### [NEW] `backend/src/test/java/com/edtech/platform/common/AbstractIntegrationTest.java`
- Khởi tạo Singleton PostgreSQL 16 (`postgres:16-alpine`) + Redis 7 (`redis:7-alpine`) Testcontainers.
- Dùng `@DynamicPropertySource` inject động cấu hình datasource và redis.

#### [NEW] `backend/src/test/java/com/edtech/platform/migration/FlywayMigrationTest.java`
- Kiểm thử toàn bộ 15 script DDL chạy sạch từ database rỗng.
- Kiểm tra tính hợp lệ của function `set_updated_at()`, trigger cập nhật `updated_at`, và exclusion constraints GiST chống trùng lịch.

#### [MODIFY] [backend/src/test/java/com/edtech/platform/ApiApplicationTests.java](file:///d:/EdTech/backend/src/test/java/com/edtech/platform/ApiApplicationTests.java)
- Kế thừa `AbstractIntegrationTest` để Spring Boot context load xanh 100%.

---

### 5. Nhật ký triển khai (`PROGRESS_BE_B.md`)

#### [NEW] `d:/EdTech/PROGRESS_BE_B.md`
- Tạo file nhật ký và append entry log sau khi hoàn thành Task 1 theo chuẩn quy ước.

---

## Verification Plan

### Automated Tests
1. Chạy kiểm thử toàn bộ migration và context load:
   ```bash
   cd backend
   mvn clean test
   ```
   - Xác nhận `FlywayMigrationTest` pass (toàn bộ 15 migrations V1->V15 chạy sạch sẽ trên PostgreSQL Testcontainers).
   - Xác nhận `ApiApplicationTests` pass.

### Manual Verification
1. Khởi động Docker Compose:
   ```bash
   docker compose up -d
   ```
2. Kiểm tra container health:
   ```bash
   docker compose ps
   ```
   - `edtech_postgres` và `edtech_redis` đều `healthy`.
