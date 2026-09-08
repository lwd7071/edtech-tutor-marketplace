-- =====================================================================
-- V21: Payment-ready domain, deterministic idempotency and constraints.
-- V1-V19 remain immutable; all changes are forward-only.
-- Pre-deploy gate: the target PostgreSQL role must be allowed to install
-- (or already have) pgcrypto.
-- =====================================================================

DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;
    IF EXISTS (
        SELECT 1 FROM pg_extension
        WHERE extname = 'pgcrypto' AND extnamespace != 'public'::regnamespace
    ) THEN
        ALTER EXTENSION pgcrypto SET SCHEMA public;
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS invoice_number_seq AS bigint START WITH 1 INCREMENT BY 1 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS payos_order_code_seq AS bigint START WITH 1 INCREMENT BY 1 NO CYCLE;
SELECT setval(
    'payos_order_code_seq',
    COALESCE((SELECT max(payos_order_code) FROM invoices), 0) + 1,
    false
);

ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS idempotency_key uuid,
    ADD COLUMN IF NOT EXISTS request_fingerprint varchar(64);

UPDATE invoices
SET idempotency_key = COALESCE(idempotency_key, id),
    payos_order_code = COALESCE(payos_order_code, nextval('payos_order_code_seq')),
    request_fingerprint = COALESCE(request_fingerprint, encode(
        public.digest(
            (id::text
            || '|'
            || amount_vnd::text
            || '|'
            || to_char(
                created_at AT TIME ZONE 'UTC',
                'YYYY-MM-DD"T"HH24:MI:SS.US"Z"'
            ))::bytea,
            'sha256'::text
        ),
        'hex'
    ));

ALTER TABLE invoices
    ALTER COLUMN idempotency_key SET NOT NULL,
    ALTER COLUMN request_fingerprint SET NOT NULL,
    ALTER COLUMN payos_order_code SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_invoices_student_idempotency') THEN
        ALTER TABLE invoices ADD CONSTRAINT uq_invoices_student_idempotency UNIQUE (student_id, idempotency_key);
    END IF;
END $$;

CREATE OR REPLACE FUNCTION pg_temp.assert_payment_predicate(
    target_table regclass,
    target_constraint text,
    invalid_predicate text
) RETURNS void LANGUAGE plpgsql AS $$
DECLARE
    invalid_count bigint;
BEGIN
    EXECUTE format('SELECT count(*) FROM %s WHERE %s', target_table, invalid_predicate)
        INTO invalid_count;
    IF invalid_count > 0 THEN
        RAISE EXCEPTION 'table=%, constraint=%, invalid_rows=%',
            target_table::text, target_constraint, invalid_count;
    END IF;
END $$;

SELECT pg_temp.assert_payment_predicate(
    'invoices', 'ck_invoices_amount_positive', 'amount_vnd <= 0');
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_invoices_amount_positive') THEN
        ALTER TABLE invoices ADD CONSTRAINT ck_invoices_amount_positive CHECK (amount_vnd > 0);
    END IF;
END $$;

SELECT pg_temp.assert_payment_predicate(
    'payment_transactions', 'ck_payment_transactions_amount_positive', 'amount_vnd <= 0');
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_payment_transactions_amount_positive') THEN
        ALTER TABLE payment_transactions ADD CONSTRAINT ck_payment_transactions_amount_positive CHECK (amount_vnd > 0);
    END IF;
END $$;

SELECT pg_temp.assert_payment_predicate(
    'student_packages', 'ck_student_packages_total_positive', 'total_sessions <= 0');
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_student_packages_total_positive') THEN
        ALTER TABLE student_packages ADD CONSTRAINT ck_student_packages_total_positive CHECK (total_sessions > 0);
    END IF;
END $$;

SELECT pg_temp.assert_payment_predicate(
    'student_packages', 'ck_student_packages_price_positive', 'purchase_price_vnd <= 0');
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_student_packages_price_positive') THEN
        ALTER TABLE student_packages ADD CONSTRAINT ck_student_packages_price_positive CHECK (purchase_price_vnd > 0);
    END IF;
END $$;

SELECT pg_temp.assert_payment_predicate(
    'student_packages', 'ck_student_packages_commission_range',
    'commission_rate < 0 OR commission_rate > 100');
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_student_packages_commission_range') THEN
        ALTER TABLE student_packages ADD CONSTRAINT ck_student_packages_commission_range CHECK (commission_rate BETWEEN 0 AND 100);
    END IF;
END $$;

SELECT pg_temp.assert_payment_predicate(
    'ledger_entries', 'ck_ledger_entries_balance_bucket',
    $predicate$balance_bucket NOT IN ('PENDING','AVAILABLE','RESERVED')$predicate$);
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_ledger_entries_balance_bucket') THEN
        ALTER TABLE ledger_entries ADD CONSTRAINT ck_ledger_entries_balance_bucket CHECK (balance_bucket IN ('PENDING','AVAILABLE','RESERVED'));
    END IF;
END $$;

SELECT pg_temp.assert_payment_predicate(
    'ledger_entries', 'ck_ledger_entries_entry_type',
    $predicate$entry_type NOT IN (
        'PACKAGE_FUNDED',
        'SESSION_RELEASE_PENDING',
        'SESSION_CREDIT_AVAILABLE',
        'COMMISSION_RECOGNIZED',
        'PAYOUT_RESERVED',
        'PAYOUT_SUCCEEDED',
        'PAYOUT_RELEASED',
        'REFUND_DEBIT_PENDING',
        'ADJUSTMENT'
    )$predicate$);
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_ledger_entries_entry_type') THEN
        ALTER TABLE ledger_entries ADD CONSTRAINT ck_ledger_entries_entry_type CHECK (entry_type IN (
            'PACKAGE_FUNDED',
            'SESSION_RELEASE_PENDING',
            'SESSION_CREDIT_AVAILABLE',
            'COMMISSION_RECOGNIZED',
            'PAYOUT_RESERVED',
            'PAYOUT_SUCCEEDED',
            'PAYOUT_RELEASED',
            'REFUND_DEBIT_PENDING',
            'ADJUSTMENT'
        ));
    END IF;
END $$;
