-- V29: forward-only hardening for finance workflow state and active-request uniqueness.
-- This migration deliberately refuses to coerce legacy data. Resolve reported rows
-- against proof, ledger and audit records before retrying the migration.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM refund_requests
        WHERE is_deleted = false AND status IN ('PROCESSING', 'FAILED')
    ) THEN
        RAISE EXCEPTION 'V29 preflight failed: refund_requests contains PROCESSING/FAILED rows';
    END IF;

    IF EXISTS (
        SELECT 1 FROM payout_requests
        WHERE is_deleted = false AND status = 'FAILED'
    ) THEN
        RAISE EXCEPTION 'V29 preflight failed: payout_requests contains FAILED rows';
    END IF;

    IF EXISTS (
        SELECT 1 FROM refund_requests
        WHERE is_deleted = false AND status IN ('PENDING', 'APPROVED')
        GROUP BY student_package_id HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'V29 preflight failed: multiple active refund requests for a package';
    END IF;

    IF EXISTS (
        SELECT 1 FROM package_extension_requests
        WHERE is_deleted = false AND status = 'PENDING'
        GROUP BY student_package_id HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'V29 preflight failed: multiple pending extensions for a package';
    END IF;

    IF EXISTS (
        SELECT 1 FROM payout_requests
        WHERE is_deleted = false AND status IN ('PENDING', 'PROCESSING')
        GROUP BY teacher_id HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'V29 preflight failed: multiple active payouts for a teacher';
    END IF;
END $$;

ALTER TABLE refund_requests ADD COLUMN IF NOT EXISTS transferred_at timestamptz;

ALTER TABLE refund_requests DROP CONSTRAINT IF EXISTS ck_refund_requests_status;
ALTER TABLE refund_requests ADD CONSTRAINT ck_refund_requests_status
    CHECK (status IN ('PENDING', 'APPROVED', 'REFUNDED', 'REJECTED'));

ALTER TABLE payout_requests DROP CONSTRAINT IF EXISTS ck_payout_requests_status;
ALTER TABLE payout_requests ADD CONSTRAINT ck_payout_requests_status
    CHECK (status IN ('PENDING', 'PROCESSING', 'SUCCEEDED', 'REJECTED'));

DROP INDEX IF EXISTS ux_refund_requests_pending_package;
CREATE UNIQUE INDEX ux_refund_requests_active_package
    ON refund_requests (student_package_id)
    WHERE status IN ('PENDING', 'APPROVED') AND is_deleted = false;

DROP INDEX IF EXISTS ux_extension_requests_pending_package;
CREATE UNIQUE INDEX ux_extension_requests_pending_package
    ON package_extension_requests (student_package_id)
    WHERE status = 'PENDING' AND is_deleted = false;

CREATE UNIQUE INDEX ux_payout_requests_active_teacher
    ON payout_requests (teacher_id)
    WHERE status IN ('PENDING', 'PROCESSING') AND is_deleted = false;

CREATE TABLE finance_command_receipts (
    id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id             uuid NOT NULL REFERENCES users(id),
    operation            varchar(80) NOT NULL,
    idempotency_key      uuid NOT NULL,
    request_fingerprint  varchar(64) NOT NULL,
    response_type        varchar(255) NOT NULL,
    response_payload     jsonb NOT NULL,
    created_at           timestamptz NOT NULL DEFAULT now(),
    expires_at           timestamptz NOT NULL,
    CONSTRAINT uq_finance_command_receipt_key
        UNIQUE (actor_id, operation, idempotency_key)
);

CREATE INDEX ix_finance_command_receipts_expiry
    ON finance_command_receipts (expires_at);
