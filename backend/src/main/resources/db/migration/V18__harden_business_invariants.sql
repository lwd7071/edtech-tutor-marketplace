-- =====================================================================
-- V18: Harden business invariants discovered after runtime-verifying V1-V17.
-- V1-V17 are immutable; all remediation is forward-only in this migration.
-- =====================================================================

-- V17 used "deleted" while BaseEntity maps "is_deleted".
DO $$
DECLARE
    has_deleted boolean;
    has_is_deleted boolean;
BEGIN
    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'refresh_tokens' AND column_name = 'deleted'
    ) INTO has_deleted;
    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'refresh_tokens' AND column_name = 'is_deleted'
    ) INTO has_is_deleted;

    IF has_deleted AND NOT has_is_deleted THEN
        ALTER TABLE refresh_tokens RENAME COLUMN deleted TO is_deleted;
    ELSIF has_deleted AND has_is_deleted THEN
        -- V2 already created is_deleted and V17 added deleted. Preserve a deletion
        -- recorded through either mapping before removing the duplicate column.
        UPDATE refresh_tokens
        SET is_deleted = is_deleted OR deleted
        WHERE deleted = true;
        ALTER TABLE refresh_tokens DROP COLUMN deleted;
    ELSIF NOT has_is_deleted THEN
        RAISE EXCEPTION 'table=refresh_tokens, constraint=base_entity_column, invalid_rows=missing_deleted_column';
    END IF;
END $$;

-- V3 used varchar(50)[] while the Hibernate mapping and public profile model use text[].
-- The widening conversion is lossless and lets ddl-auto=validate catch future drift.
ALTER TABLE teacher_profiles
    ALTER COLUMN languages TYPE text[] USING languages::text[];

-- Platform commission is 5% by default. Existing configured settings are preserved.
ALTER TABLE platform_settings ALTER COLUMN commission_rate SET DEFAULT 5.00;
INSERT INTO platform_settings (commission_rate, bayesian_minimum_reviews,
                               booking_reminder_hours, booking_expiration_hours, is_singleton)
SELECT 5.00, 10, 11, 12, true
WHERE NOT EXISTS (SELECT 1 FROM platform_settings);

DO $$
DECLARE
    invalid_count bigint;
BEGIN
    SELECT count(*) INTO invalid_count
    FROM student_packages
    WHERE commission_rate IS NOT NULL AND commission_rate <> 5.00;
    IF invalid_count > 0 THEN
        RAISE EXCEPTION 'table=student_packages, constraint=commission_snapshot_source, invalid_rows=%', invalid_count;
    END IF;
END $$;

UPDATE student_packages
SET commission_rate = (SELECT commission_rate FROM platform_settings WHERE is_singleton = true)
WHERE commission_rate IS NULL;
ALTER TABLE student_packages ALTER COLUMN commission_rate SET NOT NULL;

-- Known mechanical legacy mapping produced by V3's old default.
UPDATE teacher_profiles SET profile_status = 'PENDING_APPROVAL' WHERE profile_status = 'PENDING';

-- Defaults used for all new rows after V18.
ALTER TABLE users ALTER COLUMN status SET DEFAULT 'PENDING_VERIFICATION';
ALTER TABLE teacher_profiles ALTER COLUMN profile_status SET DEFAULT 'DRAFT';
ALTER TABLE pricing_packages ALTER COLUMN status SET DEFAULT 'DRAFT';
ALTER TABLE student_packages ALTER COLUMN status SET DEFAULT 'PENDING_PAYMENT';
ALTER TABLE assignments ALTER COLUMN status SET DEFAULT 'DRAFT';
ALTER TABLE submissions ALTER COLUMN status SET DEFAULT 'DRAFT';

-- Reusable audit helper. It never coerces unknown data.
CREATE OR REPLACE FUNCTION pg_temp.assert_allowed_values(
    target_table regclass,
    target_column text,
    target_constraint text,
    allowed_values text[]
) RETURNS void LANGUAGE plpgsql AS $$
DECLARE
    invalid_count bigint;
BEGIN
    EXECUTE format(
        'SELECT count(*) FROM %s WHERE %I IS NULL OR NOT (%I = ANY ($1))',
        target_table, target_column, target_column
    ) INTO invalid_count USING allowed_values;

    IF invalid_count > 0 THEN
        RAISE EXCEPTION 'table=%, constraint=%, invalid_rows=%',
            target_table::text, target_constraint, invalid_count;
    END IF;
END $$;

SELECT pg_temp.assert_allowed_values('users', 'status', 'ck_users_status',
    ARRAY['PENDING_VERIFICATION','ACTIVE','LOCKED','DISABLED']);
ALTER TABLE users ADD CONSTRAINT ck_users_status
    CHECK (status IN ('PENDING_VERIFICATION','ACTIVE','LOCKED','DISABLED'));

SELECT pg_temp.assert_allowed_values('teacher_profiles', 'profile_status', 'ck_teacher_profiles_status',
    ARRAY['DRAFT','PENDING_APPROVAL','APPROVED','REJECTED']);
ALTER TABLE teacher_profiles ADD CONSTRAINT ck_teacher_profiles_status
    CHECK (profile_status IN ('DRAFT','PENDING_APPROVAL','APPROVED','REJECTED'));

SELECT pg_temp.assert_allowed_values('teacher_documents', 'verification_status', 'ck_teacher_documents_status',
    ARRAY['PENDING','VERIFIED','REJECTED']);
ALTER TABLE teacher_documents ADD CONSTRAINT ck_teacher_documents_status
    CHECK (verification_status IN ('PENDING','VERIFIED','REJECTED'));

SELECT pg_temp.assert_allowed_values('subject_proposals', 'status', 'ck_subject_proposals_status',
    ARRAY['PENDING','APPROVED','REJECTED']);
ALTER TABLE subject_proposals ADD CONSTRAINT ck_subject_proposals_status
    CHECK (status IN ('PENDING','APPROVED','REJECTED'));

SELECT pg_temp.assert_allowed_values('pricing_packages', 'status', 'ck_pricing_packages_status',
    ARRAY['DRAFT','ACTIVE','INACTIVE']);
ALTER TABLE pricing_packages ADD CONSTRAINT ck_pricing_packages_status
    CHECK (status IN ('DRAFT','ACTIVE','INACTIVE'));

SELECT pg_temp.assert_allowed_values('invoices', 'status', 'ck_invoices_status',
    ARRAY['PENDING','PAID','CANCELLED','EXPIRED']);
ALTER TABLE invoices ADD CONSTRAINT ck_invoices_status
    CHECK (status IN ('PENDING','PAID','CANCELLED','EXPIRED'));

SELECT pg_temp.assert_allowed_values('student_packages', 'status', 'ck_student_packages_status',
    ARRAY['PENDING_PAYMENT','ACTIVE','COMPLETED','REFUND_PENDING','REFUNDED','LOCKED_EXPIRED']);
ALTER TABLE student_packages ADD CONSTRAINT ck_student_packages_status
    CHECK (status IN ('PENDING_PAYMENT','ACTIVE','COMPLETED','REFUND_PENDING','REFUNDED','LOCKED_EXPIRED'));

SELECT pg_temp.assert_allowed_values('payout_requests', 'status', 'ck_payout_requests_status',
    ARRAY['PENDING','PROCESSING','SUCCEEDED','REJECTED','FAILED']);
ALTER TABLE payout_requests ADD CONSTRAINT ck_payout_requests_status
    CHECK (status IN ('PENDING','PROCESSING','SUCCEEDED','REJECTED','FAILED'));

SELECT pg_temp.assert_allowed_values('refund_requests', 'status', 'ck_refund_requests_status',
    ARRAY['PENDING','APPROVED','PROCESSING','REFUNDED','REJECTED','FAILED']);
ALTER TABLE refund_requests ADD CONSTRAINT ck_refund_requests_status
    CHECK (status IN ('PENDING','APPROVED','PROCESSING','REFUNDED','REJECTED','FAILED'));

SELECT pg_temp.assert_allowed_values('package_extension_requests', 'status', 'ck_package_extension_requests_status',
    ARRAY['PENDING','APPROVED','REJECTED']);
ALTER TABLE package_extension_requests ADD CONSTRAINT ck_package_extension_requests_status
    CHECK (status IN ('PENDING','APPROVED','REJECTED'));

SELECT pg_temp.assert_allowed_values('assignments', 'assignment_type', 'ck_assignments_type',
    ARRAY['SYSTEM_QUIZ','FREEFORM']);
ALTER TABLE assignments ADD CONSTRAINT ck_assignments_type
    CHECK (assignment_type IN ('SYSTEM_QUIZ','FREEFORM'));

SELECT pg_temp.assert_allowed_values('assignments', 'status', 'ck_assignments_status',
    ARRAY['DRAFT','PUBLISHED','CLOSED']);
ALTER TABLE assignments ADD CONSTRAINT ck_assignments_status
    CHECK (status IN ('DRAFT','PUBLISHED','CLOSED'));

SELECT pg_temp.assert_allowed_values('submissions', 'status', 'ck_submissions_status',
    ARRAY['DRAFT','SUBMITTED','GRADED']);
ALTER TABLE submissions ADD CONSTRAINT ck_submissions_status
    CHECK (status IN ('DRAFT','SUBMITTED','GRADED'));

SELECT pg_temp.assert_allowed_values('messages', 'message_type', 'ck_messages_type',
    ARRAY['TEXT','IMAGE','FILE']);
ALTER TABLE messages ADD CONSTRAINT ck_messages_type
    CHECK (message_type IN ('TEXT','IMAGE','FILE'));
