-- =====================================================================
-- V22: Harden booking domain, trial uniqueness, idempotency & batch indexes
-- =====================================================================

-- 1. Constraints on bookings
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_bookings_delivery_mode') THEN
        ALTER TABLE bookings
            ADD CONSTRAINT ck_bookings_delivery_mode
            CHECK (delivery_mode IN ('ONLINE', 'OFFLINE'));
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_bookings_cancel_initiated_by') THEN
        ALTER TABLE bookings
            ADD CONSTRAINT ck_bookings_cancel_initiated_by
            CHECK (cancel_initiated_by IS NULL OR cancel_initiated_by IN ('TEACHER', 'STUDENT_REQUEST', 'SYSTEM'));
    END IF;
END $$;

-- 2. Partial unique index: at most one active/completed trial booking per student-teacher pair
CREATE UNIQUE INDEX IF NOT EXISTS ux_bookings_trial_pair
    ON bookings (student_id, teacher_id)
    WHERE is_trial = true AND status IN ('SCHEDULED', 'COMPLETED') AND is_deleted = false;

-- 3. Unique index for notification idempotency (e.g., booking reminders)
CREATE UNIQUE INDEX IF NOT EXISTS ux_notifications_idempotency
    ON notifications (user_id, type, reference_type, reference_id)
    WHERE reference_id IS NOT NULL;

-- 4. Batch query indexes
CREATE INDEX IF NOT EXISTS ix_bookings_scheduled_expiry
    ON bookings (status, end_time)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS ix_student_packages_active_expiry
    ON student_packages (status, expires_at)
    WHERE is_deleted = false;

-- 5. Constraint on session reports rating
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_session_reports_rating') THEN
        ALTER TABLE session_reports
            ADD CONSTRAINT ck_session_reports_rating
            CHECK (teacher_self_rating IS NULL OR (teacher_self_rating >= 1 AND teacher_self_rating <= 5));
    END IF;
END $$;
