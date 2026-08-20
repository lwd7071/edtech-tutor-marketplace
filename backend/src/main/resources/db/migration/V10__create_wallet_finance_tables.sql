-- =====================================================================
-- V10: WALLETS, LEDGER ENTRIES & TEACHER BANK ACCOUNTS
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
