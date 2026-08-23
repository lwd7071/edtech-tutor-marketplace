-- =====================================================================
-- V7: STUDENT PACKAGES TABLE
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
