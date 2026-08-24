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
- [ ] Cài Docker/đưa Docker CLI vào `PATH`; tại lần xác minh 2026-08-24, lệnh `docker` chưa tồn tại
- [ ] Verify `docker compose up` chạy thành công trên máy local
- [ ] Kiểm tra kết nối PostgreSQL (`edtech_db` / `edtech_user` / `edtech_password`)
- [ ] Kiểm tra kết nối Redis (`localhost:6379`)
- [x] Cấu hình `.env.example` với đầy đủ biến môi trường

### Task 1.2: Flyway Baseline Migration
> **B là người duy nhất tạo/sửa Flyway migration** (PLANBE.md quy tắc chống giẫm chân)

Các file V1–V17 tại `backend/src/main/resources/db/migration/` là baseline đã đóng băng theo quy ước nhóm, nhưng **chưa được runtime-verify** trên PostgreSQL từ database rỗng. Audit tĩnh không thay thế Testcontainers.

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
- [x] Tạo context test smoke (`ApiApplicationTests`), nhưng chưa xác nhận chạy thật trên Testcontainers
- [x] Tạo `@TestConfiguration` base cho integration test dùng PostgreSQL container

### Task 1.4: Environment Variables
- [x] Chuyển secrets ra env vars trong `application.yml` (đã có sẵn, verify)
- [x] Tạo file `.env.example` liệt kê tất cả env vars cần thiết
- [x] Kiểm tra `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_URL`, `APP_JWT_SECRET` đều có fallback dev

### ✅ Checkpoint Tuần 1
- [ ] `docker version` và `docker compose version` thành công
- [ ] Migration V1–V17 chạy được trên PostgreSQL Testcontainers từ database rỗng
- [ ] Context test chạy thật, không bị skip
- [ ] `FlywayMigrationTest`: tests run `6`, skipped `0`, failures `0`, errors `0`
- [ ] Flyway báo đúng 17 migration applied và `validate` thành công

> **Trạng thái xác minh 2026-08-24:** `mvn -Dtest=FlywayMigrationTest test` báo `BUILD SUCCESS` nhưng cả 6 test đều bị skip vì không tìm thấy Docker. Không được dùng kết quả này để đánh dấu migration/context test hoàn thành.

---

## TUẦN 1 — Các task thực hiện ngoài kế hoạch gốc

> Các task bên dưới được thực hiện để hỗ trợ remediation của Thành viên A (A-02 → A-09) và đảm bảo
> ArchUnit guard của A có thể hoạt động đúng. Không nằm trong kế hoạch 8 tuần ban đầu.

### Task 1.5: Migration V17 — Fix BaseEntity compatibility
- [x] Tạo `V17__add_deleted_to_refresh_tokens.sql`
  - Thêm cột `deleted BOOLEAN NOT NULL DEFAULT false` vào bảng `refresh_tokens`
  - Lý do: `RefreshToken` kế thừa `BaseEntity` (có field `isDeleted`) nên bảng phải có cột này
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

Dependency B cần từ module A, phải handshake trước Tuần 2:

- [ ] A expose PricingPackage snapshot facade; B dùng để đọc package purchasable khi tạo Invoice/StudentPackage
- [ ] A expose Teacher approval operations; B dùng để list/approve/reject TeacherProfile
- [ ] Chốt DTO/interface bằng public facade; B không gọi repository của `catalog` hoặc `teacher`

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
  - Fetch tất cả teacher `APPROVED` → gọi `TeacherStatsService.recalculateTeacherStats()` (A)
  - Sau đó gọi `TeacherStatsFacade.updateAllGlobalRanks()` (A)
  - Invalidate Redis cache `GLOBAL_RANKING`

### Task 1.9: Hard gate xác minh V1–V17

> **Chặn cứng:** Không chốt nội dung hoặc tạo V18 trước khi toàn bộ checklist này xanh. `BUILD SUCCESS` không đủ nếu Surefire báo test bị skip.

- [ ] Cài Docker và xác nhận `docker version`, `docker compose version` thành công
- [ ] Chạy `mvn -Dtest=FlywayMigrationTest test` trên database PostgreSQL Testcontainers rỗng
- [ ] Xác nhận Surefire: tests run `6`, skipped `0`, failures `0`, errors `0`
- [ ] Xác nhận Flyway có đúng 17 migration applied và `validate` thành công
- [ ] Audit metadata runtime, không chỉ đọc file SQL:
  - Tất cả FK `teacher_id` của V1–V17 tham chiếu `teacher_profiles(id)`
  - V16 có `uq_platform_settings_singleton` và `ck_platform_settings_singleton`
  - `bookings.status` có CHECK đủ `SCHEDULED/COMPLETED/CANCELLED/EXPIRED`
  - `student_packages.commission_rate` có precision/scale `(5,2)`
- [ ] Ghi số test run/skip/fail/error và Docker/PostgreSQL version vào `PROGRESS_BE_B.md`

Nếu phát hiện lỗi mới trong V1–V17: không sửa migration cũ; thêm lỗi vào bảng remediation dưới đây, xử lý bằng V18 và bổ sung regression assertion.

| Phát hiện | Query/Test tái hiện | Cách sửa trong V18 | Regression test | Trạng thái |
|---|---|---|---|---|
| Chưa có — chờ hard gate | — | — | — | Blocked bởi Docker |

### Task 1.10: Migration V18 — Harden business invariants

- [ ] Chỉ tạo `V18__harden_business_invariants.sql` sau khi Task 1.9 xanh
- [ ] Commission và settings:
  - Đổi default `platform_settings.commission_rate` thành `5.00`
  - Seed đúng một settings row nếu bảng rỗng, tương thích singleton constraint của V16
  - Backfill `student_packages.commission_rate IS NULL` từ settings rồi đặt column `NOT NULL`
  - Không tự đổi snapshot khác `5.00`; nếu có, fail rõ ràng để xác minh đó là cấu hình hợp lệ hay dữ liệu do default sai
- [ ] Thêm CHECK cho status/type còn thiếu theo enum trong SPEC/API contract; không tạo lại constraint booking đã có
- [ ] Allow-list và default đích phải được khóa như sau:

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

- [ ] Trước mỗi constraint, audit dữ liệu bằng query dạng:
  ```sql
  SELECT <column>, count(*)
  FROM <table>
  WHERE <column> NOT IN (<allow_list>)
  GROUP BY <column>;
  ```
- [ ] Chỉ sửa cơ học các legacy value đã có mapping được duyệt; dữ liệu không rõ nghĩa phải `RAISE EXCEPTION` kèm tên bảng, constraint và số row
- [ ] Không xóa row hoặc ép về một trạng thái tùy ý để migration tiếp tục
- [ ] Test V1–V18 từ database rỗng, fixture legacy hợp lệ và fixture legacy không hợp lệ

---

## TUẦN 2 — Admin Approval & Audit Log

### Task 2.1: Audit Log Module
- [ ] Entity: `AuditLog` (append-only, không soft delete, không `BaseEntity`)
  - File: `admin/domain/AuditLog.java`
- [ ] Repository: `AuditLogRepository`
  - File: `admin/repository/AuditLogRepository.java`
- [ ] Service: `AuditLogService.log(actorId, action, targetType, targetId, before, after, ip, userAgent)`
  - File: `admin/service/AuditLogService.java`
  - Ghi mọi action thay đổi trạng thái
- [ ] DTO: `AuditLogView`
  - File: `admin/dto/response/AuditLogView.java`

### Task 2.2: Admin Teacher Approval
- [ ] Controller: `AdminTeacherApprovalController`
  - `GET /api/admin/teacher-approvals` — list pending approvals (phân trang)
  - `POST /api/admin/teacher-approvals/{id}/approve`
  - `POST /api/admin/teacher-approvals/{id}/reject`
- [ ] DTO:
  - `ApproveTeacherRequest` (chỉ cần body rỗng hoặc optional note)
  - `RejectRequest` (có field `reason`)
  - `TeacherApprovalView`
- [ ] Service: `AdminTeacherApprovalService`
  - Gọi Teacher approval facade do module A sở hữu để list/approve/reject profile
  - Tạo AuditLog cho mỗi action
  - **Chỉ giao tiếp qua public facade/DTO, không gọi repository module A**

### Task 2.3: Admin Subject Proposal
- [ ] Controller: `AdminSubjectProposalController`
  - `GET /api/admin/subject-proposals`
  - `POST /api/admin/subject-proposals/{id}/approve` (resolution: CREATE_NEW | LINK_EXISTING)
  - `POST /api/admin/subject-proposals/{id}/reject`
- [ ] DTO: `ApproveSubjectProposalRequest`, `RejectRequest`, `SubjectProposalView`
- [ ] Service: `AdminSubjectProposalService`
  - Tạo Subject mới hoặc link existing + tạo TeacherSubject
  - AuditLog

### Task 2.4: User Moderation
- [ ] Controller endpoint: `PATCH /api/admin/users/{id}/status`
- [ ] DTO: `ChangeUserStatusRequest` (status + reason)
- [ ] Service: kiểm tra transition hợp lệ, AuditLog

### ✅ Checkpoint Tuần 2
- Teacher đăng ký → gửi hồ sơ → Admin duyệt → Teacher được phép bán gói
- AuditLog ghi đầy đủ

---

## TUẦN 3 — Invoice & StudentPackage Schema Prep

### Task 3.1: Enrollment Module — Domain & Repository
- [ ] Entity: `StudentPackage`
  - File: `enrollment/domain/StudentPackage.java`
  - Enum: `StudentPackageStatus` (PENDING_PAYMENT, ACTIVE, COMPLETED, REFUND_PENDING, REFUNDED, LOCKED_EXPIRED)
  - `@Version` field
  - Counter invariant method: `validateCounterTotal()`
- [ ] Repository: `StudentPackageRepository`
  - `findByIdForUpdate` (PESSIMISTIC_WRITE)
  - `findByStudentIdAndStatus(studentId, status, Pageable)`

### Task 3.2: Payment Module — Domain & Repository
- [ ] Entity: `Invoice`
  - File: `payment/domain/Invoice.java`
  - Enum: `InvoiceStatus` (PENDING, PAID, CANCELLED, EXPIRED)
  - State transition methods: `markPaid()`, `markExpired()`, `markCancelled()`
- [ ] Entity: `PaymentTransaction` (append-only, không soft delete)
  - File: `payment/domain/PaymentTransaction.java`
- [ ] Repository: `InvoiceRepository`, `PaymentTransactionRepository`

### Task 3.3: Finance Module — Domain Skeleton
- [ ] Entity: `Wallet`
  - File: `finance/domain/Wallet.java`
  - Balance check methods
  - `@Version` field
- [ ] Entity: `LedgerEntry` (append-only)
  - File: `finance/domain/LedgerEntry.java`
- [ ] Repository: `WalletRepository`, `LedgerEntryRepository`

### Task 3.4: Tích hợp PricingPackage facade của module A
- [ ] B consume PricingPackage snapshot facade do A expose; không tạo facade ngược chiều trong `enrollment`
- [ ] Snapshot tối thiểu phục vụ Invoice/StudentPackage: package ID, teacher profile ID, subject ID, name, total sessions, duration, price và trạng thái bán

### ✅ Checkpoint Tuần 3
- Domain classes compile thành công
- Entity relationships đúng ERD
- A có thể dùng catalog facade

---

## TUẦN 4 — Payment & StudentPackage (Trọng tâm B)

### Task 4.1: Invoice State Machine & payOS Integration
- [ ] Service: `InvoiceService`
  - `createInvoice(CreateInvoiceRequest)` tách thành 3 ranh giới: transaction lưu Invoice `PENDING` → gọi payOS ngoài transaction → transaction ngắn lưu checkoutUrl/qrCode/orderCode
  - Nhận `Idempotency-Key`; retry cùng key phải dùng lại Invoice, không tạo Invoice mới
  - Nếu Invoice chưa có link, trước khi gọi create lần nữa phải lookup theo `orderCode` và reconcile link hiện hữu khi payOS hỗ trợ
  - Nếu lookup không khả dụng hoặc kết quả không xác định: không tự tạo link thứ hai; trả lỗi retryable, log reconciliation và để Admin xử lý hoặc expiry job đóng Invoice
  - Đây là rủi ro provider đã biết của MVP; tuyệt đối không giữ DB transaction trong lúc chờ HTTP
  - Sinh `invoiceNumber` unique: `INV-{yyyyMMdd}-{sequence}`
  - Sinh `payosOrderCode` unique (int64)
- [ ] payOS Integration:
  - Port: `PaymentGateway` (interface)
  - Adapter: `PayOsPaymentGateway` (gọi payOS API tạo payment link)
  - `@ConfigurationProperties` cho payOS client ID, API key, checksum key
  - Connect/read timeout, retry policy
- [ ] Controller: `StudentInvoiceController`
  - `POST /api/student/invoices` → 201
  - `GET /api/student/invoices/{id}` → InvoiceDetail
- [ ] DTO: `CreateInvoiceRequest`, `InvoiceDetail`

### Task 4.2: Webhook Handler
- [ ] Controller: `PaymentWebhookController`
  - `POST /api/webhooks/payos` — không JWT, verify signature
- [ ] Service: `PaymentWebhookService`
  - Verify webhook signature theo tài liệu payOS
  - Check orderCode, amount khớp Invoice
  - Idempotency: nếu đã xử lý → trả 200 không effect
  - Tạo `PaymentTransaction`
  - Là orchestration transaction duy nhất: tạo PaymentTransaction → kích hoạt StudentPackage → gọi finance funding
  - Tạo `Wallet/LedgerEntry` pending đúng một lần bằng idempotency key dẫn xuất từ Invoice/PaymentTransaction
  - Commission snapshot từ settings row đã seed; default chuẩn là `5.00`

### Task 4.3: StudentPackage Activation
- [ ] Service: `StudentPackageService`
  - `activateFromPayment(invoice)` — chạy đúng 1 lần
  - Set `remainingSessions = totalSessions`, `startsAt`, `expiresAt`
  - Chỉ tạo/kích hoạt package và snapshot commission; không tạo Wallet/Ledger
- [ ] Finance funding service:
  - Credit pending balance và tạo LedgerEntry trong transaction do webhook orchestration mở
  - Idempotent theo Invoice/PaymentTransaction để webhook lặp không double funding
- [ ] Controller: `StudentPackageController`
  - `GET /api/student/packages` — list phân trang
  - `GET /api/student/packages/{id}` — chi tiết

### Task 4.4: Invoice Polling & Expiry
- [ ] Student có thể GET invoice để check status
- [ ] Scheduler: `InvoiceExpiryJob` — mark PENDING invoices as EXPIRED khi quá `paymentExpiredAt`

### ✅ Checkpoint Tuần 4
- Invoice `PAID` → StudentPackage `ACTIVE` → Wallet/Ledger cân bằng
- Webhook idempotent
- Invoice hết hạn tự chuyển EXPIRED

---

## TUẦN 5 — Booking (Trọng tâm B)

### Task 5.1: Booking CRUD
- [ ] Entity đã có từ migration. Hoàn thiện domain class:
  - `booking/domain/Booking.java`
  - `booking/domain/BookingStatus.java` — SCHEDULED, COMPLETED, CANCELLED, EXPIRED
  - State transition methods: `complete()`, `cancel()`, `expire()`
- [ ] Repository: `BookingRepository`
  - `findByIdForUpdate` (PESSIMISTIC_WRITE)
  - Query overlap (PostgreSQL exclusion sẽ bắt ở DB level)
- [ ] Service: `BookingService`
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
    4. Set cancel reason, cancelledAt
  - Mỗi cặp counter phải được đổi bằng một câu SQL update hoặc một entity mutation và đúng một lần flush; không flush trạng thái trung gian vi phạm CHECK tổng counter
- [ ] Controller:
  - `TeacherBookingController`:
    - `POST /api/teacher/bookings` → 201
    - `POST /api/teacher/bookings/{id}/complete`
    - `POST /api/teacher/bookings/{id}/cancel`
  - `StudentBookingController`:
    - `GET /api/student/bookings` — list phân trang, filter status/from/to

### Task 5.2: TrialRequest
- [ ] Entity: `booking/domain/TrialRequest.java`
- [ ] Repository: `TrialRequestRepository`
- [ ] Service: `TrialRequestService`
  - Student tạo trial request (check: chưa có PENDING request + chưa có trial SCHEDULED/COMPLETED cho cặp)
  - Teacher accept → tạo Booking `is_trial=true`, `student_package_id=null` trong cùng transaction
  - Teacher reject
- [ ] Controller:
  - `POST /api/student/trials/requests` → 201
  - `GET /api/teacher/trial-requests`
  - `POST /api/teacher/trial-requests/{id}/accept`
  - `POST /api/teacher/trial-requests/{id}/reject`

### Task 5.3: SessionReport
- [ ] Entity: `booking/domain/SessionReport.java`
- [ ] Tạo cùng lúc Complete Booking (đã có trong Task 5.1)

### Task 5.4: Scheduler — Auto-expire & Reminder
- [ ] `scheduler/BookingExpiryJob` — SCHEDULED bookings quá endTime → EXPIRED
  - Hoàn trả StudentPackage: reserved--, remaining++
- [ ] `scheduler/PackageExpiryJob` — ACTIVE packages quá expiresAt → LOCKED_EXPIRED
  - **Không hủy Booking SCHEDULED đã tạo** (ERD bất biến 6)
  - **Giới hạn MVP:** nếu Student không refund hoặc gia hạn thì không auto-sweep; lượt chưa dùng và tiền tương ứng tiếp tục nằm ở pending, chỉ xử lý qua refund/gia hạn hoặc vận hành Admin
- [ ] `scheduler/BookingReminderJob` — gửi notification trước buổi học X giờ
  - Đọc setting `bookingReminderHours` từ platform settings

### Task 5.5: Settlement sau hoàn thành buổi học
- [ ] Trong `BookingService.completeBooking()`:
  - Tạo `LedgerEntry` chuyển pending → available
  - Idempotent: check `settlementProcessed`
  - Lock Wallet FOR UPDATE
  - Phân bổ gross theo công thức tích lũy: `resolvedBefore = completedSessions + refundedSessions`; amount của một buổi là `floor((resolvedBefore + 1) × purchasePrice / totalSessions) - floor(resolvedBefore × purchasePrice / totalSessions)`
  - Dùng integer arithmetic, không dùng `double`

### ✅ Checkpoint Tuần 5
- Không double-booking (exclusion constraint)
- Không trừ buổi hai lần
- Cancel/expire hoàn lượt chính xác
- Settlement pending → available sau complete

---

## TUẦN 6 — Booking/StudentPackage Authorization Facade

### Task 6.1: Hoàn thiện facade B cung cấp cho Learning & Communication
- [ ] Mở rộng `enrollment.facade.EnrollmentFacade` hiện có để kiểm tra Student–Teacher có StudentPackage hợp lệ
- [ ] Mở rộng `booking.facade.BookingEligibilityFacade` hiện có để kiểm tra Booking/Trial hợp lệ cho cặp
- [ ] Không tạo facade trùng trong package `service`; không sửa implementation nội bộ `learning` hoặc `communication`

### Task 6.2: Student Session Reports
- [ ] Controller: `GET /api/student/session-reports` — phân trang
- [ ] DTO: `SessionReportView`

### ✅ Checkpoint Tuần 6
- Module A có thể dùng facade để kiểm tra quyền chat/learning

---

## TUẦN 7 — Finance, Admin Dashboard & Scheduled Jobs

### Task 7.1: BankAccount CRUD
- [ ] Entity: `finance/domain/TeacherBankAccount.java`
- [ ] Repository: `TeacherBankAccountRepository`
- [ ] Service: `BankAccountService`
  - CRUD, tối đa 1 default per teacher (partial unique index)
  - Mã hóa `accountNumber` trước khi lưu
  - DTO chỉ trả số đã mask
- [ ] Controller: `TeacherBankAccountController`
  - `GET /api/teacher/bank-accounts`
  - `POST /api/teacher/bank-accounts` → 201
  - `PUT /api/teacher/bank-accounts/{id}`
  - `DELETE /api/teacher/bank-accounts/{id}` → 204

### Task 7.2: Payout Request
- [ ] Entity: `finance/domain/PayoutRequest.java`
  - Status: PENDING → PROCESSING → SUCCEEDED/REJECTED/FAILED
- [ ] Service: `PayoutService`
  - Teacher tạo payout: check available balance, reserve balance, create ledger entry
  - Admin process → PROCESSING
  - Admin complete → SUCCEEDED (upload chứng từ, bankReference)
  - Admin reject → REJECTED, release reserve
  - **Lock thứ tự**: Wallet → idempotency → LedgerEntry → balance
- [ ] Controller (Teacher): `TeacherPayoutController`
  - `POST /api/teacher/payout-requests` → 201
  - `GET /api/teacher/payout-requests`
- [ ] Controller (Admin): `AdminPayoutController`
  - `GET /api/admin/payout-requests`
  - `POST /api/admin/payout-requests/{id}/process`
  - `POST /api/admin/payout-requests/{id}/complete` (multipart: proof)
  - `POST /api/admin/payout-requests/{id}/reject`

### Task 7.3: Refund Request
- [ ] Entity: `finance/domain/RefundRequest.java`
  - Status: PENDING → APPROVED → PROCESSING → REFUNDED/REJECTED/FAILED
- [ ] Service: `RefundService`
  - Student tạo: lock package, check `ACTIVE | LOCKED_EXPIRED`, remaining > 0, không có SCHEDULED booking và không có refund đang xử lý
  - Ngay khi tạo request, chuyển package sang `REFUND_PENDING` để chặn Booking mới
  - Admin không được approve quá remaining sessions
  - Tính tích lũy với `resolvedBefore = completedSessions + refundedSessions`: refund `n` buổi bằng `floor((resolvedBefore + n) × purchasePrice / totalSessions) - floor(resolvedBefore × purchasePrice / totalSessions)`
  - Công thức trên là cách tổng quát hóa SPEC 6.10: vẫn thực hiện đúng công thức floor theo số buổi và bảo đảm khi mọi buổi đã resolved thì tổng gross + refund bằng chính xác `purchase_price_vnd`
  - Refund cuối là lần làm `remainingSessions` về 0 trong khi `reservedSessions=0`; phần dư được dồn tự động bởi công thức tích lũy
  - Admin complete: upload chứng từ, chuyển approved sessions từ remaining sang refunded, debit Wallet; full refund chuyển package `REFUNDED`, partial refund phục hồi `ACTIVE` hoặc `LOCKED_EXPIRED` theo `expiresAt`
  - Admin reject: phục hồi `ACTIVE` hoặc `LOCKED_EXPIRED` theo `expiresAt`
- [ ] Controller (Student): `StudentRefundController`
  - `POST /api/student/refund-requests` → 201
- [ ] Controller (Admin): `AdminRefundController`
  - `GET /api/admin/refund-requests`
  - `POST /api/admin/refund-requests/{id}/approve`
  - `POST /api/admin/refund-requests/{id}/reject`
  - `POST /api/admin/refund-requests/{id}/complete`

### Task 7.4: Extension Request
- [ ] Entity: `finance/domain/PackageExtensionRequest.java`
  - Status: PENDING → APPROVED/REJECTED
- [ ] Service: `ExtensionService`
  - Student tạo: check package LOCKED_EXPIRED, không có extension PENDING
  - Admin approve: bắt buộc `approvedExpiryDate > now`, update expiresAt rồi chuyển package về ACTIVE
  - Admin reject
- [ ] Controller (Student): `StudentExtensionController`
  - `POST /api/student/extension-requests` → 201
- [ ] Controller (Admin): `AdminExtensionController`
  - `GET /api/admin/extension-requests`
  - `POST /api/admin/extension-requests/{id}/approve`
  - `POST /api/admin/extension-requests/{id}/reject`

### Task 7.5: Wallet & Ledger API
- [ ] Controller: `TeacherWalletController`
  - `GET /api/teacher/wallet` — WalletView
  - `GET /api/teacher/wallet/ledger` — phân trang, filter

### Task 7.6: Admin Dashboard & Settings
- [ ] `AdminDashboardController`
  - `GET /api/admin/dashboard` — AdminDashboardView (GMV, commission, booking count, etc.)
- [ ] `AdminSettingsController`
  - `GET /api/admin/settings` — PlatformSettingsView
  - `PUT /api/admin/settings` — UpdatePlatformSettingsRequest
- [ ] Implement settings service/API trên bảng `platform_settings` đã được tạo ở V15, singleton constraint ở V16 và default/seed được harden ở V18
- [ ] `AdminAuditLogController`
  - `GET /api/admin/audit-logs` — phân trang, filter

### Task 7.7: Hoàn thiện scheduled jobs
- [ ] Review tất cả scheduler: Invoice expiry, Booking expiry, Package expiry, Reminder
- [ ] Đảm bảo tất cả jobs idempotent

### ✅ Checkpoint Tuần 7
- Payout/refund không làm âm Wallet
- Dashboard đối soát được GMV, commission, Booking
- Extension reactivate package đúng

---

## TUẦN 8 — Hardening & Testing

### Task 8.1: Integration Test
- [ ] Test migration V1–V18 từ database rỗng; test không được skip
- [ ] Test schema metadata: teacher FK, settings singleton/seed, booking CHECK, commission `(5,2)` và các CHECK mới
- [ ] Test fixture legacy hợp lệ migrate thành công; fixture có enum/status không rõ nghĩa phải fail với thông báo chứa bảng/constraint/số row
- [ ] Test booking locking & exclusion constraint (double-booking)
- [ ] Test Booking ngoài availability vẫn tạo được và bật warning
- [ ] Test webhook idempotency (gửi webhook 2 lần → chỉ 1 effect)
- [ ] Test payOS timeout/reconciliation không giữ transaction và không tạo hai Invoice/payment link cục bộ
- [ ] Test ledger integrity (balance = sum of ledger entries)
- [ ] Test counter invariant (remaining + reserved + completed + refunded = total)
- [ ] Test concurrent create/complete/cancel/expire không flush counter ở trạng thái trung gian
- [ ] Test payout reserve/release
- [ ] Test nhiều thứ tự completion/refund với giá không chia hết; tổng cuối bằng `purchase_price_vnd`
- [ ] Test refund chuyển `REFUND_PENDING`, reject/partial phục hồi đúng trạng thái và full refund chuyển `REFUNDED`
- [ ] Test extension từ chối `approvedExpiryDate <= now`

### Task 8.2: Seed Data
- [ ] Tạo migration `V19__seed_demo_data.sql` hoặc `data.sql` cho profile dev:
  - Admin user
  - 2-3 Teacher (APPROVED) với subjects, packages
  - 3-5 Student với packages, bookings
  - Sample invoices, wallet entries
  - Platform settings mặc định

> ⚠️ **Lưu ý version:** V16 (`fix_schema_bugs`) và V17 (`add_deleted_to_refresh_tokens`) là baseline đóng băng; V18 dành cho hardening business invariants. Migration seed data phải dùng từ V19 trở đi.

### Task 8.3: Optimization & Polish
- [ ] Review query performance, thêm index nếu cần
- [ ] Redis cache cho settings, dashboard aggregation
- [ ] Docker hóa backend (Dockerfile cho Spring Boot)
- [ ] Chạy acceptance test end-to-end cùng với A

### ✅ Checkpoint Tuần 8
- Migration V1–V19 chạy clean từ database rỗng, không có integration test bị skip
- Không race condition ở booking/payment/finance
- Seed data chạy được cho demo

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
| `/api/admin/teacher-approvals` | ALL | admin |
| `/api/admin/subject-proposals` | ALL | admin |
| `/api/admin/subjects` | POST, PUT | admin |
| `/api/admin/refund-requests` | ALL | admin |
| `/api/admin/extension-requests` | ALL | admin |
| `/api/admin/payout-requests` | ALL | admin |
| `/api/admin/users/{id}/status` | PATCH | admin |
| `/api/admin/dashboard` | GET | admin |
| `/api/admin/audit-logs` | GET | admin |
| `/api/admin/settings` | GET, PUT | admin |
