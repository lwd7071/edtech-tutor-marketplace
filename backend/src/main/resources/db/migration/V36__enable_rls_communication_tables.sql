-- =====================================================================
-- V36: ENABLE ROW LEVEL SECURITY & REVOKE TRUNCATE ON COMMUNICATION
-- =====================================================================

-- 1. Bật Row Level Security default-deny cho 4 bảng Communication & Notifications
ALTER TABLE conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE attachments ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;

-- 2. REVOKE TRUNCATE đối với unprivileged roles để triệt tiêu nguy cơ RLS bypass
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
        REVOKE TRUNCATE ON TABLE
            conversations,
            messages,
            attachments,
            notifications
        FROM anon;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
        REVOKE TRUNCATE ON TABLE
            conversations,
            messages,
            attachments,
            notifications
        FROM authenticated;
    END IF;
END $$;
