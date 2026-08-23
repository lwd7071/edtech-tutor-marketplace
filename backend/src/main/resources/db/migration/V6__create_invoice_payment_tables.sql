-- =====================================================================
-- V6: INVOICES & PAYMENT TRANSACTIONS
-- =====================================================================

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
