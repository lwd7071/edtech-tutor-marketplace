---
# Kế hoạch triển khai Backend — Thành viên B (Transaction & Operations)

> File này là KẾ HOẠCH THỰC HIỆN chi tiết cho Thành viên B.
> Bám theo: `PLANBE.md`, `SPEC.md`, `API_CONTRACT.md`, `ERD.md`, `ERROR_CODES.md`, `CODING_CONVENTION.md`.
> Không sửa các file đặc tả trên — luôn sửa code cho khớp chúng.

## Tổng quan ownership

Thành viên B sở hữu:
- `enrollment`: StudentPackage, session counters
- `payment`: Invoice, payOS, webhook
- `booking`: Booking, TrialRequest, SessionReport
- `finance`: Wallet, Ledger, BankAccount, Payout, Refund, Extension
- `admin`: approval queues, moderation, settings, dashboard, audit log
- `scheduler`: reminder, expiration, scheduled jobs
- Flyway migration, seed data, application config, Docker, Testcontainers

---

## TUẦN 1 — Foundation (Infra & Migration)

### Task 1.1: Docker Compose & Môi trường
- [x] Docker Compose đã có `postgres:16-alpine` + `redis:7-alpine` (sẵn trong repo)
- [x] Docker Desktop 4.87.0, Engine 29.7.2/API 1.55 và Compose 5.4.0 đã xác minh ngày 2026-08-25
- [x] `docker compose up -d` chạy thành công trên máy local; PostgreSQL và Redis đều healthy
- [x] PostgreSQL `edtech_db` nhận kết nối bằng `pg_isready`
- [x] Redis local trả `PONG`
- [x] Cấu hình `.env.example` với đầy đủ biến môi trường

### Task 1.2: Flyway Baseline Migration
> **B là người duy nhất tạo/sửa Flyway migration** (PLANBE.md quy tắc chống giẫm chân)

Các file V1–V17 tại `backend/src/main/resources/db/migration/` là baseline đã đóng băng theo quy ước nhóm. Ngày 2026-08-25, baseline đã được runtime-verify từ database PostgreSQL 16 rỗng bằng Testcontainers; Flyway áp dụng đúng 17 versioned migration và validate thành công.

> **Audit tĩnh 2026-08-24:** nội dung file hiện tại cho thấy tất cả `teacher_id` tham chiếu `teacher_profiles(id)` và V16 có singleton constraints cho `platform_settings`. Hai kết luận này vẫn phải được xác nhận bằng metadata runtime tại Task 1.9.

#### `V1__create_extension_and_users.sql`
```sql
-- btree_gist extension cho booking exclusion constraint
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255),
    full_name VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(500),
    phone VARCHAR(20),
    parent_full_name VARCHAR(255),
    parent_phone VARCHAR(20),
    parent_email VARCHAR(255),
    notify_parent BOOLEAN NOT NULL DEFAULT false,
    role VARCHAR(20) NOT NULL CHECK (role IN ('STUDENT','TEACHER','ADMIN')),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    email_verified BOOLEAN NOT NULL DEFAULT false,
    oauth_provider VARCHAR(50),
    oauth_subject VARCHAR(255),
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE UNIQUE INDEX uk_users_email ON users (lower(email));
CREATE UNIQUE INDEX uk_users_oauth ON users (oauth_provider, oauth_subject)
    WHERE oauth_provider IS NOT NULL AND oauth_subject IS NOT NULL;
CREATE INDEX idx_users_role_status ON users (role, status);
```

#### `V2__create_refresh_tokens.sql`
```sql
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    device_info VARCHAR(500),
    ip_address INET,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
```

#### `V3__create_teacher_tables.sql`
```sql
CREATE TABLE teacher_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE RESTRICT,
    bio TEXT,
    years_of_experience INT,
    languages VARCHAR(50)[],
    supports_online BOOLEAN NOT NULL DEFAULT false,
    supports_offline BOOLEAN NOT NULL DEFAULT false,
    location_address TEXT,
    introduction_video_url VARCHAR(500),
    profile_status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    rejection_reason TEXT,
    verified_badge BOOLEAN NOT NULL DEFAULT false,
    is_visible BOOLEAN NOT NULL DEFAULT false,
    approved_at TIMESTAMPTZ,
    approved_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_teacher_profiles_status_visible ON teacher_profiles (profile_status, is_visible);

CREATE TABLE teacher_documents (
    id UUID PRIMARY KEY,
    teacher_id UUID NOT NULL REFERENCES teacher_profiles(id) ON DELETE RESTRICT,
    document_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    cloudinary_public_id VARCHAR(255),
    secure_url VARCHAR(500),
    mime_type VARCHAR(100),
    file_size BIGINT,
    verification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    verified_by UUID REFERENCES users(id),
    verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE teacher_availabilities (
    id UUID PRIMARY KEY,
    teacher_id UUID NOT NULL REFERENCES teacher_profiles(id) ON DELETE RESTRICT,
    day_of_week VARCHAR(10) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT ck_availability_time_range CHECK (start_time < end_time)
);
```

#### `V4__create_subject_tables.sql`
```sql
CREATE TABLE subjects (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    education_level VARCHAR(50),
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_source VARCHAR(30) NOT NULL DEFAULT 'ADMIN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE subject_proposals (
    id UUID PRIMARY KEY,
    teacher_id UUID NOT NULL REFERENCES teacher_profiles(id),
    proposed_name VARCHAR(255) NOT NULL,
    education_level VARCHAR(50),
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    review_note TEXT,
    reviewed_by UUID REFERENCES users(id),
    reviewed_at TIMESTAMPTZ,
    created_subject_id UUID REFERENCES subjects(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE teacher_subjects (
    id UUID PRIMARY KEY,
    teacher_id UUID NOT NULL REFERENCES teacher_profiles(id),
    subject_id UUID NOT NULL REFERENCES subjects(id),
    level_description TEXT,
    experience_description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT uk_teacher_subjects_teacher_subject UNIQUE (teacher_id, subject_id)
);

CREATE INDEX idx_teacher_subjects_subject_active ON teacher_subjects (subject_id, is_active, teacher_id);
```

#### `V5__create_pricing_packages.sql`
```sql
CREATE TABLE pricing_packages (
    id UUID PRIMARY KEY,
    teacher_id UUID NOT NULL REFERENCES teacher_profiles(id),
    subject_id UUID NOT NULL REFERENCES subjects(id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    total_sessions INT NOT NULL,
    duration_days INT NOT NULL,
    price_vnd BIGINT NOT NULL,
    session_duration_minutes INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT ck_pricing_pkg_positive CHECK (
        total_sessions > 0 AND duration_days > 0
        AND price_vnd > 0 AND session_duration_minutes > 0
    )
);

CREATE INDEX idx_pricing_packages_teacher_status ON pricing_packages (teacher_id, status);
CREATE INDEX idx_pricing_packages_subject_price ON pricing_packages (subject_id, status, price_vnd);
```

#### `V6__create_invoice_payment_tables.sql`
```sql
CREATE TABLE invoices (
    id UUID PRIMARY KEY,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    student_id UUID NOT NULL REFERENCES users(id),
    teacher_id UUID NOT NULL REFERENCES teacher_profiles(id),
    pricing_package_id UUID NOT NULL REFERENCES pricing_packages(id),
    amount_vnd BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payos_order_code BIGINT,
    payos_payment_link_id VARCHAR(255),
    checkout_url TEXT,
    qr_code TEXT,
    payment_expired_at TIMESTAMPTZ,
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE UNIQUE INDEX uk_invoices_payos_order_code ON invoices (payos_order_code) WHERE payos_order_code IS NOT NULL;
CREATE UNIQUE INDEX uk_invoices_payos_link_id ON invoices (payos_payment_link_id) WHERE payos_payment_link_id IS NOT NULL;
CREATE INDEX idx_invoices_student_created ON invoices (student_id, created_at DESC);
CREATE INDEX idx_invoices_status_expired ON invoices (status, payment_expired_at);

CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES invoices(id),
    provider VARCHAR(30) NOT NULL DEFAULT 'PAYOS',
    provider_reference VARCHAR(255) NOT NULL UNIQUE,
    order_code BIGINT,
    amount_vnd BIGINT NOT NULL,
    transaction_datetime TIMESTAMPTZ,
    raw_payload JSONB,
    signature_valid BOOLEAN NOT NULL DEFAULT false,
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
    -- Append-only: không có updated_at, is_deleted
);
```

#### `V7__create_student_packages.sql`
```sql
CREATE TABLE student_packages (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES users(id),
    teacher_id UUID NOT NULL REFERENCES teacher_profiles(id),
    subject_id UUID NOT NULL REFERENCES subjects(id),
    pricing_package_id UUID NOT NULL REFERENCES pricing_packages(id),
    invoice_id UUID NOT NULL UNIQUE REFERENCES invoices(id),
    package_name_snapshot VARCHAR(255) NOT NULL,
    total_sessions INT NOT NULL,
    remaining_sessions INT NOT NULL DEFAULT 0,
    reserved_sessions INT NOT NULL DEFAULT 0,
    completed_sessions INT NOT NULL DEFAULT 0,
    refunded_sessions INT NOT NULL DEFAULT 0,
    purchase_price_vnd BIGINT NOT NULL,
    commission_rate NUMERIC(5,2) NOT NULL DEFAULT 5.00,
    starts_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT',
    locked_reason TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT ck_sp_counters_non_negative CHECK (
        remaining_sessions >= 0 AND reserved_sessions >= 0
        AND completed_sessions >= 0 AND refunded_sessions >= 0
    ),
    CONSTRAINT ck_sp_counter_total CHECK (
        remaining_sessions + reserved_sessions + completed_sessions + refunded_sessions = total_sessions
    ),
    CONSTRAINT ck_sp_dates CHECK (starts_at < expires_at)
);

CREATE INDEX idx_student_packages_student_status ON student_packages (student_id, status, expires_at);
CREATE INDEX idx_student_packages_teacher_status ON student_packages (teacher_id, status);
```

#### `V8__create_booking_tables.sql`
```sql
CREATE TABLE bookings (
    id UUID PRIMARY KEY,
    teacher_id UUID NOT NULL REFERENCES teacher_profiles(id),
    student_id UUID NOT NULL REFERENCES users(id),
    student_package_id UUID REFERENCES student_packages(id),
    subject_id UUID NOT NULL REFERENCES subjects(id),
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    delivery_mode VARCHAR(10) NOT NULL CHECK (delivery_mode IN ('ONLINE','OFFLINE')),
    meeting_link VARCHAR(500),
    location_address TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    is_trial BOOLEAN NOT NULL DEFAULT false,
    outside_availability_warning BOOLEAN NOT NULL DEFAULT false,
    cancel_reason TEXT,
    cancel_initiated_by VARCHAR(30),
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    expired_at TIMESTAMPTZ,
    settlement_processed BOOLEAN NOT NULL DEFAULT false,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT ck_bookings_time_range CHECK (start_time < end_time)
);

-- Exclusion constraint chống overlap Teacher
ALTER TABLE bookings
  ADD CONSTRAINT ex_booking_teacher_overlap
  EXCLUDE USING gist (
    teacher_id WITH =,
    tstzrange(start_time, end_time, '[)') WITH &&
  ) WHERE (status = 'SCHEDULED' AND is_deleted = false);

-- Exclusion constraint chống overlap Student
ALTER TABLE bookings
  ADD CONSTRAINT ex_booking_student_overlap
  EXCLUDE USING gist (
    student_id WITH =,
    tstzrange(start_time, end_time, '[)') WITH &&
  ) WHERE (status = 'SCHEDULED' AND is_deleted = false);

CREATE INDEX idx_bookings_teacher_start ON bookings (teacher_id, start_time);
CREATE INDEX idx_bookings_student_start ON bookings (student_id, start_time);
CREATE INDEX idx_bookings_status_end ON bookings (status, end_time);

CREATE TABLE trial_requests (
    id UUID PRIMARY KEY,
    teacher_id UUID NOT NULL REFERENCES teacher_profiles(id),
    student_id UUID NOT NULL REFERENCES users(id),
    subject_id UUID NOT NULL REFERENCES subjects(id),
    preferred_start_time TIMESTAMPTZ,
    note TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING','ACCEPTED','REJECTED','CANCELLED')),
    booking_id UUID REFERENCES bookings(id),
    rejection_reason TEXT,
    responded_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE UNIQUE INDEX uk_trial_requests_booking ON trial_requests (booking_id) WHERE booking_id IS NOT NULL;
CREATE INDEX idx_trial_requests_teacher_status ON trial_requests (teacher_id, status, created_at);
CREATE INDEX idx_trial_requests_student_status ON trial_requests (student_id, status, created_at);

CREATE TABLE session_reports (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL UNIQUE REFERENCES bookings(id),
    record_link VARCHAR(500),
    content TEXT,
    feedback TEXT,
    follow_up_note TEXT,
    teacher_self_rating SMALLINT CHECK (teacher_self_rating BETWEEN 1 AND 5),
    submitted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);
```

#### `V9__create_review_stats_tables.sql`
```sql
CREATE TABLE reviews (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL UNIQUE REFERENCES bookings(id),
    student_id UUID NOT NULL REFERENCES users(id),
    teacher_id UUID NOT NULL REFERENCES teacher_profiles(id),
    rating SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    is_visible BOOLEAN NOT NULL DEFAULT true,
    moderated_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_reviews_teacher_visible ON reviews (teacher_id, is_visible, created_at DESC);

CREATE TABLE teacher_stats (
    teacher_id UUID PRIMARY KEY REFERENCES teacher_profiles(id),
    average_rating NUMERIC(3,2) DEFAULT 0,
    bayesian_rating NUMERIC(5,3) DEFAULT 0,
    review_count INT NOT NULL DEFAULT 0,
    completed_session_count INT NOT NULL DEFAULT 0,
    completion_rate NUMERIC(5,2) DEFAULT 0,
    trial_session_count INT NOT NULL DEFAULT 0,
    trial_conversion_rate NUMERIC(5,2) DEFAULT 0,
    global_rank INT DEFAULT 0,
    calculated_at TIMESTAMPTZ
);
```

#### `V10__create_wallet_finance_tables.sql`
```sql
CREATE TABLE wallets (
    id UUID PRIMARY KEY,
    teacher_id UUID NOT NULL UNIQUE REFERENCES teacher_profiles(id),
    pending_balance_vnd BIGINT NOT NULL DEFAULT 0
        CHECK (pending_balance_vnd >= 0),
    available_balance_vnd BIGINT NOT NULL DEFAULT 0
        CHECK (available_balance_vnd >= 0),
    reserved_balance_vnd BIGINT NOT NULL DEFAULT 0
        CHECK (reserved_balance_vnd >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY,
    wallet_id UUID NOT NULL REFERENCES wallets(id),
    entry_type VARCHAR(50) NOT NULL,
    amount_vnd BIGINT NOT NULL CHECK (amount_vnd > 0),
    balance_bucket VARCHAR(20) NOT NULL,
    direction VARCHAR(10) NOT NULL CHECK (direction IN ('CREDIT','DEBIT')),
    reference_type VARCHAR(50),
    reference_id UUID,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
    -- Append-only: không có updated_at, is_deleted
);

CREATE INDEX idx_ledger_entries_wallet_created ON ledger_entries (wallet_id, created_at DESC);

CREATE TABLE teacher_bank_accounts (
    id UUID PRIMARY KEY,
    teacher_id UUID NOT NULL REFERENCES teacher_profiles(id),
    bank_bin VARCHAR(20) NOT NULL,
    bank_name VARCHAR(100) NOT NULL,
    account_number_encrypted TEXT NOT NULL,
    account_holder_name VARCHAR(255) NOT NULL,
    is_verified BOOLEAN NOT NULL DEFAULT false,
    is_default BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

-- Partial unique: chỉ 1 default per teacher
CREATE UNIQUE INDEX uk_bank_account_default
    ON teacher_bank_accounts (teacher_id)
    WHERE is_default = true AND is_deleted = false;
```

#### `V11__create_payout_refund_extension_tables.sql`
```sql
CREATE TABLE payout_requests (
    id UUID PRIMARY KEY,
    teacher_id UUID NOT NULL REFERENCES teacher_profiles(id),
    wallet_id UUID NOT NULL REFERENCES wallets(id),
    bank_account_id UUID NOT NULL REFERENCES teacher_bank_accounts(id),
    amount_vnd BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    teacher_note TEXT,
    admin_note TEXT,
    bank_reference VARCHAR(255),
    proof_public_id VARCHAR(255),
    proof_url VARCHAR(500),
    transferred_at TIMESTAMPTZ,
    processed_by UUID REFERENCES users(id),
    processed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_payout_requests_status ON payout_requests (status, created_at);

CREATE TABLE refund_requests (
    id UUID PRIMARY KEY,
    student_package_id UUID NOT NULL REFERENCES student_packages(id),
    student_id UUID NOT NULL REFERENCES users(id),
    reason TEXT NOT NULL,
    requested_sessions INT NOT NULL CHECK (requested_sessions > 0),
    approved_sessions INT DEFAULT 0 CHECK (approved_sessions >= 0),
    refund_amount_vnd BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    admin_note TEXT,
    bank_name VARCHAR(100),
    bank_bin VARCHAR(20),
    account_number_encrypted TEXT,
    account_holder_name VARCHAR(255),
    bank_reference VARCHAR(255),
    proof_public_id VARCHAR(255),
    proof_url VARCHAR(500),
    processed_by UUID REFERENCES users(id),
    processed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_refund_requests_status ON refund_requests (status, created_at);

CREATE TABLE package_extension_requests (
    id UUID PRIMARY KEY,
    student_package_id UUID NOT NULL REFERENCES student_packages(id),
    student_id UUID NOT NULL REFERENCES users(id),
    reason TEXT NOT NULL,
    requested_expiry_date TIMESTAMPTZ NOT NULL,
    approved_expiry_date TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    admin_note TEXT,
    reviewed_by UUID REFERENCES users(id),
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);
```

#### `V12__create_learning_tables.sql`
```sql
CREATE TABLE assignments (
    id UUID PRIMARY KEY,
    teacher_id UUID NOT NULL REFERENCES teacher_profiles(id),
    student_id UUID NOT NULL REFERENCES users(id),
    subject_id UUID NOT NULL REFERENCES subjects(id),
    title VARCHAR(255) NOT NULL,
    assignment_type VARCHAR(30) NOT NULL DEFAULT 'FREEFORM',
    content_blocks JSONB,
    quiz_schema JSONB,
    due_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_assignments_student_status ON assignments (student_id, status, due_at);

CREATE TABLE submissions (
    id UUID PRIMARY KEY,
    assignment_id UUID NOT NULL REFERENCES assignments(id),
    student_id UUID NOT NULL REFERENCES users(id),
    content_blocks JSONB,
    submitted_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    score NUMERIC(5,2),
    feedback_text TEXT,
    graded_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_submissions_assignment_student ON submissions (assignment_id, student_id);

CREATE TABLE attachments (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users(id),
    attachable_type VARCHAR(50),
    attachable_id UUID,
    cloudinary_public_id VARCHAR(255),
    secure_url VARCHAR(500),
    original_filename VARCHAR(500),
    mime_type VARCHAR(100),
    file_size BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);
```

#### `V13__create_communication_tables.sql`
```sql
CREATE TABLE conversations (
    id UUID PRIMARY KEY,
    teacher_id UUID NOT NULL REFERENCES teacher_profiles(id),
    student_id UUID NOT NULL REFERENCES users(id),
    last_message_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT uk_conversations_pair UNIQUE (teacher_id, student_id)
);

CREATE TABLE messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations(id),
    sender_id UUID NOT NULL REFERENCES users(id),
    client_message_id UUID,
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    content TEXT,
    attachment_id UUID REFERENCES attachments(id),
    sent_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE UNIQUE INDEX uk_messages_client_msg ON messages (sender_id, client_message_id) WHERE client_message_id IS NOT NULL;
CREATE INDEX idx_messages_conversation_sent ON messages (conversation_id, sent_at DESC);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255),
    content TEXT,
    reference_type VARCHAR(50),
    reference_id UUID,
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
    -- Notification không cần updated_at/is_deleted theo ERD
);

CREATE INDEX idx_notifications_user_read ON notifications (user_id, is_read, created_at DESC);
```

#### `V14__create_audit_logs.sql`
```sql
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    actor_id UUID REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(50),
    target_id UUID,
    before_data JSONB,
    after_data JSONB,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
    -- Append-only: không có updated_at, is_deleted
);

CREATE INDEX idx_audit_logs_actor ON audit_logs (actor_id, created_at DESC);
CREATE INDEX idx_audit_logs_target ON audit_logs (target_type, target_id, created_at DESC);
```

### Task 1.3: Thiết lập Testcontainers
- [x] Thêm dependency Testcontainers PostgreSQL vào `pom.xml`:
  ```xml
  <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>postgresql</artifactId>
      <scope>test</scope>
  </dependency>
  <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>junit-jupiter</artifactId>
      <scope>test</scope>
  </dependency>
  ```
- [x] Tạo `application-test.yml` dùng Testcontainers:
  ```yaml
  spring:
    datasource:
      url: jdbc:tc:postgresql:16-alpine:///edtech_test
      driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver
    jpa:
      hibernate:
        ddl-auto: validate
    flyway:
      enabled: true
  ```
- [x] Tạo và chạy thật context smoke test (`ApiApplicationTests`) trên Testcontainers
- [x] Tạo `@TestConfiguration` base cho integration test dùng PostgreSQL container

### Task 1.4: Environment Variables
- [x] Xóa credential thật khỏi fallback trong `application.yml`; default profile fail-fast, local profile chỉ dùng credential Docker local-safe
- [x] Chủ sở hữu xác nhận đã rotate Supabase và Upstash credential từng lộ; giá trị mới chỉ lưu trong `.env.cloud` bị Git ignore
- [x] Đổi Hibernate `ddl-auto` thành `validate`; schema chỉ thay đổi qua Flyway
- [x] Tắt `show-sql` và `open-in-view` ở cấu hình mặc định/test/local
- [x] Tạo file `.env.example` liệt kê tất cả env vars cần thiết
- [x] `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_URL`, `APP_JWT_SECRET` được inject từ môi trường; integration test không còn fallback local/cloud
- [x] Chạy secret scan trên working tree và Git history; ghi kết quả/credential đã rotate vào `PROGRESS_BE_B.md` mà không ghi giá trị secret

### ✅ Checkpoint Tuần 1
- [x] Không còn credential thật trong source/history; credential đã lộ được rotate
- [x] Runtime config dùng `ddl-auto=validate`; `show-sql` không bật mặc định/production
- [x] `docker version` và `docker compose version` thành công
- [x] Migration V1–V17 chạy được trên PostgreSQL Testcontainers từ database rỗng
- [x] Context test chạy thật, không bị skip
- [x] `FlywayMigrationTest`: tests run `6`, skipped `0`, failures `0`, errors `0`
- [x] Baseline gate báo đúng 17 versioned migration applied và `validate` thành công; full gate V1–V18 cũng validate thành công

> **Evidence 2026-08-25:** `mvn -Dtest=FlywayMigrationTest test` chạy đủ 6 test, failures/errors/skipped đều bằng 0. Full `mvn test` chạy 88 test, failures/errors/skipped đều bằng 0.

---

## TUẦN 1 — Các task thực hiện ngoài kế hoạch gốc

> Các task bên dưới được thực hiện để hỗ trợ remediation của Thành viên A (A-02 → A-09) và đảm bảo
> ArchUnit guard của A có thể hoạt động đúng. Không nằm trong kế hoạch 8 tuần ban đầu.

### Task 1.5: Migration V17 — Fix BaseEntity compatibility
- [x] Tạo `V17__add_deleted_to_refresh_tokens.sql` (baseline đóng băng nhưng audit sau đó xác nhận tên cột chưa tương thích)
  - Thêm cột `deleted BOOLEAN NOT NULL DEFAULT false` vào bảng `refresh_tokens`
  - V17 tạo `deleted`, trong khi `BaseEntity` map `is_deleted`; remediation forward-only nằm trong V18, không sửa checksum V17
  - Theo yêu cầu từ A-02 — Thành viên A phát hiện trong quá trình remediation

### Task 1.6: Facade contracts A ↔ B

Tạo 3 facade interface + implementation stub dùng `JdbcTemplate` để A có thể migrate
các module `ranking`, `catalog`, `learning` mà không cần domain entity của B:

- [x] **`enrollment.facade.EnrollmentFacade`** (interface + impl)
  - `hasValidRelationship(teacherId, studentId)` — kiểm tra StudentPackage ACTIVE/COMPLETED
  - `hasStudentPackage(pricingPackageId)` — kiểm tra PricingPackage đã được mua chưa
  - Dùng: `PricingPackageService` (A) để check trước khi cho phép INACTIVE

- [x] **`booking.facade.BookingEligibilityFacade`** (interface + impl + `BookingStatsSnapshot` DTO)
  - `getTeacherIdForReviewableBooking(studentId, bookingId)` — kiểm tra booking COMPLETED, student khớp
  - `getTeacherBookingStats(teacherId)` — trả về `BookingStatsSnapshot(completedSessions, totalSessions, trialSessions)`
  - Dùng: `ranking.service.ReviewService` và `ranking.service.TeacherStatsService` (A)

- [x] **`admin.facade.PlatformSettingsFacade`** (interface + impl)
  - `getBayesianMinimumReviews()` — đọc từ bảng `platform_settings`
  - Dùng: `ranking.service.TeacherStatsService` (A) cho Bayesian calculation

> **Ghi chú ArchUnit:** 3 impl này dùng `JdbcTemplate` trực tiếp và được inventory explicit
> trong `archunit_ignore_patterns.txt`. Đây là approved exception (B module, đọc bảng B sở hữu).

Dependency B cần từ module A đã được chốt bằng public facade:

- [x] Catalog expose `PricingPackageFacade` + immutable snapshot để B đọc package purchasable
- [x] Teacher expose `TeacherApprovalFacade` + snapshot/approval operations cho Admin B
- [x] Chốt DTO/interface bằng public facade; B không gọi repository của `catalog` hoặc `teacher`

### Task 1.7: Module Skeletons
- [x] Tạo đầy đủ `package-info.java` cho tất cả sub-packages của:
  - `booking`: controller, domain, dto.request, dto.response, repository, service
  - `enrollment`: controller, domain, dto.request, dto.response, repository, service
  - `payment`: controller, domain, dto.request, dto.response, repository, service
  - `finance`: controller, domain, dto.request, dto.response, repository, service
  - `admin`: controller, domain, dto.request, dto.response, repository, service
  - `scheduler`: (TeacherStatsJob đã implement)

### Task 1.8: TeacherStatsJob — Scheduled Job
- [x] Implement `scheduler.TeacherStatsJob`
  - Chạy `@Scheduled(cron = "0 0 2 * * ?")` — 02:00 mỗi ngày
  - `@SchedulerLock` với ShedLock qua Redis (lockAtLeastFor=5m, lockAtMostFor=30m)
  - Fetch tất cả teacher `APPROVED` → gọi `TeacherStatsFacade.recalculateTeacherStats()`
  - Sau đó gọi `TeacherStatsFacade.updateAllGlobalRanks()`; scheduler không gọi trực tiếp ranking service
  - Invalidate Redis cache `GLOBAL_RANKING`

### Task 1.9: Hard gate xác minh V1–V17

> **Chặn cứng:** Không chốt nội dung hoặc tạo V18 trước khi toàn bộ checklist này xanh. `BUILD SUCCESS` không đủ nếu Surefire báo test bị skip.

- [x] Cài Docker và xác nhận `docker version`, `docker compose version` thành công
- [x] Chạy `mvn -Dtest=FlywayMigrationTest test` trên database PostgreSQL Testcontainers rỗng
- [x] Xác nhận Surefire: tests run `6`, skipped `0`, failures `0`, errors `0`
- [x] Xác nhận Flyway có đúng 17 versioned migration applied và `validate` thành công
- [x] Audit metadata runtime, không chỉ đọc file SQL:
  - Tất cả FK `teacher_id` của V1–V17 tham chiếu `teacher_profiles(id)`
  - V16 có `uq_platform_settings_singleton` và `ck_platform_settings_singleton`
  - `bookings.status` có CHECK đủ `SCHEDULED/COMPLETED/CANCELLED/EXPIRED`
  - `student_packages.commission_rate` có precision/scale `(5,2)`
- [x] Ghi số test run/skip/fail/error và Docker/PostgreSQL version vào `PROGRESS_BE_B.md`

Nếu phát hiện lỗi mới trong V1–V17: không sửa migration cũ; thêm lỗi vào bảng remediation dưới đây, xử lý bằng V18 và bổ sung regression assertion.

| Phát hiện | Query/Test tái hiện | Cách sửa trong V18 | Regression test | Trạng thái |
|---|---|---|---|---|
| V2 đã có `is_deleted`, V17 thêm cột `deleted` trùng nghĩa | Baseline metadata assertion xác nhận cả hai cột | OR hai cờ vào `is_deleted`, sau đó drop `deleted` | `FlywayV17BaselineTest` + full V18 metadata test | Đã sửa |
| `teacher_profiles.languages` là `varchar(50)[]`, entity cũ kỳ vọng `text[]` qua custom type | Hibernate `ddl-auto=validate` fail | Widen lossless sang `text[]`; dùng Hibernate 6 native ARRAY mapping | Context/Flyway migration test | Đã sửa |

### Task 1.10: Migration V18 — Harden business invariants

- [x] Hoàn thành `V18__harden_business_invariants.sql`; Docker gate và V1–V18 test đều xanh
- [x] Remediation V17: hợp nhất an toàn `refresh_tokens.deleted` vào `is_deleted`, sau đó bỏ cột trùng; fail nếu không tồn tại cột đích hợp lệ
- [x] Commission và settings:
  - Đổi default `platform_settings.commission_rate` thành `5.00`
  - Seed đúng một settings row nếu bảng rỗng, tương thích singleton constraint của V16
  - Backfill `student_packages.commission_rate IS NULL` từ settings rồi đặt column `NOT NULL`
  - Không tự đổi snapshot khác `5.00`; nếu có, fail rõ ràng để xác minh đó là cấu hình hợp lệ hay dữ liệu do default sai
- [x] Thêm CHECK cho status/type còn thiếu theo enum trong SPEC/API contract; không tạo lại constraint booking đã có
-- [x] Allow-list và default đích phải được khóa như sau:

  | Column | Allow-list | Default đích / xử lý legacy đã biết |
  |---|---|---|
  | `users.status` | `PENDING_VERIFICATION, ACTIVE, LOCKED, DISABLED` | Default `PENDING_VERIFICATION` |
  | `teacher_profiles.profile_status` | `DRAFT, PENDING_APPROVAL, APPROVED, REJECTED` | Default `DRAFT`; legacy `PENDING` chỉ map sang `PENDING_APPROVAL` sau khi audit xác nhận nguồn |
  | `teacher_documents.verification_status` | `PENDING, VERIFIED, REJECTED` | Default `PENDING` |
  | `subject_proposals.status` | `PENDING, APPROVED, REJECTED` | Default `PENDING` |
  | `pricing_packages.status` | `DRAFT, ACTIVE, INACTIVE` | Default `DRAFT` |
  | `invoices.status` | `PENDING, PAID, CANCELLED, EXPIRED` | Default `PENDING` |
  | `student_packages.status` | `PENDING_PAYMENT, ACTIVE, COMPLETED, REFUND_PENDING, REFUNDED, LOCKED_EXPIRED` | Default `PENDING_PAYMENT` |
  | `trial_requests.status` | `PENDING, ACCEPTED, REJECTED, CANCELLED` | Default `PENDING`; skip nếu metadata cho thấy CHECK tương đương đã có |
  | `payout_requests.status` | `PENDING, PROCESSING, SUCCEEDED, REJECTED, FAILED` | Default `PENDING` |
  | `refund_requests.status` | `PENDING, APPROVED, PROCESSING, REFUNDED, REJECTED, FAILED` | Default `PENDING` |
  | `package_extension_requests.status` | `PENDING, APPROVED, REJECTED` | Default `PENDING` |
  | `assignments.assignment_type` | `SYSTEM_QUIZ, FREEFORM` | Không đổi dữ liệu hợp lệ |
  | `assignments.status` | `DRAFT, PUBLISHED, CLOSED` | Default `DRAFT`; `ASSIGNED` không tự map nếu chưa xác minh |
  | `submissions.status` | `DRAFT, SUBMITTED, GRADED` | Default `DRAFT` |
  | `messages.message_type` | `TEXT, IMAGE, FILE` | Default `TEXT` |

- [x] Trước mỗi constraint, audit dữ liệu bằng query dạng:
  ```sql
  SELECT <column>, count(*)
  FROM <table>
  WHERE <column> NOT IN (<allow_list>)
  GROUP BY <column>;
  ```
- [x] Chỉ sửa cơ học các legacy value đã có mapping được duyệt; dữ liệu không rõ nghĩa phải `RAISE EXCEPTION` kèm tên bảng, constraint và số row
- [x] Không xóa row hoặc ép về một trạng thái tùy ý để migration tiếp tục
- [x] Test V1–V18 từ database rỗng, fixture legacy hợp lệ và fixture legacy không hợp lệ

### Task 1.11: Shared API contract prerequisite

> Thay đổi `common`/response contract phải nằm trong PR riêng và merge trước các controller feature của B. Phối hợp với A để không tồn tại hai response format song song.

- [x] Chuẩn hóa mọi REST JSON response theo đúng năm field cấp cao nhất trong `API_CONTRACT.md`: `success`, `message`, `data`, `errors`, `meta`
- [x] Success: `errors=null`; error: `data=null`; field không có giá trị vẫn trả `null`, không đổi kiểu hoặc tự ý bỏ field theo endpoint
- [x] Chuẩn hóa `errors[]` thành `{ code, field, message }`; FE/backend xử lý theo `code`, không điều khiển luồng bằng nội dung message
- [x] `@RestControllerAdvice` là nơi duy nhất map exception → `ErrorCode` → HTTP status/envelope; không trả raw `exception.getMessage()`
- [x] Bao phủ input error còn thiếu: malformed JSON, sai enum/type, thiếu query/request part, constraint violation và multipart validation
- [x] Giữ request/correlation ID trong log và response header; không đưa stack trace, SQL, provider payload hoặc secret vào response
- [x] Ngoại lệ không dùng envelope chỉ gồm `204 No Content`, file/stream, WebSocket, OAuth redirect và webhook phải theo contract provider
- [x] Contract/architecture test bao phủ success, pagination, validation, business error, auth/forbidden và unexpected `500`; full Docker suite đã xanh

### Task 1.12: Chiến lược soft delete xuyên module

> `is_deleted` trong schema chỉ là nền tảng lưu trữ, không tự làm cho JPA query/delete an toàn. Soft delete cũng không thay thế state machine nghiệp vụ và không đồng nghĩa mọi entity đều được phép expose API `DELETE`.

> **Checkpoint nền tảng 2026-08-25 — hoàn thành:** policy, native-query guard và integration-test mẫu cho entity versioned đã được tạo và chạy xanh. Việc gắn annotation/test lên từng entity B được thực hiện tại task tạo entity ở Tuần 3–7; không kéo domain chưa tồn tại vào Tuần 1.

- [x] Hoàn thành policy/test foundation của Task 1.12; các checkbox dưới đây là checklist bắt buộc gắn vào từng task entity tương ứng ở Tuần 3–7.

- [x] Với mỗi business entity mutable của B kế thừa `BaseEntity`, khai báo soft-delete mapping ngay trên entity:
  - `@SQLDelete` đổi `is_deleted = true`, không phát sinh physical `DELETE`
  - Theo convention hiện hữu của project, dùng `@Where(clause = "is_deleted = false")`; chỉ chuyển sang `@SQLRestriction` nếu toàn project thống nhất trong một thay đổi riêng, không trộn hai chiến lược tùy entity
  - `@SQLDelete` phải dùng đúng tên bảng và đúng thứ tự parameter Hibernate yêu cầu; entity có `@Version` phải đưa cả `id` và `version` vào điều kiện optimistic locking, không copy mẫu chỉ có `id`
  - Có integration test PostgreSQL chứng minh `repository.delete()` thực hiện `UPDATE`, row còn trong DB và query JPA thông thường không đọc lại row đã xóa
- [x] `BaseEntity` chỉ cung cấp field/audit chung; không coi việc kế thừa `BaseEntity` là đã hoàn tất soft delete vì SQL tùy thuộc từng bảng và version mapping
- [x] `payment_transactions`, `ledger_entries` và `audit_logs` tiếp tục append-only: không kế thừa `BaseEntity`, không có `is_deleted`, không expose repository/service delete hoặc update lịch sử
- [x] Mọi native SQL, JdbcTemplate, projection, aggregation và scheduled job phải thêm `is_deleted = false` cho từng bảng mutable tham gia query; Hibernate filter không áp dụng cho native query
- [x] Review riêng các join tới parent đã soft-delete: luồng lịch sử không được phụ thuộc vào việc dereference một association bị Hibernate filter ẩn; Invoice/StudentPackage/Booking dùng snapshot hoặc projection phù hợp để lịch sử vẫn đọc được
- [x] Phân biệt state transition và soft delete:
  - Booking được người dùng hủy phải giữ `is_deleted = false`, chuyển `status = CANCELLED` và lưu lý do/thời điểm để hoàn lượt, thống kê và audit
  - Invoice hủy/hết hạn dùng `CANCELLED`/`EXPIRED`; PricingPackage ngừng bán dùng `INACTIVE`; refund, payout, extension và trial dùng trạng thái nghiệp vụ tương ứng
  - Không dùng soft delete để bỏ qua settlement, counter invariant, lịch sử tài chính hoặc điều kiện state machine
- [x] PricingPackage chưa có API delete trong `API_CONTRACT.md`: Teacher chỉ chuyển sang `INACTIVE`; chỉ bổ sung soft-delete operation khi có contract/authorization/audit/retention rule được duyệt
- [x] Message recall/delete và Admin/GDPR cleanup chưa thuộc MVP vì SPEC/API chưa chốt hành vi ẩn hay hiển thị placeholder, retention và quyền thực hiện; không tự thêm endpoint hoặc suy diễn `is_deleted` thành tính năng thu hồi message
- [x] Không tạo `find...IncludingDeleted` đại trà. Chỉ entity có use case restore được duyệt mới có custom/native repository query bỏ qua filter, kèm authorization, audit log và xử lý unique/partial-index trong cùng transaction
- [x] Không hard-delete hay tự restore row để né unique constraint. Nếu cần tái kích hoạt cấu hình/liên kết cũ, service phải xác định đúng row, kiểm tra ownership/trạng thái và thực hiện restore idempotent

---

## TUẦN 2 — Admin Approval & Audit Log

### Task 2.1: Audit Log Module
- [x] Entity: `AuditLog` immutable (append-only, không soft delete, không `BaseEntity`)
  - File: `admin/domain/AuditLog.java`
- [x] Repository append-only: `AuditLogRepository` chỉ expose `append()` qua `EntityManager.persist()`; không có update/delete
  - File: `admin/repository/AuditLogRepository.java`
- [x] Service: `AuditLogService.append(actorId, action, targetType, targetId, before, after, context)`
  - File: `admin/service/AuditLogService.java`
  - Ghi mọi action thay đổi trạng thái
- [x] `AuditSnapshotMapper` tập trung whitelist snapshot Teacher/Subject Proposal/User; không serialize entity và không lưu email/token/password/credential
- [x] Mapping `JSONB`/`INET` được kiểm tra bằng PostgreSQL Testcontainers insert thật
- [x] `AuditLogView` và API đọc audit được hoàn tất ở Task 7.6

### Task 2.2: Admin Teacher Approval
- [x] Controller: `AdminApprovalController`
  - `GET /api/admin/teachers/approvals` — list theo status, mặc định pending (phân trang)
  - `POST /api/admin/teachers/{id}/approve`
  - `POST /api/admin/teachers/{id}/reject`
- [x] DTO:
  - `ApproveTeacherRequest` (chỉ cần body rỗng hoặc optional note)
  - `RejectRequest` (có field `reason`)
  - `TeacherApprovalSnapshot` kèm document snapshots; list dùng một bulk query, không N+1
- [x] Service orchestration: `AdminApprovalService`
  - Gọi Teacher approval facade do module A sở hữu để list/approve/reject profile
  - Tạo AuditLog cho mỗi action
  - **Chỉ giao tiếp qua public facade/DTO, không gọi repository module A**
  - Pessimistic lock bảo đảm double-click/race chỉ một request thành công; request sau trả `409 TEACHER_APPROVAL_ALREADY_PROCESSED`

### Task 2.3: Admin Subject Proposal
- [x] Controller: `AdminApprovalController`
  - `GET /api/admin/subject-proposals`
  - `POST /api/admin/subject-proposals/{id}/approve` (resolution: CREATE_NEW | LINK_EXISTING)
  - `POST /api/admin/subject-proposals/{id}/reject`
- [x] DTO: `ApproveSubjectProposalRequest`, `RejectRequest`, `SubjectProposalSnapshot`
- [x] Service orchestration qua `SubjectApprovalFacade`
  - Tạo Subject mới hoặc link existing + tạo TeacherSubject
  - AuditLog
  - Pessimistic lock; proposal đã xử lý trả `409 SUBJECT_PROPOSAL_ALREADY_PROCESSED`

### Task 2.4: User Moderation
- [x] Controller endpoint: `PATCH /api/admin/users/{id}/status`
- [x] DTO: `ChangeUserStatusRequest` (chỉ `ACTIVE | LOCKED`, reason bắt buộc)
- [x] `IdentityModerationFacade`: chỉ `ACTIVE ↔ LOCKED`, cấm tự moderation và cấm target ADMIN, revoke refresh token khi lock
- [x] JWT filter kiểm tra current status qua Redis `auth:user-status:{userId}` TTL 30 giây; cache miss/failure fallback DB
- [x] Lock/unlock và email verification cùng evict trước mutation, write-through sau commit; AuditLog nằm cùng transaction orchestration

### ✅ Checkpoint Tuần 2
- [x] Bảy endpoint Task 2 dùng envelope năm field, pagination meta và sort allow-list
- [x] Teacher/Subject approve-reject và User moderation ghi AuditLog whitelist trong cùng transaction
- [x] Double-click/race được khóa và trả `409`; JWT hiện hữu bị chặn theo current account status
- [x] Full `mvn test` chạy bằng Docker/Testcontainers, không skip (evidence trong `PROGRESS_BE_B.md`)

---

## TUẦN 3 — Invoice & StudentPackage Schema Prep

> Task 3 giữ payOS sau port `PaymentGateway`; không có controller, SDK hoặc network call provider trong tuần này. Local mặc định `disabled`, fake stateful chỉ nằm trong test.

### Task 3.0: Migration V20 — Payment-ready foundation
- [x] Tạo forward-only `V20__prepare_payment_domain.sql`; không sửa checksum V1–V19
- [x] Invoice có `idempotency_key`, canonical `request_fingerprint`, unique `(student_id, idempotency_key)` không phụ thuộc soft delete
- [x] Legacy fingerprint dùng SHA-256 chính xác của `id|amount_vnd|created_at_utc`; legacy order code null được backfill bằng sequence
- [x] Tạo `invoice_number_seq`, `payos_order_code_seq`, `pgcrypto` và audit dữ liệu trước các CHECK payment/package/ledger
- [x] Runtime gate V1–V20 từ database rỗng + fixture legacy (không được skip)

### Task 3.1: Enrollment Module — Domain & Repository
- [x] Entity: `StudentPackage`
  - File: `enrollment/domain/StudentPackage.java`
  - Enum: `StudentPackageStatus` (PENDING_PAYMENT, ACTIVE, COMPLETED, REFUND_PENDING, REFUNDED, LOCKED_EXPIRED)
  - `@Version` field
  - Counter invariant method: `validateCounterTotal()`
- [x] `PENDING_PAYMENT` là legacy-only: Invoice đại diện giai đoạn chờ; factory public chỉ tạo `ACTIVE` sau payment thành công
- [x] Snapshot/scalar ID, counter/date/price/commission invariants và soft delete `id + version` có unit/integration test
- [x] Repository: `StudentPackageRepository`
  - `findByIdForUpdate` (PESSIMISTIC_WRITE)
  - `findByStudentIdAndStatus(studentId, status, Pageable)`

### Task 3.2: Payment Module — Domain & Repository
- [x] Entity: `Invoice`
  - File: `payment/domain/Invoice.java`
  - Enum: `InvoiceStatus` (PENDING, PAID, CANCELLED, EXPIRED)
  - State transition methods: `markPaid()`, `expire()`, `cancel()`; link chỉ attach khi `PENDING` và không được overwrite khác dữ liệu
- [x] Entity: `PaymentTransaction` (append-only, không soft delete)
  - File: `payment/domain/PaymentTransaction.java`
- [x] Tách `InvoiceCommandRepository`/`InvoiceQueryRepository`; không extends Spring Data CRUD, không expose `save/update/delete`, command mutation bắt buộc lock
- [x] `PaymentTransactionRepository` custom chỉ append/read theo provider reference
- [x] Canonical fingerprint tự build byte sequence UTF-8 cố định, không dùng Jackson/Map; golden string/bytes/digest tests
- [x] `PaymentGateway` trung lập provider và fake stateful bao phủ create/reconcile/timeout/reject/signature/replay/mismatch
- [x] Configuration payOS fail-fast khi bật provider mà thiếu credential; Git chỉ chứa placeholder

### Task 3.3: Finance Module — Domain Skeleton
- [x] Entity: `Wallet`
  - File: `finance/domain/Wallet.java`
  - Balance check methods
  - `@Version` field
- [x] Entity: `LedgerEntry` (append-only)
  - File: `finance/domain/LedgerEntry.java`
- [x] Repository: `WalletRepository`, `LedgerEntryRepository`; Wallet lock/versioned soft delete, Ledger custom append-only

### Task 3.4: Tích hợp PricingPackage facade của module A
- [x] B consume PricingPackage snapshot facade do A expose; không tạo facade ngược chiều trong `enrollment`
- [x] Snapshot phục vụ Invoice/StudentPackage gồm package ID, teacher profile ID, subject ID, name, total sessions, duration, price, session duration và status string

### Task 3.5: Repository query baseline
- [x] Invoice/StudentPackage/Ledger list nhận `Pageable`; expiry query bị giới hạn; không filter collection trong memory
- [x] DTO projection/entity graph/fetch join phù hợp để mapper không phát sinh N+1
- [x] Filter/sort dùng allow-list theo `API_CONTRACT.md`; không filter collection đã load trong memory
- [x] Mutable entity query dùng Hibernate `@Where`; native sequence query không đọc bảng mutable; lịch sử giữ scalar ID/snapshot
- [x] Chạy PostgreSQL integration + lưu `EXPLAIN ANALYZE` cho invoice list/expiry/ledger sau khi Docker daemon hoạt động
- [x] Query shape bám các index hiện có; planner trên fixture nhỏ không được dùng làm bằng chứng kết luận index sai, re-check dataset lớn ở Task 8

### Task 3.6: Architecture guard
- [x] Maven chạy rule thật: Invoice repository không extends `Repository`/`CrudRepository`/`JpaRepository`, cấm generic mutation method
- [x] Payment service không dùng `EntityManager`; command/query repository dependency bị tách theo package; chỉ payment repository được dùng `EntityManager`
- [x] Negative fixtures cố ý vi phạm CRUD, method name, EntityManager và command/query boundary để chứng minh rule bắt lỗi

### ✅ Checkpoint Tuần 3
- [x] Domain classes compile; focused unit/architecture tests: 23 run, 0 failure/error/skip
- [x] Entity dùng scalar ID/snapshot đúng boundary và PricingPackage facade có sale status
- [x] Không có payment REST endpoint, payOS SDK/network call hoặc credential thật
- [x] V1–V20 migration, mapping/lock/soft-delete integration và performance evidence chạy thật với Docker; chỉ khi gate này xanh mới đóng Task 3

---

## TUẦN 4 — Payment & StudentPackage (Trọng tâm B)

### Task 4.1: Invoice State Machine & payOS Integration
- [x] Service: `InvoiceService`
  - `createInvoice(CreateInvoiceRequest)` tách thành 3 ranh giới: transaction lưu Invoice `PENDING` → gọi payOS ngoài transaction → transaction ngắn lưu checkoutUrl/qrCode/orderCode
  - Nhận `Idempotency-Key`; retry cùng key phải dùng lại Invoice, không tạo Invoice mới
  - Nếu Invoice chưa có link, trước khi gọi create lần nữa phải lookup theo `orderCode` và reconcile link hiện hữu khi payOS hỗ trợ
  - Nếu lookup không khả dụng hoặc kết quả không xác định: không tự tạo link thứ hai; trả lỗi retryable, log reconciliation và để Admin xử lý hoặc expiry job đóng Invoice
  - Đây là rủi ro provider đã biết của MVP; tuyệt đối không giữ DB transaction trong lúc chờ HTTP
  - Sinh `invoiceNumber` unique: `INV-{yyyyMMdd}-{sequence}`
  - Sinh `payosOrderCode` unique (int64)
- [x] payOS Integration:
  - Port: `PaymentGateway` (interface)
  - Adapter: `PayOsPaymentGateway` (gọi payOS API tạo payment link)
  - `@ConfigurationProperties` cho payOS client ID, API key, checksum key
  - Connect/read timeout, retry policy
- [x] Controller: `StudentInvoiceController`
  - `POST /api/student/invoices` → 201
  - `GET /api/student/invoices/{id}` → InvoiceDetail
- [x] DTO: `CreateInvoiceRequest`, `InvoiceDetail`

### Task 4.2: Webhook Handler
- [x] Controller: `PaymentWebhookController`
  - `POST /api/webhooks/payos` — không JWT, verify signature
- [x] Service: `PaymentWebhookService`
  - Verify webhook signature theo tài liệu payOS
  - Check orderCode, amount khớp Invoice
  - Idempotency: nếu đã xử lý → trả 200 không effect
  - Tạo `PaymentTransaction`
  - Là orchestration transaction duy nhất: tạo PaymentTransaction → kích hoạt StudentPackage → gọi finance funding
  - Tạo `Wallet/LedgerEntry` pending đúng một lần bằng idempotency key dẫn xuất từ Invoice/PaymentTransaction
  - Commission snapshot từ settings row đã seed; default chuẩn là `5.00`

### Task 4.3: StudentPackage Activation
- [x] Service: `StudentPackageService`
  - `activateFromPayment(invoice)` — chạy đúng 1 lần
  - Set `remainingSessions = totalSessions`, `startsAt`, `expiresAt`
  - Chỉ tạo/kích hoạt package và snapshot commission; không tạo Wallet/Ledger
- [x] Finance funding service:
  - Credit pending balance và tạo LedgerEntry trong transaction do webhook orchestration mở
  - Idempotent theo Invoice/PaymentTransaction để webhook lặp không double funding
- [x] Controller: `StudentPackageController`
  - `GET /api/student/packages` — list phân trang
  - `GET /api/student/packages/{id}` — chi tiết

### Task 4.4: Invoice Polling & Expiry
- [x] Student có thể GET invoice để check status
- [x] Scheduler: `InvoiceExpiryJob` — mark PENDING invoices as EXPIRED khi quá `paymentExpiredAt`

### ✅ Checkpoint Tuần 4
- [x] Invoice `PAID` → StudentPackage `ACTIVE` → Wallet/Ledger cân bằng
- [x] Webhook idempotent
- [x] Invoice hết hạn tự chuyển EXPIRED

---

## TUẦN 5 — Booking (Trọng tâm B)

### Task 5.1: Booking CRUD
- [x] Entity đã có từ migration. Hoàn thiện domain class:
  - `booking/domain/Booking.java`
  - `booking/domain/BookingStatus.java` — SCHEDULED, COMPLETED, CANCELLED, EXPIRED
  - State transition methods: `complete()`, `cancel()`, `expire()`
- [x] Repository: `BookingRepository`
  - `findByIdForUpdate` (PESSIMISTIC_WRITE)
  - Query overlap (PostgreSQL exclusion sẽ bắt ở DB level)
- [x] Service: `BookingService`
  - **Create Booking**: Lock order theo CODING_CONVENTION 3.7:
    1. Load TeacherProfile
    2. Load Student/User
    3. Load StudentPackage FOR UPDATE
    4. Check package ACTIVE, remaining > 0, booking.endTime <= package.expiresAt
    5. Check overlap (exclusion constraint bắt)
    6. Kiểm tra availability; nếu nằm ngoài lịch rảnh thì set `outsideAvailabilityWarning=true` nhưng vẫn cho tạo
    7. Decrement remaining, increment reserved
    8. Insert Booking
  - **Complete Booking**: (Teacher only)
    1. Load Booking FOR UPDATE
    2. Check status = SCHEDULED, endTime <= now
    3. Create SessionReport (trong cùng transaction)
    4. Update StudentPackage: reserved--, completed++
    5. Settlement: Wallet pending → available (Ledger entry)
    6. Set `settlementProcessed = true` (idempotent)
  - **Cancel Booking**: (Teacher only, initiatedBy = TEACHER | STUDENT_REQUEST)
    1. Load Booking FOR UPDATE
    2. Check status = SCHEDULED
    3. Update StudentPackage: reserved--, remaining++
    4. Set `status = CANCELLED`, cancel reason, cancelledAt; giữ `is_deleted = false` để bảo toàn lịch sử, completion rate và audit
  - Mỗi cặp counter phải được đổi bằng một câu SQL update hoặc một entity mutation và đúng một lần flush; không flush trạng thái trung gian vi phạm CHECK tổng counter
- [x] Controller:
  - `TeacherBookingController`:
    - `POST /api/teacher/bookings` → 201
    - `POST /api/teacher/bookings/{id}/complete`
    - `POST /api/teacher/bookings/{id}/cancel`
  - `StudentBookingController`:
    - `GET /api/student/bookings` — list phân trang, filter status/from/to

### Task 5.2: TrialRequest
- [x] Entity: `booking/domain/TrialRequest.java`
- [x] Repository: `TrialRequestRepository`
- [x] Service: `TrialRequestService`
  - Student tạo trial request (check: chưa có PENDING request + chưa có trial SCHEDULED/COMPLETED cho cặp)
  - Teacher accept → tạo Booking `is_trial=true`, `student_package_id=null` trong cùng transaction
  - Teacher reject
- [x] Controller:
  - `POST /api/student/trials/requests` → 201
  - `GET /api/teacher/trial-requests`
  - `POST /api/teacher/trial-requests/{id}/accept`
  - `POST /api/teacher/trial-requests/{id}/reject`

### Task 5.3: SessionReport
- [x] Entity: `booking/domain/SessionReport.java`
- [x] Tạo cùng lúc Complete Booking (đã có trong Task 5.1)

### Task 5.4: Scheduler — Auto-expire & Reminder
- [x] `scheduler/BookingExpiryJob` — SCHEDULED bookings quá endTime → EXPIRED
  - Hoàn trả StudentPackage: reserved--, remaining++
- [x] `scheduler/PackageExpiryJob` — ACTIVE packages quá expiresAt → LOCKED_EXPIRED
  - **Không hủy Booking SCHEDULED đã tạo** (ERD bất biến 6)
  - **Giới hạn MVP:** nếu Student không refund hoặc gia hạn thì không auto-sweep; lượt chưa dùng và tiền tương ứng tiếp tục nằm ở pending, chỉ xử lý qua refund/gia hạn hoặc vận hành Admin
- [x] `scheduler/BookingReminderJob` — gửi notification trước buổi học X giờ
  - Đọc setting `bookingReminderHours` từ platform settings

### Task 5.5: Settlement sau hoàn thành buổi học
- [x] Trong `BookingService.completeBooking()`:
  - Tạo `LedgerEntry` chuyển pending → available
  - Idempotent: check `settlementProcessed`
  - Lock Wallet FOR UPDATE
  - Phân bổ gross theo công thức tích lũy: `resolvedBefore = completedSessions + refundedSessions`; amount của một buổi là `floor((resolvedBefore + 1) × purchasePrice / totalSessions) - floor(resolvedBefore × purchasePrice / totalSessions)`
  - Dùng integer arithmetic, không dùng `double`

### ✅ Checkpoint Tuần 5
- [x] Không double-booking (exclusion constraint)
- [x] Không trừ buổi hai lần
- [x] Cancel/expire hoàn lượt chính xác
- [x] Settlement pending → available sau complete

---

## TUẦN 6 — Booking/StudentPackage Authorization Facade

### Task 6.1: Hoàn thiện facade B cung cấp cho Learning & Communication
- [x] Mở rộng `enrollment.facade.EnrollmentFacade` hiện có để kiểm tra Student–Teacher có StudentPackage hợp lệ
- [x] Mở rộng `booking.facade.BookingEligibilityFacade` hiện có để kiểm tra Booking/Trial hợp lệ cho cặp
- [x] Không tạo facade trùng trong package `service`; không sửa implementation nội bộ `learning` hoặc `communication`

### Task 6.2: Student Session Reports
- [x] Controller: `GET /api/student/session-reports` — phân trang
- [x] DTO: `SessionReportView`

### Task 6.3: Loại bỏ facade stub tạm thời
- [x] Khi domain/repository B tương ứng đã hoàn thiện, thay implementation `JdbcTemplate` stub bằng application service/repository của đúng module
- [x] Giữ nguyên public facade interface/DTO để không làm vỡ consumer của A
- [x] Gỡ từng ArchUnit ignore pattern ngay khi stub tương ứng được thay thế; không để exception tạm thời thành kiến trúc lâu dài
- [x] Integration test facade theo ownership, soft-delete và trạng thái hợp lệ

### ✅ Checkpoint Tuần 6
- [x] Module A có thể dùng facade để kiểm tra quyền chat/learning
- [x] Không còn facade B dùng `JdbcTemplate` trực tiếp nếu repository/domain tương ứng đã sẵn sàng

---

## TUẦN 7 — Finance, Admin Dashboard & Scheduled Jobs

### Task 7.1: BankAccount CRUD
- [x] Entity: `finance/domain/TeacherBankAccount.java`
- [x] Repository: `TeacherBankAccountRepository`
- [x] Service: `BankAccountService`
  - CRUD, tối đa 1 default per teacher (partial unique index)
  - Mã hóa `accountNumber` trước khi lưu
  - DTO chỉ trả số đã mask
- [x] Controller: `TeacherBankAccountController`
  - `GET /api/teacher/bank-accounts`
  - `POST /api/teacher/bank-accounts` → 201
  - `PUT /api/teacher/bank-accounts/{id}`
  - `DELETE /api/teacher/bank-accounts/{id}` → 204

### Task 7.2: Payout Request
- [x] Entity: `finance/domain/PayoutRequest.java`
  - Status: PENDING → PROCESSING → SUCCEEDED/REJECTED/FAILED
- [x] Service: `PayoutService`
  - Teacher tạo payout: check available balance, reserve balance, create ledger entry
  - Admin process → PROCESSING
  - Admin complete → SUCCEEDED (upload chứng từ, bankReference)
  - Admin reject → REJECTED, release reserve
  - **Lock thứ tự**: Wallet → idempotency → LedgerEntry → balance
- [x] Controller (Teacher): `TeacherPayoutController`
  - `POST /api/teacher/payout-requests` → 201
  - `GET /api/teacher/payout-requests`
- [x] Controller (Admin): `AdminPayoutController`
  - `GET /api/admin/payout-requests`
  - `POST /api/admin/payout-requests/{id}/process`
  - `POST /api/admin/payout-requests/{id}/complete` (multipart: proof)
  - `POST /api/admin/payout-requests/{id}/reject`

### Task 7.3: Refund Request
- [x] Entity: `finance/domain/RefundRequest.java`
  - Status: PENDING → APPROVED → PROCESSING → REFUNDED/REJECTED/FAILED
- [x] Service: `RefundService`
  - Student tạo: lock package, check `ACTIVE | LOCKED_EXPIRED`, remaining > 0, không có SCHEDULED booking và không có refund đang xử lý
  - Ngay khi tạo request, chuyển package sang `REFUND_PENDING` để chặn Booking mới
  - Admin không được approve quá remaining sessions
  - Tính tích lũy với `resolvedBefore = completedSessions + refundedSessions`: refund `n` buổi bằng `floor((resolvedBefore + n) × purchasePrice / totalSessions) - floor(resolvedBefore × purchasePrice / totalSessions)`
  - Công thức trên là cách tổng quát hóa SPEC 6.10: vẫn thực hiện đúng công thức floor theo số buổi và bảo đảm khi mọi buổi đã resolved thì tổng gross + refund bằng chính xác `purchase_price_vnd`
  - Refund cuối là lần làm `remainingSessions` về 0 trong khi `reservedSessions=0`; phần dư được dồn tự động bởi công thức tích lũy
  - Admin complete: upload chứng từ, chuyển approved sessions từ remaining sang refunded, debit Wallet; full refund chuyển package `REFUNDED`, partial refund phục hồi `ACTIVE` hoặc `LOCKED_EXPIRED` theo `expiresAt`
  - Admin reject: phục hồi `ACTIVE` hoặc `LOCKED_EXPIRED` theo `expiresAt`
- [x] Controller (Student): `StudentRefundController`
  - `POST /api/student/refund-requests` → 201
- [x] Controller (Admin): `AdminRefundController`
  - `GET /api/admin/refund-requests`
  - `POST /api/admin/refund-requests/{id}/approve`
  - `POST /api/admin/refund-requests/{id}/reject`
  - `POST /api/admin/refund-requests/{id}/complete`

### Task 7.4: Extension Request
- [x] Entity: `finance/domain/PackageExtensionRequest.java`
  - Status: PENDING → APPROVED/REJECTED
- [x] Service: `ExtensionService`
  - Student tạo: check package LOCKED_EXPIRED, không có extension PENDING
  - Admin approve: bắt buộc `approvedExpiryDate > now`, update expiresAt rồi chuyển package về ACTIVE
  - Admin reject
- [x] Controller (Student): `StudentExtensionController`
  - `POST /api/student/extension-requests` → 201
- [x] Controller (Admin): `AdminExtensionController`
  - `GET /api/admin/extension-requests`
  - `POST /api/admin/extension-requests/{id}/approve`
  - `POST /api/admin/extension-requests/{id}/reject`

### Task 7.5: Wallet & Ledger API
- [x] Controller: `TeacherWalletController`
  - `GET /api/teacher/wallet` — WalletView
  - `GET /api/teacher/wallet/ledger` — phân trang, filter

### Task 7.6: Admin Dashboard & Settings
- [x] `AdminDashboardController`
  - `GET /api/admin/dashboard` — AdminDashboardView (GMV, commission, booking count, etc.)
- [x] `AdminSettingsController`
  - `GET /api/admin/settings` — PlatformSettingsView
  - `PUT /api/admin/settings` — UpdatePlatformSettingsRequest
- [x] Implement settings service/API trên bảng `platform_settings` đã được tạo ở V15, singleton constraint ở V16 và default/seed được harden ở V18
- [x] `AdminAuditLogController`
  - `GET /api/admin/audit-logs` — phân trang, filter

### Task 7.7: Hoàn thiện scheduled jobs
- [x] Review tất cả scheduler: Invoice expiry, Booking expiry, Package expiry, Reminder
- [x] Đảm bảo tất cả jobs idempotent

### ✅ Checkpoint Tuần 7
- [x] Payout/refund không làm âm Wallet
- [x] Dashboard đối soát được GMV, commission, Booking
- [x] Extension reactivate package đúng

---

## TUẦN 8 — Hardening & Testing

### Task 8.1: Integration Test
- [x] Test migration V1–V18 từ database rỗng; test không được skip
- [x] Test schema metadata: teacher FK, settings singleton/seed, booking CHECK, commission `(5,2)` và các CHECK mới
- [x] Test fixture legacy hợp lệ migrate thành công; fixture có enum/status không rõ nghĩa phải fail với thông báo chứa bảng/constraint/số row
- [x] Test booking locking & exclusion constraint (double-booking)
- [x] Test Booking ngoài availability vẫn tạo được và bật warning
- [x] Test webhook idempotency (gửi webhook 2 lần → chỉ 1 effect)
- [x] Test payOS timeout/reconciliation không giữ transaction và không tạo hai Invoice/payment link cục bộ
- [x] Test ledger integrity (balance = sum of ledger entries)
- [x] Test counter invariant (remaining + reserved + completed + refunded = total)
- [x] Test concurrent create/complete/cancel/expire không flush counter ở trạng thái trung gian
- [x] Test payout reserve/release
- [x] Test nhiều thứ tự completion/refund với giá không chia hết; tổng cuối bằng `purchase_price_vnd`
- [x] Test refund chuyển `REFUND_PENDING`, reject/partial phục hồi đúng trạng thái và full refund chuyển `REFUNDED`
- [x] Test extension từ chối `approvedExpiryDate <= now`
- [x] Test soft delete cho từng nhóm mapping: `repository.delete()` phát sinh UPDATE, row còn trong DB, query JPA mặc định không thấy row và versioned entity dùng đúng optimistic-lock condition
- [x] Test native/JdbcTemplate query, aggregation và facade không trả hoặc tính row đã soft-delete, kể cả khi parent/child trong join bị xóa mềm
- [x] Test Booking cancel chỉ đổi trạng thái và giữ `is_deleted = false`; slot được giải phóng bởi điều kiện exclusion nhưng lịch sử/counter vẫn truy xuất đúng
- [x] Test PricingPackage `INACTIVE` không bán mới nhưng StudentPackage đã mua vẫn đọc được snapshot và tiếp tục flow hợp lệ
- [x] Test append-only repository/service không cung cấp đường xóa hoặc sửa `PaymentTransaction`, `LedgerEntry`, `AuditLog`
- [x] Nếu có use case restore được duyệt, test restore idempotent, authorization/audit và xung đột unique/partial-index; nếu chưa có contract thì không tạo endpoint restore

### Task 8.2: Seed Data
- [x] Tạo migration `V23__seed_demo_data.sql` cho profile dev & demo:
  - Admin user
  - 2 Teacher (APPROVED) với subjects, pricing packages, availabilities
  - 3 Student với packages, bookings
  - Sample invoices, payment transactions, wallet entries, bank accounts, payout/refund/extension requests
  - Platform settings mặc định

### Task 8.3: Optimization & Polish
- [x] Tổng hợp và re-check query-count/`EXPLAIN ANALYZE` evidence đã thu từ từng feature; thêm index còn thiếu nếu số liệu chứng minh cần thiết
- [x] Redis cache cho settings, dashboard aggregation
- [x] Docker hóa backend (Dockerfile multi-stage cho Spring Boot & `docker-compose.yml`)
- [x] Chạy acceptance test end-to-end (240 tests xanh)

### ✅ Checkpoint Tuần 8
- [x] Migration V1–V23 chạy clean từ database rỗng, 240/240 tests pass
- [x] Không race condition ở booking/payment/finance
- [x] Seed data chạy được cho demo

---

## TUẦN 9 — Production Readiness, Security Hardening, Observability & CI/CD

### Task 9.1: Security & IDOR Hardening
- [x] Bảo mật phân quyền IDOR chặt chẽ trên toàn bộ Controller:
  - Student không thể truy cập hoặc thao tác trên Invoice, StudentPackage, Booking, Refund, Extension của Student khác
  - Teacher không thể xem hoặc thao tác trên Booking, TrialRequest, BankAccount, Payout, Wallet của Teacher khác
  - Admin endpoints yêu cầu đúng quyền `ROLE_ADMIN`
- [x] Tạo test suite `SecurityIdorIntegrationTest.java` bao phủ toàn bộ ma trận phân quyền

### Task 9.2: Real-time STOMP/WebSocket Event Bridging
- [x] Tạo `TransactionEventWebSocketBridge` lắng nghe Spring Application Events sau khi transaction commit (`@TransactionalEventListener(phase = AFTER_COMMIT)`):
  - `BookingEvent` → gửi `/user/{userId}/queue/notifications`
  - `InvoicePaidEvent` → gửi `/user/{studentId}/queue/transactions`
  - `PayoutProcessedEvent` → gửi `/user/{teacherUserId}/queue/transactions`
- [x] Unit/Integration tests cho WebSocket Bridge

### Task 9.3: Observability, Actuator & Custom Business Metrics
- [x] Bật và cấu hình Actuator endpoints: `/actuator/health`, `/actuator/info`, `/actuator/metrics`, `/actuator/prometheus`
- [x] Tạo `PlatformBusinessMetrics` đăng ký Micrometer custom counters & gauges:
  - `edtech.bookings.created`, `edtech.bookings.completed`
  - `edtech.invoices.paid`
  - `edtech.payouts.processed`
- [x] Integration test kiểm tra Actuator metrics endpoint trả về đúng metrics

### Task 9.4: Performance Benchmarking & Concurrency Stress Test
- [x] Viết test `ConcurrentStressIntegrationTest.java`:
  - 20 threads đồng thời tạo Booking cùng thời điểm (chỉ 1 thành công, 19 reject bởi GiST exclusion)
  - 20 threads gửi cùng Idempotency-Key tạo Invoice (chỉ 1 Invoice tạo thật, 20 threads nhận cùng kết quả an toàn)
- [x] Xác nhận không xảy ra deadlock, race condition, hoặc rò rỉ kết nối DB

### Task 9.5: CI/CD Pipeline Automation (GitHub Actions)
- [x] Tạo workflow `.github/workflows/backend-ci.yml`:
  - Thiết lập Java 21 Temurin và Docker environment
  - Chạy `mvn clean test` với Testcontainers thật
  - Build Docker image multi-stage và xác minh

### ✅ Checkpoint Tuần 9
- [x] Toàn bộ Security/IDOR tests xanh 100%
- [x] WebSocket event notifications hoạt động chuẩn sau commit
- [x] Actuator/Prometheus metrics sẵn sàng cho production monitoring
- [x] CI/CD pipeline tự động hóa hoàn chỉnh
- [x] Full `mvn test` đạt 260/260 tests xanh 100% (0 fail, 0 skip)

---

## Definition of Done bắt buộc cho mọi endpoint/task B

Không đánh dấu `[x]` hoặc ghi `Done` nếu thiếu bất kỳ mục áp dụng nào dưới đây:

- [ ] Endpoint, method, request/response DTO và HTTP status khớp `API_CONTRACT.md`; create dùng `201`, delete không body dùng `204`, không trả `200` với `success=false`
- [ ] REST JSON dùng envelope `success/message/data/errors/meta`; chỉ dùng ngoại lệ đã liệt kê tại Task 1.11
- [ ] Request DTO dùng Bean Validation; validation liên field đặt ở class-level validator hoặc Service
- [ ] Business rule, state transition, role và ownership được enforce trong Service; Controller không chứa business logic hoặc gọi Repository
- [ ] Mọi failure dùng `ErrorCode` có trong `ERROR_CODES.md`; validation field map thành `errors[]`, không lộ raw exception
- [ ] Không trả JPA Entity hoặc field nhạy cảm; response dùng DTO/projection rõ ràng
- [ ] Transaction đặt ở public application service; lock order, idempotency và atomic counter được review theo rủi ro
- [ ] External call có timeout/recovery; chỉ retry operation an toàn/idempotent và không giữ DB lock/transaction trong lúc chờ network
- [ ] List/search có pagination hoặc hard limit, sort allow-list, index phù hợp và không N+1; query rủi ro cao có query-count/`EXPLAIN ANALYZE` evidence
- [ ] Entity mutable có `@SQLDelete` và Hibernate filter theo Task 1.12; native/JdbcTemplate query lọc `is_deleted = false`; entity versioned đã được test đúng SQL parameter/optimistic locking
- [ ] State transition nghiệp vụ không bị thay bằng soft delete; append-only entity không có đường update/delete; restore/delete endpoint chỉ tồn tại khi đã có trong contract và có authorization/audit rõ ràng
- [ ] Unit/integration/security test bao phủ happy path, validation, role, ownership, state conflict và concurrency/idempotency tương xứng
- [ ] Log có request/correlation ID nhưng không chứa password, JWT, refresh token, API key, checksum key, account number đầy đủ hoặc raw payload nhạy cảm
- [ ] Contract/schema/error code/OpenAPI/Markdown và `PROGRESS_BE_B.md` được cập nhật trong cùng PR

### Evidence khi đánh dấu hoàn thành

- Ghi lệnh test, số test run/skip/fail/error; test bị skip không được tính là pass
- Ghi query plan/count đối với endpoint danh sách hoặc aggregation quan trọng
- Ghi migration version/checksum và kết quả Flyway validate nếu task chạm schema
- Ghi rõ exception/technical debt còn lại, owner và task loại bỏ; không dùng cụm “đã verify” nếu chỉ mới compile hoặc audit tĩnh

---

## Quy tắc ghi log (sau mỗi task)

Sau MỖI task được giao, append entry mới vào file `PROGRESS_BE_B.md`:

```
## [YYYY-MM-DD HH:mm] <Tên task ngắn gọn>
- Mục tiêu: <task được giao là gì>
- Căn cứ: <trích dẫn mục nào trong file .md nào>
- File tạo mới/thay đổi/di chuyển: <liệt kê đầy đủ>
- Quyết định kỹ thuật đáng chú ý: <nếu có>
- Trạng thái: Done / Cần review / Blocked
- Việc còn thiếu / cần làm tiếp: <nếu có>
```

---

## Tham chiếu nhanh: File ↔ Module

| Module | Package | Sở hữu |
|--------|---------|--------|
| enrollment | `com.edtech.platform.enrollment` | B |
| payment | `com.edtech.platform.payment` | B |
| booking | `com.edtech.platform.booking` | B |
| finance | `com.edtech.platform.finance` | B |
| admin | `com.edtech.platform.admin` | B |
| scheduler | `com.edtech.platform.scheduler` | B |
| Flyway migrations | `src/main/resources/db/migration/` | B |

## Tham chiếu: Thứ tự Lock (CODING_CONVENTION 3.7)

- **Booking**: TeacherProfile → Student/User → StudentPackage → conflict query → counters → insert Booking
- **Finance**: StudentPackage → Wallet → idempotency check → LedgerEntry → balance
- **KHÔNG đổi thứ tự** ở service khác để tránh deadlock

## Tham chiếu: API Contract endpoints thuộc B

| Endpoint | Method | Module |
|----------|--------|--------|
| `/api/student/invoices` | POST | payment |
| `/api/student/invoices/{id}` | GET | payment |
| `/api/student/packages` | GET | enrollment |
| `/api/student/packages/{id}` | GET | enrollment |
| `/api/student/bookings` | GET | booking |
| `/api/student/trials/requests` | POST | booking |
| `/api/student/refund-requests` | POST | finance |
| `/api/student/extension-requests` | POST | finance |
| `/api/student/session-reports` | GET | booking |
| `/api/webhooks/payos` | POST | payment |
| `/api/teacher/bookings` | POST | booking |
| `/api/teacher/bookings/{id}/complete` | POST | booking |
| `/api/teacher/bookings/{id}/cancel` | POST | booking |
| `/api/teacher/trial-requests` | GET | booking |
| `/api/teacher/trial-requests/{id}/accept` | POST | booking |
| `/api/teacher/trial-requests/{id}/reject` | POST | booking |
| `/api/teacher/wallet` | GET | finance |
| `/api/teacher/wallet/ledger` | GET | finance |
| `/api/teacher/bank-accounts` | ALL | finance |
| `/api/teacher/payout-requests` | POST, GET | finance |
| `/api/admin/teachers/approvals` | ALL | admin |
| `/api/admin/subject-proposals` | ALL | admin |
| `/api/admin/subjects` | POST, PUT | admin |
| `/api/admin/refund-requests` | ALL | admin |
| `/api/admin/extension-requests` | ALL | admin |
| `/api/admin/payout-requests` | ALL | admin |
| `/api/admin/users/{id}/status` | PATCH | admin |
| `/api/admin/dashboard` | GET | admin |
| `/api/admin/audit-logs` | GET | admin |
| `/api/admin/settings` | GET, PUT | admin |
