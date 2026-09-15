-- V40: server-owned teacher credential badges
create table teacher_credentials (
    id                    uuid primary key default gen_random_uuid(),
    teacher_id            uuid not null references teacher_profiles(id),
    label                 varchar(120) not null,
    evidence_public_id    varchar(255) not null,
    evidence_resource_type varchar(20) not null,
    evidence_format      varchar(20) not null,
    evidence_mime_type   varchar(100) not null,
    evidence_size        bigint not null,
    status                varchar(20) not null default 'PENDING',
    rejected_reason       text,
    approved_by           uuid references users(id),
    approved_at           timestamptz,
    version               bigint not null default 0,
    created_at            timestamptz not null default now(),
    updated_at            timestamptz not null default now(),
    is_deleted            boolean not null default false,
    constraint ck_teacher_credentials_status check (status in ('PENDING','APPROVED','REJECTED')),
    constraint ck_teacher_credentials_label check (length(btrim(label)) between 1 and 120),
    constraint ck_teacher_credentials_file_size check (evidence_size > 0 and evidence_size <= 10485760),
    constraint ck_teacher_credentials_approval_fields check (
        (status = 'APPROVED' and approved_by is not null and approved_at is not null and rejected_reason is null)
        or (status = 'REJECTED' and rejected_reason is not null and approved_by is null and approved_at is null)
        or (status = 'PENDING' and approved_by is null and approved_at is null and rejected_reason is null)
    )
);
create unique index ux_teacher_credentials_teacher_label on teacher_credentials (teacher_id, lower(btrim(label))) where is_deleted = false;
create index ix_teacher_credentials_status_created on teacher_credentials (status, created_at) where is_deleted = false;
create index ix_teacher_credentials_teacher_status on teacher_credentials (teacher_id, status) where is_deleted = false;
create trigger trg_teacher_credentials_updated before update on teacher_credentials for each row execute function set_updated_at();

-- Backend-only proof metadata: default-deny for PostgREST roles.
alter table teacher_credentials enable row level security;
do $$
begin
    if exists (select 1 from pg_roles where rolname = 'anon') then
        revoke all on table teacher_credentials from anon;
    end if;
    if exists (select 1 from pg_roles where rolname = 'authenticated') then
        revoke all on table teacher_credentials from authenticated;
    end if;
end $$;
