-- =====================================================================
-- V8: BOOKINGS, TRIAL REQUESTS & SESSION REPORTS
-- =====================================================================

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
