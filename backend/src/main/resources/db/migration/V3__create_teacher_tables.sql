-- =====================================================================
-- V3: TEACHER PROFILES, DOCUMENTS & AVAILABILITIES
-- =====================================================================

create table teacher_profiles (
    id                       uuid primary key default gen_random_uuid(),
    user_id                  uuid not null references users(id),
    bio                      text,
    years_of_experience      int,
    languages                varchar(50)[],
    supports_online          boolean not null default true,
    supports_offline         boolean not null default false,
    location_address         text,
    introduction_video_url   varchar(500),
    profile_status           varchar(20) not null default 'PENDING',
    rejection_reason         text,
    verified_badge           boolean not null default false,
    is_visible               boolean not null default false,
    approved_at              timestamptz,
    approved_by              uuid references users(id),
    created_at               timestamptz not null default now(),
    updated_at               timestamptz not null default now(),
    is_deleted               boolean not null default false,
    constraint uq_teacher_profiles_user unique (user_id)
);
create index ix_teacher_profiles_status_visible on teacher_profiles (profile_status, is_visible);
create index ix_teacher_profiles_languages on teacher_profiles using gin (languages);
create trigger trg_teacher_profiles_updated before update on teacher_profiles for each row execute function set_updated_at();

create table teacher_documents (
    id                    uuid primary key default gen_random_uuid(),
    teacher_id            uuid not null references teacher_profiles(id),
    document_type         varchar(50) not null,
    title                 varchar(255),
    cloudinary_public_id  varchar(255),
    secure_url            varchar(500) not null,
    mime_type             varchar(100),
    file_size             bigint,
    verification_status   varchar(20) not null default 'PENDING',
    verified_by           uuid references users(id),
    verified_at           timestamptz,
    created_at            timestamptz not null default now(),
    updated_at            timestamptz not null default now(),
    is_deleted            boolean not null default false
);
create index ix_teacher_documents_teacher on teacher_documents (teacher_id);
create trigger trg_teacher_documents_updated before update on teacher_documents for each row execute function set_updated_at();

create table teacher_availabilities (
    id          uuid primary key default gen_random_uuid(),
    teacher_id  uuid not null references teacher_profiles(id),
    day_of_week varchar(10) not null,
    start_time  time not null,
    end_time    time not null,
    timezone    varchar(50) not null default 'Asia/Ho_Chi_Minh',
    is_active   boolean not null default true,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now(),
    is_deleted  boolean not null default false,
    constraint ck_availability_time check (start_time < end_time)
);
create index ix_availabilities_teacher on teacher_availabilities (teacher_id, day_of_week);
create trigger trg_availabilities_updated before update on teacher_availabilities for each row execute function set_updated_at();
