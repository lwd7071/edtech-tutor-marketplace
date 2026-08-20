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
- [ ] Verify `docker compose up` chạy thành công trên máy local
- [ ] Kiểm tra kết nối PostgreSQL (`edtech_db` / `edtech_user` / `edtech_password`)
- [ ] Kiểm tra kết nối Redis (`localhost:6379`)

### Task 1.2: Flyway Baseline Migration
> **B là người duy nhất tạo/sửa Flyway migration** (PLANBE.md quy tắc chống giẫm chân)

Tạo các file migration tại `backend/src/main/resources/db/migration/`:

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
    teacher_id UUID NOT NULL REFERENCES users(id),
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
    teacher_id UUID NOT NULL REFERENCES users(id),
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
    commission_rate NUMERIC(5,2) NOT NULL DEFAULT 10.00,
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
    teacher_id UUID NOT NULL REFERENCES users(id),
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
    teacher_id UUID NOT NULL REFERENCES users(id),
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
    teacher_id UUID NOT NULL REFERENCES users(id),
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
    teacher_id UUID NOT NULL REFERENCES users(id),
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
    teacher_id UUID NOT NULL REFERENCES users(id),
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

#### `V16__fix_schema_bugs.sql`
- **Mục đích:** Sửa lỗi từ baseline `V1-V15` (Tuân thủ CODING_CONVENTION.md, không sửa file đã apply).
- Sửa `commission_rate` trong `student_packages` thành `NUMERIC(5,2)` để tránh tràn số.
- Thêm `CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED', 'EXPIRED'))` cho `bookings`.
- Áp dụng pattern Singleton cho `platform_settings` với `is_singleton BOOLEAN NOT NULL DEFAULT true` + `UNIQUE` + `CHECK`.

### Task 1.3: Thiết lập Testcontainers
- [ ] Thêm dependency Testcontainers PostgreSQL vào `pom.xml`:
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
- [ ] Tạo `application-test.yml` dùng Testcontainers:
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
- [ ] Tạo context test smoke (`ApiApplicationTests`) xanh chạy trên Testcontainers
- [ ] Tạo `@TestConfiguration` base cho integration test dùng PostgreSQL container

### Task 1.4: Environment Variables
- [ ] Chuyển secrets ra env vars trong `application.yml` (đã có sẵn, verify)
- [ ] Tạo file `.env.example` liệt kê tất cả env vars cần thiết
- [ ] Kiểm tra `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_URL`, `APP_JWT_SECRET` đều có fallback dev

### ✅ Checkpoint Tuần 1
- `docker compose up` thành công
- Migration chạy được trên database rỗng
- Context test xanh

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
  - Gọi facade TeacherProfile (module A) để đổi status
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

### Task 3.4: Facade cho module A
- [ ] Tạo facade interface để A có thể đọc snapshot PricingPackage:
  - `enrollment/service/EnrollmentFacade.java` — expose thông tin cần thiết cho learning/chat authorization

### ✅ Checkpoint Tuần 3
- Domain classes compile thành công
- Entity relationships đúng ERD
- A có thể dùng catalog facade

---

## TUẦN 4 — Payment & StudentPackage (Trọng tâm B)

### Task 4.1: Invoice State Machine & payOS Integration
- [ ] Service: `InvoiceService`
  - `createInvoice(CreateInvoiceRequest)` → tạo Invoice PENDING → gọi payOS → lưu checkoutUrl, qrCode, orderCode
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
  - Kích hoạt `StudentPackage` → ACTIVE
  - Tạo `Wallet/LedgerEntry` pending balance
  - Commission snapshot từ platform settings

### Task 4.3: StudentPackage Activation
- [ ] Service: `StudentPackageService`
  - `activateFromPayment(invoice)` — chạy đúng 1 lần
  - Set `remainingSessions = totalSessions`, `startsAt`, `expiresAt`
  - Tạo Wallet entry: pending balance = price × (1 - commissionRate)
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
    1. Load Teacher/User
    2. Load Student/User
    3. Load StudentPackage FOR UPDATE
    4. Check package ACTIVE, remaining > 0, booking.endTime <= package.expiresAt
    5. Check overlap (exclusion constraint bắt)
    6. Decrement remaining, increment reserved
    7. Insert Booking
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
- [ ] `scheduler/BookingReminderJob` — gửi notification trước buổi học X giờ
  - Đọc setting `bookingReminderHours` từ platform settings

### Task 5.5: Settlement sau hoàn thành buổi học
- [ ] Trong `BookingService.completeBooking()`:
  - Tạo `LedgerEntry` chuyển pending → available
  - Idempotent: check `settlementProcessed`
  - Lock Wallet FOR UPDATE

### ✅ Checkpoint Tuần 5
- Không double-booking (exclusion constraint)
- Không trừ buổi hai lần
- Cancel/expire hoàn lượt chính xác
- Settlement pending → available sau complete

---

## TUẦN 6 — Booking/StudentPackage Authorization Facade

### Task 6.1: Facade cho Learning & Communication
- [ ] `enrollment/service/EnrollmentFacade.java` — kiểm tra Student-Teacher có StudentPackage hợp lệ
- [ ] `booking/service/BookingFacade.java` — kiểm tra có Booking/Trial hợp lệ cho cặp
- [ ] Expose qua public interface, KHÔNG sửa implementation nội bộ `learning` hoặc `communication`

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
  - Student tạo: check package ACTIVE, không có SCHEDULED booking, không có refund đang xử lý
  - Admin approve: tính `refundAmountVnd = floor(approvedSessions × purchasePriceVnd / totalSessions)` (cộng dư vào lần cuối)
  - Admin complete: upload chứng từ, mark REFUNDED, trừ StudentPackage counters, debit Wallet
  - Admin reject
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
  - Admin approve: update expiresAt, chuyển package về ACTIVE
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
- [ ] Tạo table `platform_settings` (migration V15):
  ```sql
  CREATE TABLE platform_settings (
      id UUID PRIMARY KEY,
      commission_rate NUMERIC(5,2) NOT NULL DEFAULT 10.00,
      bayesian_minimum_reviews INT NOT NULL DEFAULT 10,
      booking_reminder_hours INT NOT NULL DEFAULT 11,
      booking_expiration_hours INT NOT NULL DEFAULT 12,
      updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
  );
  ```
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
- [ ] Test migration từ database rỗng (Testcontainers, toàn bộ V1-V15)
- [ ] Test booking locking & exclusion constraint (double-booking)
- [ ] Test webhook idempotency (gửi webhook 2 lần → chỉ 1 effect)
- [ ] Test ledger integrity (balance = sum of ledger entries)
- [ ] Test counter invariant (remaining + reserved + completed + refunded = total)
- [ ] Test payout reserve/release
- [ ] Test refund calculation (dư do làm tròn)

### Task 8.2: Seed Data
- [ ] Tạo migration `V16__seed_demo_data.sql` hoặc `data.sql` cho profile dev:
  - Admin user
  - 2-3 Teacher (APPROVED) với subjects, packages
  - 3-5 Student với packages, bookings
  - Sample invoices, wallet entries
  - Platform settings mặc định

### Task 8.3: Optimization & Polish
- [ ] Review query performance, thêm index nếu cần
- [ ] Redis cache cho settings, dashboard aggregation
- [ ] Docker hóa backend (Dockerfile cho Spring Boot)
- [ ] Chạy acceptance test end-to-end cùng với A

### ✅ Checkpoint Tuần 8
- Migration chạy clean từ database rỗng
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

- **Booking**: Teacher/User → Student/User → StudentPackage → conflict query → counters → insert Booking
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
