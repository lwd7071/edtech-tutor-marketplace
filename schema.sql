-- =====================================================================
-- SCHEMA: Nền tảng Quản lý Gia sư & Lớp học trực tuyến 1-1
-- DB: PostgreSQL (Supabase) | ID: UUID | Time: UTC timestamptz | Tiền: VND bigint
-- =====================================================================

-- ---------- EXTENSIONS ----------
create extension if not exists pgcrypto;   -- gen_random_uuid()
create extension if not exists btree_gist; -- exclusion constraint chống overlap lịch

-- ---------- TRIGGER FUNCTION: auto update updated_at ----------
create or replace function set_updated_at()
returns trigger as $$
begin
  new.updated_at = now();
  return new;
end;
$$ language plpgsql;

-- =====================================================================
-- 1. USERS & AUTH
-- =====================================================================

create table users (
    id                uuid primary key default gen_random_uuid(),
    email             varchar(255) not null,
    password_hash     varchar(255),
    full_name         varchar(150) not null,
    avatar_url        varchar(500),
    phone             varchar(20),
    parent_full_name  varchar(150),
    parent_phone      varchar(20),
    parent_email      varchar(255),
    notify_parent     boolean not null default false,
    role              varchar(20) not null check (role in ('STUDENT','TEACHER','ADMIN')),
    status            varchar(20) not null default 'ACTIVE',
    email_verified    boolean not null default false,
    oauth_provider    varchar(50),
    oauth_subject     varchar(255),
    last_login_at     timestamptz,
    created_at        timestamptz not null default now(),
    updated_at        timestamptz not null default now(),
    is_deleted        boolean not null default false
);
create unique index ux_users_email on users (lower(email)) where is_deleted = false;
create unique index ux_users_oauth on users (oauth_provider, oauth_subject) where oauth_provider is not null and is_deleted = false;
create index ix_users_role_status on users (role, status);
create trigger trg_users_updated before update on users for each row execute function set_updated_at();

create table refresh_tokens (
    id           uuid primary key default gen_random_uuid(),
    user_id      uuid not null references users(id),
    token_hash   varchar(255) not null,
    expires_at   timestamptz not null,
    revoked_at   timestamptz,
    device_info  varchar(255),
    ip_address   inet,
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now(),
    is_deleted   boolean not null default false
);
create unique index ux_refresh_tokens_hash on refresh_tokens (token_hash);
create index ix_refresh_tokens_user on refresh_tokens (user_id);
create trigger trg_refresh_tokens_updated before update on refresh_tokens for each row execute function set_updated_at();

-- =====================================================================
-- 2. TEACHER PROFILE
-- =====================================================================

create table teacher_profiles (
    id                       uuid primary key default gen_random_uuid(),
    user_id                  uuid not null references users(id),
    bio                      text,
    years_of_experience      int,
    languages                varchar(50)[],
    supports_online          boolean not null default true,
    supports_offline         boolean not null default false,
    location_address         text,
    introduction_video_url   varchar(500),
    profile_status           varchar(20) not null default 'PENDING',
    rejection_reason         text,
    verified_badge           boolean not null default false,
    is_visible               boolean not null default false,
    approved_at              timestamptz,
    approved_by              uuid references users(id),
    created_at               timestamptz not null default now(),
    updated_at               timestamptz not null default now(),
    is_deleted               boolean not null default false,
    constraint uq_teacher_profiles_user unique (user_id)
);
create index ix_teacher_profiles_status_visible on teacher_profiles (profile_status, is_visible);
create index ix_teacher_profiles_languages on teacher_profiles using gin (languages);
create trigger trg_teacher_profiles_updated before update on teacher_profiles for each row execute function set_updated_at();

create table teacher_documents (
    id                    uuid primary key default gen_random_uuid(),
    teacher_id            uuid not null references teacher_profiles(id),
    document_type         varchar(50) not null,
    title                 varchar(255),
    cloudinary_public_id  varchar(255),
    secure_url            varchar(500) not null,
    mime_type             varchar(100),
    file_size             bigint,
    verification_status   varchar(20) not null default 'PENDING',
    verified_by           uuid references users(id),
    verified_at           timestamptz,
    created_at            timestamptz not null default now(),
    updated_at            timestamptz not null default now(),
    is_deleted            boolean not null default false
);
create index ix_teacher_documents_teacher on teacher_documents (teacher_id);
create trigger trg_teacher_documents_updated before update on teacher_documents for each row execute function set_updated_at();

create table teacher_availabilities (
    id          uuid primary key default gen_random_uuid(),
    teacher_id  uuid not null references teacher_profiles(id),
    day_of_week varchar(10) not null,
    start_time  time not null,
    end_time    time not null,
    timezone    varchar(50) not null default 'Asia/Ho_Chi_Minh',
    is_active   boolean not null default true,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now(),
    is_deleted  boolean not null default false,
    constraint ck_availability_time check (start_time < end_time)
);
create index ix_availabilities_teacher on teacher_availabilities (teacher_id, day_of_week);
create trigger trg_availabilities_updated before update on teacher_availabilities for each row execute function set_updated_at();

-- =====================================================================
-- 3. SUBJECTS
-- =====================================================================

create table subjects (
    id               uuid primary key default gen_random_uuid(),
    code             varchar(50) not null,
    name             varchar(150) not null,
    slug             varchar(150) not null,
    education_level  varchar(50),
    description      text,
    is_active        boolean not null default true,
    created_source   varchar(20) not null default 'ADMIN',
    created_at       timestamptz not null default now(),
    updated_at       timestamptz not null default now(),
    is_deleted       boolean not null default false,
    constraint uq_subjects_code unique (code),
    constraint uq_subjects_slug unique (slug)
);
create trigger trg_subjects_updated before update on subjects for each row execute function set_updated_at();

create table subject_proposals (
    id                  uuid primary key default gen_random_uuid(),
    teacher_id          uuid not null references teacher_profiles(id),
    proposed_name       varchar(150) not null,
    education_level     varchar(50),
    description         text,
    status              varchar(20) not null default 'PENDING',
    review_note         text,
    reviewed_by         uuid references users(id),
    reviewed_at         timestamptz,
    created_subject_id  uuid references subjects(id),
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now(),
    is_deleted          boolean not null default false
);
create index ix_subject_proposals_teacher on subject_proposals (teacher_id);
create trigger trg_subject_proposals_updated before update on subject_proposals for each row execute function set_updated_at();

create table teacher_subjects (
    id                       uuid primary key default gen_random_uuid(),
    teacher_id               uuid not null references teacher_profiles(id),
    subject_id               uuid not null references subjects(id),
    level_description        text,
    experience_description   text,
    is_active                boolean not null default true,
    created_at               timestamptz not null default now(),
    updated_at               timestamptz not null default now(),
    is_deleted               boolean not null default false,
    constraint uq_teacher_subjects unique (teacher_id, subject_id)
);
create index ix_teacher_subjects_subject on teacher_subjects (subject_id, is_active, teacher_id);
create trigger trg_teacher_subjects_updated before update on teacher_subjects for each row execute function set_updated_at();

-- =====================================================================
-- 4. PRICING & INVOICE & PAYMENT
-- =====================================================================

create table pricing_packages (
    id                        uuid primary key default gen_random_uuid(),
    teacher_id                uuid not null references teacher_profiles(id),
    subject_id                uuid not null references subjects(id),
    name                      varchar(150) not null,
    description               text,
    total_sessions            int not null,
    duration_days             int not null,
    price_vnd                 bigint not null,
    session_duration_minutes  int not null,
    status                    varchar(20) not null default 'ACTIVE',
    version                   bigint not null default 0,
    created_at                timestamptz not null default now(),
    updated_at                timestamptz not null default now(),
    is_deleted                boolean not null default false,
    constraint ck_pricing_packages_positive check (
        total_sessions > 0 and duration_days > 0 and price_vnd > 0 and session_duration_minutes > 0
    )
);
create index ix_pricing_packages_teacher_status on pricing_packages (teacher_id, status);
create index ix_pricing_packages_subject_status_price on pricing_packages (subject_id, status, price_vnd);
create trigger trg_pricing_packages_updated before update on pricing_packages for each row execute function set_updated_at();

create table invoices (
    id                       uuid primary key default gen_random_uuid(),
    invoice_number           varchar(50) not null,
    student_id               uuid not null references users(id),
    teacher_id               uuid not null references teacher_profiles(id),
    pricing_package_id       uuid not null references pricing_packages(id),
    amount_vnd               bigint not null,
    status                   varchar(20) not null default 'PENDING',
    payos_order_code         bigint,
    payos_payment_link_id    varchar(255),
    checkout_url             varchar(500),
    qr_code                  text,
    payment_expired_at       timestamptz,
    paid_at                  timestamptz,
    created_at               timestamptz not null default now(),
    updated_at               timestamptz not null default now(),
    is_deleted               boolean not null default false,
    constraint uq_invoices_number unique (invoice_number)
);
create unique index ux_invoices_payos_order_code on invoices (payos_order_code) where payos_order_code is not null;
create unique index ux_invoices_payos_link_id on invoices (payos_payment_link_id) where payos_payment_link_id is not null;
create index ix_invoices_student on invoices (student_id, created_at desc);
create index ix_invoices_status_expired on invoices (status, payment_expired_at);
create trigger trg_invoices_updated before update on invoices for each row execute function set_updated_at();

-- Bảng tài chính lịch sử: append-only, KHÔNG soft delete, không updated_at
create table payment_transactions (
    id                     uuid primary key default gen_random_uuid(),
    invoice_id             uuid not null references invoices(id),
    provider               varchar(50) not null,
    provider_reference     varchar(255) not null,
    order_code             bigint,
    amount_vnd             bigint not null,
    transaction_datetime   timestamptz not null,
    raw_payload            jsonb,
    signature_valid        boolean not null default false,
    processed_at           timestamptz,
    created_at             timestamptz not null default now(),
    constraint uq_payment_transactions_ref unique (provider_reference)
);
create index ix_payment_transactions_invoice on payment_transactions (invoice_id);

-- =====================================================================
-- 5. STUDENT PACKAGE & BOOKING
-- =====================================================================

create table student_packages (
    id                    uuid primary key default gen_random_uuid(),
    student_id            uuid not null references users(id),
    teacher_id            uuid not null references teacher_profiles(id),
    subject_id            uuid not null references subjects(id),
    pricing_package_id    uuid not null references pricing_packages(id),
    invoice_id            uuid not null references invoices(id),
    package_name_snapshot varchar(150) not null,
    total_sessions        int not null,
    remaining_sessions    int not null default 0,
    reserved_sessions     int not null default 0,
    completed_sessions    int not null default 0,
    refunded_sessions     int not null default 0,
    purchase_price_vnd    bigint not null,
    commission_rate       numeric(5,4),
    starts_at             timestamptz not null,
    expires_at            timestamptz not null,
    status                varchar(20) not null default 'ACTIVE',
    locked_reason         text,
    version               bigint not null default 0,
    created_at            timestamptz not null default now(),
    updated_at            timestamptz not null default now(),
    is_deleted            boolean not null default false,
    constraint uq_student_packages_invoice unique (invoice_id),
    constraint ck_student_packages_counters check (
        remaining_sessions >= 0 and reserved_sessions >= 0
        and completed_sessions >= 0 and refunded_sessions >= 0
        and total_sessions = remaining_sessions + reserved_sessions + completed_sessions + refunded_sessions
    ),
    constraint ck_student_packages_dates check (starts_at < expires_at)
);
create index ix_student_packages_student_status_expires on student_packages (student_id, status, expires_at);
create index ix_student_packages_teacher_status on student_packages (teacher_id, status);
create trigger trg_student_packages_updated before update on student_packages for each row execute function set_updated_at();

create table bookings (
    id                              uuid primary key default gen_random_uuid(),
    teacher_id                      uuid not null references teacher_profiles(id),
    student_id                      uuid not null references users(id),
    student_package_id              uuid references student_packages(id),
    subject_id                      uuid not null references subjects(id),
    start_time                      timestamptz not null,
    end_time                        timestamptz not null,
    delivery_mode                   varchar(20) not null,
    meeting_link                    varchar(500),
    location_address                text,
    status                          varchar(20) not null default 'SCHEDULED',
    is_trial                        boolean not null default false,
    outside_availability_warning    boolean not null default false,
    cancel_reason                   text,
    cancel_initiated_by             varchar(20),
    completed_at                    timestamptz,
    cancelled_at                    timestamptz,
    expired_at                      timestamptz,
    settlement_processed            boolean not null default false,
    version                         bigint not null default 0,
    created_at                      timestamptz not null default now(),
    updated_at                      timestamptz not null default now(),
    is_deleted                      boolean not null default false,
    constraint ck_bookings_time check (start_time < end_time),
    constraint ck_bookings_trial_package check (
        (is_trial = true and student_package_id is null) or
        (is_trial = false and student_package_id is not null)
    )
);
create index ix_bookings_teacher_start on bookings (teacher_id, start_time);
create index ix_bookings_student_start on bookings (student_id, start_time);
create index ix_bookings_status_end on bookings (status, end_time);
create trigger trg_bookings_updated before update on bookings for each row execute function set_updated_at();

-- Chống trùng lịch: chỉ áp dụng với booking đang SCHEDULED, chưa xóa mềm
alter table bookings
  add constraint ex_booking_teacher_overlap
  exclude using gist (
    teacher_id with =,
    tstzrange(start_time, end_time, '[)') with &&
  ) where (status = 'SCHEDULED' and is_deleted = false);

alter table bookings
  add constraint ex_booking_student_overlap
  exclude using gist (
    student_id with =,
    tstzrange(start_time, end_time, '[)') with &&
  ) where (status = 'SCHEDULED' and is_deleted = false);

create table trial_requests (
    id                     uuid primary key default gen_random_uuid(),
    teacher_id             uuid not null references teacher_profiles(id),
    student_id             uuid not null references users(id),
    subject_id             uuid not null references subjects(id),
    preferred_start_time   timestamptz not null,
    note                   text,
    status                 varchar(20) not null default 'PENDING'
        check (status in ('PENDING','ACCEPTED','REJECTED','CANCELLED')),
    booking_id             uuid references bookings(id),
    rejection_reason       text,
    responded_at           timestamptz,
    created_at             timestamptz not null default now(),
    updated_at             timestamptz not null default now(),
    is_deleted             boolean not null default false
);
create unique index ux_trial_requests_booking on trial_requests (booking_id) where booking_id is not null;
-- Tối đa 1 request PENDING cho mỗi cặp Student-Teacher
create unique index ux_trial_requests_pending_pair on trial_requests (teacher_id, student_id) where status = 'PENDING';
create index ix_trial_requests_teacher_status on trial_requests (teacher_id, status, created_at);
create index ix_trial_requests_student_status on trial_requests (student_id, status, created_at);
create trigger trg_trial_requests_updated before update on trial_requests for each row execute function set_updated_at();

create table session_reports (
    id                 uuid primary key default gen_random_uuid(),
    booking_id         uuid not null references bookings(id),
    record_link        varchar(500),
    content            text,
    feedback           text,
    follow_up_note     text,
    teacher_self_rating smallint,
    submitted_at       timestamptz,
    created_at         timestamptz not null default now(),
    updated_at         timestamptz not null default now(),
    is_deleted         boolean not null default false,
    constraint uq_session_reports_booking unique (booking_id)
);
create trigger trg_session_reports_updated before update on session_reports for each row execute function set_updated_at();

create table reviews (
    id            uuid primary key default gen_random_uuid(),
    booking_id    uuid not null references bookings(id),
    student_id    uuid not null references users(id),
    teacher_id    uuid not null references teacher_profiles(id),
    rating        smallint not null,
    comment       text,
    is_visible    boolean not null default true,
    moderated_by  uuid references users(id),
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now(),
    is_deleted    boolean not null default false,
    constraint uq_reviews_booking unique (booking_id),
    constraint ck_reviews_rating check (rating between 1 and 5)
);
create index ix_reviews_teacher_visible on reviews (teacher_id, is_visible, created_at desc);
create trigger trg_reviews_updated before update on reviews for each row execute function set_updated_at();

create table teacher_stats (
    teacher_id                 uuid primary key references teacher_profiles(id),
    average_rating             numeric(3,2) not null default 0,
    bayesian_rating            numeric(3,2) not null default 0,
    review_count               int not null default 0,
    completed_session_count    int not null default 0,
    completion_rate            numeric(5,4) not null default 0,
    trial_session_count        int not null default 0,
    trial_conversion_rate      numeric(5,4) not null default 0,
    global_rank                int,
    calculated_at              timestamptz not null default now()
);

-- =====================================================================
-- 6. WALLET & PAYOUT & REFUND
-- =====================================================================

create table wallets (
    id                      uuid primary key default gen_random_uuid(),
    teacher_id              uuid not null references teacher_profiles(id),
    pending_balance_vnd     bigint not null default 0,
    available_balance_vnd   bigint not null default 0,
    reserved_balance_vnd    bigint not null default 0,
    version                 bigint not null default 0,
    created_at              timestamptz not null default now(),
    updated_at              timestamptz not null default now(),
    is_deleted              boolean not null default false,
    constraint uq_wallets_teacher unique (teacher_id),
    constraint ck_wallets_balances check (
        pending_balance_vnd >= 0 and available_balance_vnd >= 0 and reserved_balance_vnd >= 0
    )
);
create trigger trg_wallets_updated before update on wallets for each row execute function set_updated_at();

-- Sổ cái: append-only, KHÔNG update/delete
create table ledger_entries (
    id               uuid primary key default gen_random_uuid(),
    wallet_id        uuid not null references wallets(id),
    entry_type       varchar(30) not null,
    amount_vnd       bigint not null,
    balance_bucket   varchar(20) not null,
    direction        varchar(10) not null check (direction in ('CREDIT','DEBIT')),
    reference_type   varchar(30) not null,
    reference_id     uuid not null,
    idempotency_key  varchar(255) not null,
    description      text,
    created_at       timestamptz not null default now(),
    constraint uq_ledger_entries_idempotency unique (idempotency_key),
    constraint ck_ledger_entries_amount check (amount_vnd > 0)
);
create index ix_ledger_entries_wallet on ledger_entries (wallet_id, created_at desc);

create table teacher_bank_accounts (
    id                          uuid primary key default gen_random_uuid(),
    teacher_id                  uuid not null references teacher_profiles(id),
    bank_bin                    varchar(20) not null,
    bank_name                   varchar(150) not null,
    account_number_encrypted    text not null,
    account_holder_name         varchar(150) not null,
    is_verified                 boolean not null default false,
    is_default                  boolean not null default false,
    created_at                  timestamptz not null default now(),
    updated_at                  timestamptz not null default now(),
    is_deleted                  boolean not null default false
);
-- Tối đa 1 tài khoản mặc định cho mỗi teacher
create unique index ux_bank_accounts_default on teacher_bank_accounts (teacher_id) where is_default = true and is_deleted = false;
create trigger trg_bank_accounts_updated before update on teacher_bank_accounts for each row execute function set_updated_at();

create table payout_requests (
    id                uuid primary key default gen_random_uuid(),
    teacher_id        uuid not null references teacher_profiles(id),
    wallet_id         uuid not null references wallets(id),
    bank_account_id   uuid not null references teacher_bank_accounts(id),
    amount_vnd        bigint not null,
    status            varchar(20) not null default 'PENDING',
    teacher_note      text,
    admin_note        text,
    bank_reference    varchar(255),
    proof_public_id   varchar(255),
    proof_url         varchar(500),
    transferred_at    timestamptz,
    processed_by      uuid references users(id),
    processed_at      timestamptz,
    version           bigint not null default 0,
    created_at        timestamptz not null default now(),
    updated_at        timestamptz not null default now(),
    is_deleted        boolean not null default false,
    constraint ck_payout_requests_amount check (amount_vnd > 0)
);
create index ix_payout_requests_status on payout_requests (status, created_at);
create trigger trg_payout_requests_updated before update on payout_requests for each row execute function set_updated_at();

create table refund_requests (
    id                          uuid primary key default gen_random_uuid(),
    student_package_id         uuid not null references student_packages(id),
    student_id                  uuid not null references users(id),
    reason                      text,
    requested_sessions          int not null,
    approved_sessions           int not null default 0,
    refund_amount_vnd           bigint,
    status                      varchar(20) not null default 'PENDING',
    admin_note                  text,
    bank_name                   varchar(150),
    bank_bin                    varchar(20),
    account_number_encrypted    text,
    account_holder_name         varchar(150),
    bank_reference               varchar(255),
    proof_public_id              varchar(255),
    proof_url                    varchar(500),
    processed_by                 uuid references users(id),
    processed_at                 timestamptz,
    version                      bigint not null default 0,
    created_at                   timestamptz not null default now(),
    updated_at                   timestamptz not null default now(),
    is_deleted                   boolean not null default false,
    constraint ck_refund_requests_sessions check (requested_sessions > 0 and approved_sessions >= 0)
);
-- Tối đa 1 request đang xử lý (PENDING) cho mỗi package
create unique index ux_refund_requests_pending_package on refund_requests (student_package_id) where status = 'PENDING';
create index ix_refund_requests_status on refund_requests (status, created_at);
create trigger trg_refund_requests_updated before update on refund_requests for each row execute function set_updated_at();

create table package_extension_requests (
    id                       uuid primary key default gen_random_uuid(),
    student_package_id      uuid not null references student_packages(id),
    student_id               uuid not null references users(id),
    reason                   text,
    requested_expiry_date    timestamptz not null,
    approved_expiry_date     timestamptz,
    status                   varchar(20) not null default 'PENDING',
    admin_note               text,
    reviewed_by              uuid references users(id),
    reviewed_at              timestamptz,
    created_at               timestamptz not null default now(),
    updated_at               timestamptz not null default now(),
    is_deleted               boolean not null default false
);
-- Tối đa 1 request PENDING cho mỗi package
create unique index ux_extension_requests_pending_package on package_extension_requests (student_package_id) where status = 'PENDING';
create trigger trg_extension_requests_updated before update on package_extension_requests for each row execute function set_updated_at();

-- =====================================================================
-- 7. ASSIGNMENT & SUBMISSION
-- =====================================================================

create table assignments (
    id               uuid primary key default gen_random_uuid(),
    teacher_id       uuid not null references teacher_profiles(id),
    student_id       uuid not null references users(id),
    subject_id       uuid not null references subjects(id),
    title            varchar(255) not null,
    assignment_type  varchar(30) not null,
    content_blocks   jsonb,
    quiz_schema      jsonb,
    due_at           timestamptz,
    status           varchar(20) not null default 'ASSIGNED',
    created_at       timestamptz not null default now(),
    updated_at       timestamptz not null default now(),
    is_deleted       boolean not null default false
);
create index ix_assignments_student_status_due on assignments (student_id, status, due_at);
create trigger trg_assignments_updated before update on assignments for each row execute function set_updated_at();

create table submissions (
    id               uuid primary key default gen_random_uuid(),
    assignment_id    uuid not null references assignments(id),
    student_id       uuid not null references users(id),
    content_blocks   jsonb,
    submitted_at     timestamptz,
    status           varchar(20) not null default 'SUBMITTED',
    score            numeric(5,2),
    feedback_text    text,
    graded_at        timestamptz,
    created_at       timestamptz not null default now(),
    updated_at       timestamptz not null default now(),
    is_deleted       boolean not null default false
);
create index ix_submissions_assignment_student on submissions (assignment_id, student_id);
create trigger trg_submissions_updated before update on submissions for each row execute function set_updated_at();

-- =====================================================================
-- 8. ATTACHMENT / MESSAGING / NOTIFICATION / AUDIT
-- =====================================================================

create table attachments (
    id                     uuid primary key default gen_random_uuid(),
    owner_id               uuid not null references users(id),
    attachable_type        varchar(50) not null,
    attachable_id          uuid not null,
    cloudinary_public_id   varchar(255),
    secure_url             varchar(500) not null,
    original_filename      varchar(255),
    mime_type              varchar(100),
    file_size              bigint,
    created_at             timestamptz not null default now(),
    updated_at             timestamptz not null default now(),
    is_deleted             boolean not null default false
);
create index ix_attachments_attachable on attachments (attachable_type, attachable_id);
create trigger trg_attachments_updated before update on attachments for each row execute function set_updated_at();

create table conversations (
    id                uuid primary key default gen_random_uuid(),
    teacher_id        uuid not null references teacher_profiles(id),
    student_id        uuid not null references users(id),
    last_message_at   timestamptz,
    created_at        timestamptz not null default now(),
    updated_at        timestamptz not null default now(),
    is_deleted        boolean not null default false,
    constraint uq_conversations_pair unique (teacher_id, student_id)
);
create trigger trg_conversations_updated before update on conversations for each row execute function set_updated_at();

create table messages (
    id                 uuid primary key default gen_random_uuid(),
    conversation_id    uuid not null references conversations(id),
    sender_id          uuid not null references users(id),
    client_message_id  uuid not null,
    message_type       varchar(20) not null default 'TEXT',
    content             text,
    attachment_id       uuid references attachments(id),
    sent_at             timestamptz not null default now(),
    read_at             timestamptz,
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now(),
    is_deleted           boolean not null default false,
    constraint uq_messages_sender_client unique (sender_id, client_message_id)
);
create index ix_messages_conversation_sent on messages (conversation_id, sent_at desc);
create trigger trg_messages_updated before update on messages for each row execute function set_updated_at();

create table notifications (
    id              uuid primary key default gen_random_uuid(),
    user_id         uuid not null references users(id),
    type            varchar(50) not null,
    title           varchar(255) not null,
    content         text,
    reference_type  varchar(30),
    reference_id    uuid,
    is_read         boolean not null default false,
    created_at      timestamptz not null default now()
);
create index ix_notifications_user_read on notifications (user_id, is_read, created_at desc);

-- Audit log: append-only, KHÔNG update/delete
create table audit_logs (
    id           uuid primary key default gen_random_uuid(),
    actor_id     uuid references users(id),
    action       varchar(100) not null,
    target_type  varchar(50) not null,
    target_id    uuid not null,
    before_data  jsonb,
    after_data   jsonb,
    ip_address   inet,
    user_agent   text,
    created_at   timestamptz not null default now()
);
create index ix_audit_logs_actor on audit_logs (actor_id, created_at desc);
create index ix_audit_logs_target on audit_logs (target_type, target_id, created_at desc);

-- =====================================================================
-- 9. PLATFORM SETTINGS
-- =====================================================================

create table platform_settings (
    id                        uuid primary key default gen_random_uuid(),
    commission_rate           numeric(5,2) not null default 10.00,
    bayesian_minimum_reviews  int not null default 10,
    booking_reminder_hours    int not null default 11,
    booking_expiration_hours  int not null default 12,
    updated_at                timestamptz not null default now()
);
create trigger trg_platform_settings_updated before update on platform_settings for each row execute function set_updated_at();
