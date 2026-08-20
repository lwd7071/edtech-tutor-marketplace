-- =====================================================================
-- V9: REVIEWS & TEACHER STATS
-- =====================================================================

create table reviews (
    id            uuid primary key default gen_random_uuid(),
    booking_id    uuid not null references bookings(id),
    student_id    uuid not null references users(id),
    teacher_id    uuid not null references teacher_profiles(id),
    rating        smallint not null,
    comment       text,
    is_visible    boolean not null default true,
    moderated_by  uuid references users(id),
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now(),
    is_deleted    boolean not null default false,
    constraint uq_reviews_booking unique (booking_id),
    constraint ck_reviews_rating check (rating between 1 and 5)
);
create index ix_reviews_teacher_visible on reviews (teacher_id, is_visible, created_at desc);
create trigger trg_reviews_updated before update on reviews for each row execute function set_updated_at();

create table teacher_stats (
    teacher_id                 uuid primary key references teacher_profiles(id),
    average_rating             numeric(3,2) not null default 0,
    bayesian_rating            numeric(3,2) not null default 0,
    review_count               int not null default 0,
    completed_session_count    int not null default 0,
    completion_rate            numeric(5,4) not null default 0,
    trial_session_count        int not null default 0,
    trial_conversion_rate      numeric(5,4) not null default 0,
    global_rank                int,
    calculated_at              timestamptz not null default now()
);
