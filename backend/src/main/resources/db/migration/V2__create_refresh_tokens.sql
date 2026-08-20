-- =====================================================================
-- V2: REFRESH TOKENS TABLE
-- =====================================================================

create table refresh_tokens (
    id           uuid primary key default gen_random_uuid(),
    user_id      uuid not null references users(id),
    token_hash   varchar(255) not null,
    expires_at   timestamptz not null,
    revoked_at   timestamptz,
    device_info  varchar(255),
    ip_address   inet,
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now(),
    is_deleted   boolean not null default false
);
create unique index ux_refresh_tokens_hash on refresh_tokens (token_hash);
create index ix_refresh_tokens_user on refresh_tokens (user_id);
create trigger trg_refresh_tokens_updated before update on refresh_tokens for each row execute function set_updated_at();
