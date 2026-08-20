-- =====================================================================
-- V15: PLATFORM SETTINGS TABLE
-- =====================================================================

create table platform_settings (
    id                        uuid primary key default gen_random_uuid(),
    commission_rate           numeric(5,2) not null default 10.00,
    bayesian_minimum_reviews  int not null default 10,
    booking_reminder_hours    int not null default 11,
    booking_expiration_hours  int not null default 12,
    updated_at                timestamptz not null default now()
);
create trigger trg_platform_settings_updated before update on platform_settings for each row execute function set_updated_at();
