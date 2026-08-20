-- =====================================================================
-- V14: AUDIT LOGS TABLE
-- =====================================================================

-- Audit log: append-only, KHÔNG update/delete
create table audit_logs (
    id           uuid primary key default gen_random_uuid(),
    actor_id     uuid references users(id),
    action       varchar(100) not null,
    target_type  varchar(50) not null,
    target_id    uuid not null,
    before_data  jsonb,
    after_data   jsonb,
    ip_address   inet,
    user_agent   text,
    created_at   timestamptz not null default now()
);
create index ix_audit_logs_actor on audit_logs (actor_id, created_at desc);
create index ix_audit_logs_target on audit_logs (target_type, target_id, created_at desc);
