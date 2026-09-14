-- =====================================================================
-- V37: ENABLE ROW LEVEL SECURITY & REVOKE PRIVILEGES ON SYSTEM TABLES
-- =====================================================================

-- 1. Bật Row Level Security default-deny cho các bảng System & Outbox
ALTER TABLE platform_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE email_outbox ENABLE ROW LEVEL SECURITY;

-- 2. Xử lý bảng rác modulebentity (nếu tồn tại từ test fixture cũ trên cloud)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = 'modulebentity') THEN
        EXECUTE 'ALTER TABLE modulebentity ENABLE ROW LEVEL SECURITY;';
    END IF;
END $$;

-- 3. Thu hồi toàn bộ quyền truy cập và TRUNCATE từ các vai trò unprivileged (anon, authenticated)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
        REVOKE ALL ON TABLE
            platform_settings,
            audit_logs,
            email_outbox
        FROM anon;

        IF EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = 'modulebentity') THEN
            EXECUTE 'REVOKE ALL ON TABLE modulebentity FROM anon;';
        END IF;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
        REVOKE ALL ON TABLE
            platform_settings,
            audit_logs,
            email_outbox
        FROM authenticated;

        IF EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = 'modulebentity') THEN
            EXECUTE 'REVOKE ALL ON TABLE modulebentity FROM authenticated;';
        END IF;
    END IF;
END $$;