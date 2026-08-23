-- =====================================================================
-- V1: EXTENSIONS, TRIGGER FUNCTION & USERS TABLE
-- =====================================================================

create extension if not exists pgcrypto;   -- gen_random_uuid()
create extension if not exists btree_gist; -- exclusion constraint chống overlap lịch

create or replace function set_updated_at()
returns trigger as $$
begin
  new.updated_at = now();
  return new;
end;
$$ language plpgsql;

create table users (
    id                uuid primary key default gen_random_uuid(),
    email             varchar(255) not null,
    password_hash     varchar(255),
    full_name         varchar(150) not null,
    avatar_url        varchar(500),
    phone             varchar(20),
    parent_full_name  varchar(150),
    parent_phone      varchar(20),
    parent_email      varchar(255),
    notify_parent     boolean not null default false,
    role              varchar(20) not null check (role in ('STUDENT','TEACHER','ADMIN')),
    status            varchar(20) not null default 'ACTIVE',
    email_verified    boolean not null default false,
    oauth_provider    varchar(50),
    oauth_subject     varchar(255),
    last_login_at     timestamptz,
    created_at        timestamptz not null default now(),
    updated_at        timestamptz not null default now(),
    is_deleted        boolean not null default false
);
create unique index ux_users_email on users (lower(email)) where is_deleted = false;
create unique index ux_users_oauth on users (oauth_provider, oauth_subject) where oauth_provider is not null and is_deleted = false;
create index ix_users_role_status on users (role, status);
create trigger trg_users_updated before update on users for each row execute function set_updated_at();
