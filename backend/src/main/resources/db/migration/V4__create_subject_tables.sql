-- =====================================================================
-- V4: SUBJECTS, PROPOSALS & TEACHER SUBJECTS
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
