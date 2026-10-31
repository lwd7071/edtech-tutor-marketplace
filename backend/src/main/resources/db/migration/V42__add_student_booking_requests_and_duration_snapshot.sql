-- Preserve the purchased lesson length after catalog package changes.
alter table student_packages
    add column session_duration_minutes_snapshot integer;

update student_packages sp
set session_duration_minutes_snapshot = i.session_duration_minutes_snapshot
from invoices i
where i.id = sp.invoice_id
  and sp.session_duration_minutes_snapshot is null;

do $$
begin
    if exists (
        select 1 from student_packages
        where session_duration_minutes_snapshot is null
           or session_duration_minutes_snapshot <= 0
    ) then
        raise exception 'Cannot backfill a valid session duration for every student package';
    end if;
end $$;

alter table student_packages
    alter column session_duration_minutes_snapshot set not null;

alter table student_packages
    add constraint ck_student_packages_session_duration_snapshot
    check (session_duration_minutes_snapshot > 0);

create table booking_requests (
    id uuid primary key default gen_random_uuid(),
    student_package_id uuid not null references student_packages(id),
    student_id uuid not null references users(id),
    teacher_id uuid not null references teacher_profiles(id),
    subject_id uuid not null references subjects(id),
    preferred_start_time timestamptz not null,
    student_note varchar(500),
    status varchar(20) not null default 'PENDING',
    decision_reason text,
    accepted_booking_id uuid unique references bookings(id),
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    is_deleted boolean not null default false,
    constraint ck_booking_requests_status check (status in ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED')),
    constraint ck_booking_requests_accepted_booking check (
        (status = 'ACCEPTED' and accepted_booking_id is not null)
        or (status <> 'ACCEPTED' and accepted_booking_id is null)
    ),
    constraint ck_booking_requests_rejection_reason check (
        status <> 'REJECTED' or length(btrim(decision_reason)) > 0
    )
);

create unique index uq_booking_requests_one_pending_per_package
    on booking_requests(student_package_id)
    where status = 'PENDING' and is_deleted = false;
create index ix_booking_requests_student_created
    on booking_requests(student_id, created_at desc, id desc);
create index ix_booking_requests_teacher_status_created
    on booking_requests(teacher_id, status, created_at, id);
create trigger trg_booking_requests_updated
    before update on booking_requests
    for each row execute function set_updated_at();

alter table booking_requests enable row level security;
do $$
begin
    if exists (select 1 from pg_roles where rolname = 'anon') then
        execute 'revoke all on booking_requests from anon';
    end if;
    if exists (select 1 from pg_roles where rolname = 'authenticated') then
        execute 'revoke all on booking_requests from authenticated';
    end if;
end $$;
