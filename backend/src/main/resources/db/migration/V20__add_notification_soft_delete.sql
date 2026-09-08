ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS updated_at timestamptz NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS is_deleted boolean NOT NULL DEFAULT false;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_notifications_updated') THEN
        CREATE TRIGGER trg_notifications_updated
            BEFORE UPDATE ON notifications
            FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END $$;
