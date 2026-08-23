-- =====================================================================
-- V12: ASSIGNMENTS & SUBMISSIONS
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
