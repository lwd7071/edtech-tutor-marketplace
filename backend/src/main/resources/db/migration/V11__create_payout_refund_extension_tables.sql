-- =====================================================================
-- V11: PAYOUT, REFUND & EXTENSION REQUESTS
-- =====================================================================

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
