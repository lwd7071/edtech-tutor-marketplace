-- =====================================================================
-- V5: PRICING PACKAGES TABLE
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
