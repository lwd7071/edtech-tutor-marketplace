ALTER TABLE notifications
    ADD COLUMN updated_at timestamptz NOT NULL DEFAULT now(),
    ADD COLUMN is_deleted boolean NOT NULL DEFAULT false;

CREATE TRIGGER trg_notifications_updated
    BEFORE UPDATE ON notifications
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
