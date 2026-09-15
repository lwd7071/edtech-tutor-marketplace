-- Two-party confirmation state and platform escrow audit trail.
create table booking_settlements (
    id uuid primary key default gen_random_uuid(),
    booking_id uuid not null references bookings(id),
    status varchar(40) not null,
    teacher_confirmed_at timestamptz,
    student_confirmed_at timestamptz,
    initial_deadline timestamptz not null,
    reopen_deadline timestamptz,
    net_amount_vnd bigint check (net_amount_vnd >= 0),
    dispute_reason text,
    disputed_at timestamptz,
    reopened_at timestamptz,
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    is_deleted boolean not null default false,
    constraint uq_booking_settlements_booking unique (booking_id),
    constraint ck_booking_settlements_status check (status in ('AWAITING_CONFIRMATION','HELD','DISPUTE_PENDING','REOPENED','AWAITING_ADMIN_DECISION','RELEASED','RETAINED'))
);
create index ix_booking_settlements_expiry on booking_settlements(status, initial_deadline);
create index ix_booking_settlements_reopen_expiry on booking_settlements(status, reopen_deadline);
create trigger trg_booking_settlements_updated before update on booking_settlements for each row execute function set_updated_at();

create table platform_ledger_entries (
    id uuid primary key default gen_random_uuid(),
    booking_id uuid not null references bookings(id),
    bucket varchar(20) not null check (bucket in ('ESCROW','REVENUE')),
    direction varchar(10) not null check (direction in ('CREDIT','DEBIT')),
    amount_vnd bigint not null check (amount_vnd > 0),
    idempotency_key varchar(255) not null unique,
    description text,
    is_deleted boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index ix_platform_ledger_booking on platform_ledger_entries(booking_id, created_at desc);

-- Backfill the settlement read model for existing paid bookings. Historical
-- completed rows keep their completed/reviewable state; the amount is copied
-- from the legacy SESSION_CREDIT_AVAILABLE entry when one exists.
insert into booking_settlements (
    booking_id, status, initial_deadline, net_amount_vnd, created_at, updated_at
)
select b.id,
       case when b.status = 'COMPLETED' and b.settlement_processed
            then 'RELEASED' else 'AWAITING_CONFIRMATION' end,
       b.end_time + interval '24 hours',
       case when b.status = 'COMPLETED' and b.settlement_processed
            then coalesce(legacy_credit.amount_vnd, 0) else null end,
       now(), now()
from bookings b
left join session_reports sr on sr.booking_id = b.id
left join lateral (
    select le.amount_vnd
    from ledger_entries le
    where le.reference_type = 'SESSION_REPORT'
      and le.reference_id = sr.id
      and le.entry_type = 'SESSION_CREDIT_AVAILABLE'
    order by le.created_at desc
    limit 1
) legacy_credit on true
where b.is_trial = false
  and b.status in ('SCHEDULED', 'COMPLETED')
on conflict (booking_id) do nothing;

-- The two wallet movements for escrow must pass the same allow-list as every other ledger entry.
-- Keep the existing varchar(30) contract; new values are deliberately <= 30 chars.
alter table ledger_entries drop constraint ck_ledger_entries_entry_type;
alter table ledger_entries add constraint ck_ledger_entries_entry_type check (entry_type in (
    'PACKAGE_FUNDED', 'SESSION_RELEASE_PENDING', 'SESSION_CREDIT_AVAILABLE',
    'SESSION_ESCROW_HELD', 'SESSION_ESCROW_RELEASED',
    'COMMISSION_RECOGNIZED', 'PAYOUT_RESERVED', 'PAYOUT_SUCCEEDED',
    'PAYOUT_RELEASED', 'REFUND_DEBIT_PENDING', 'ADJUSTMENT'
));

create function reject_platform_ledger_mutation() returns trigger language plpgsql as $$
begin
    raise exception 'platform_ledger_entries is append-only';
end $$;
create trigger trg_platform_ledger_append_only before update or delete on platform_ledger_entries
    for each row execute function reject_platform_ledger_mutation();

alter table booking_settlements enable row level security;
alter table platform_ledger_entries enable row level security;
alter table provinces enable row level security;
alter table wards enable row level security;
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
        EXECUTE 'REVOKE ALL ON booking_settlements, platform_ledger_entries, provinces, wards FROM anon';
    END IF;
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
        EXECUTE 'REVOKE ALL ON booking_settlements, platform_ledger_entries, provinces, wards FROM authenticated';
    END IF;
END $$;
