-- =====================================================================
-- V13: ATTACHMENTS, CONVERSATIONS, MESSAGES & NOTIFICATIONS
-- =====================================================================

create table attachments (
    id                     uuid primary key default gen_random_uuid(),
    owner_id               uuid not null references users(id),
    attachable_type        varchar(50) not null,
    attachable_id          uuid not null,
    cloudinary_public_id   varchar(255),
    secure_url             varchar(500) not null,
    original_filename      varchar(255),
    mime_type              varchar(100),
    file_size              bigint,
    created_at             timestamptz not null default now(),
    updated_at             timestamptz not null default now(),
    is_deleted             boolean not null default false
);
create index ix_attachments_attachable on attachments (attachable_type, attachable_id);
create trigger trg_attachments_updated before update on attachments for each row execute function set_updated_at();

create table conversations (
    id                uuid primary key default gen_random_uuid(),
    teacher_id        uuid not null references teacher_profiles(id),
    student_id        uuid not null references users(id),
    last_message_at   timestamptz,
    created_at        timestamptz not null default now(),
    updated_at        timestamptz not null default now(),
    is_deleted        boolean not null default false,
    constraint uq_conversations_pair unique (teacher_id, student_id)
);
create trigger trg_conversations_updated before update on conversations for each row execute function set_updated_at();

create table messages (
    id                 uuid primary key default gen_random_uuid(),
    conversation_id    uuid not null references conversations(id),
    sender_id          uuid not null references users(id),
    client_message_id  uuid not null,
    message_type       varchar(20) not null default 'TEXT',
    content             text,
    attachment_id       uuid references attachments(id),
    sent_at             timestamptz not null default now(),
    read_at             timestamptz,
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now(),
    is_deleted           boolean not null default false,
    constraint uq_messages_sender_client unique (sender_id, client_message_id)
);
create index ix_messages_conversation_sent on messages (conversation_id, sent_at desc);
create trigger trg_messages_updated before update on messages for each row execute function set_updated_at();

create table notifications (
    id              uuid primary key default gen_random_uuid(),
    user_id         uuid not null references users(id),
    type            varchar(50) not null,
    title           varchar(255) not null,
    content         text,
    reference_type  varchar(30),
    reference_id    uuid,
    is_read         boolean not null default false,
    created_at      timestamptz not null default now()
);
create index ix_notifications_user_read on notifications (user_id, is_read, created_at desc);
