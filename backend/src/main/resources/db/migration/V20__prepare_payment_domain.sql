-- =====================================================================
-- V20: Payment-ready domain, deterministic idempotency and constraints.
-- V1-V19 remain immutable; all changes are forward-only.
-- Pre-deploy gate: the target PostgreSQL role must be allowed to install
-- (or already have) pgcrypto.
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE SEQUENCE invoice_number_seq AS bigint START WITH 1 INCREMENT BY 1 NO CYCLE;
CREATE SEQUENCE payos_order_code_seq AS bigint START WITH 1 INCREMENT BY 1 NO CYCLE;
SELECT setval(
    'payos_order_code_seq',
    COALESCE((SELECT max(payos_order_code) FROM invoices), 0) + 1,
    false
);

ALTER TABLE invoices
    ADD COLUMN idempotency_key uuid,
    ADD COLUMN request_fingerprint varchar(64);

UPDATE invoices
SET idempotency_key = id,
    payos_order_code = COALESCE(payos_order_code, nextval('payos_order_code_seq')),
    request_fingerprint = encode(
        digest(
            id::text
            || '|'
            || amount_vnd::text
            || '|'
            || to_char(
                created_at AT TIME ZONE 'UTC',
                'YYYY-MM-DD"T"HH24:MI:SS.US"Z"'
            ),
            'sha256'
        ),
        'hex'
    );

ALTER TABLE invoices
    ALTER COLUMN idempotency_key SET NOT NULL,
    ALTER COLUMN request_fingerprint SET NOT NULL,
    ALTER COLUMN payos_order_code SET NOT NULL,
    ADD CONSTRAINT uq_invoices_student_idempotency UNIQUE (student_id, idempotency_key);

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
ALTER TABLE invoices ADD CONSTRAINT ck_invoices_amount_positive CHECK (amount_vnd > 0);

SELECT pg_temp.assert_payment_predicate(
    'payment_transactions', 'ck_payment_transactions_amount_positive', 'amount_vnd <= 0');
ALTER TABLE payment_transactions ADD CONSTRAINT ck_payment_transactions_amount_positive
    CHECK (amount_vnd > 0);

SELECT pg_temp.assert_payment_predicate(
    'student_packages', 'ck_student_packages_total_positive', 'total_sessions <= 0');
ALTER TABLE student_packages ADD CONSTRAINT ck_student_packages_total_positive
    CHECK (total_sessions > 0);

SELECT pg_temp.assert_payment_predicate(
    'student_packages', 'ck_student_packages_price_positive', 'purchase_price_vnd <= 0');
ALTER TABLE student_packages ADD CONSTRAINT ck_student_packages_price_positive
    CHECK (purchase_price_vnd > 0);

SELECT pg_temp.assert_payment_predicate(
    'student_packages', 'ck_student_packages_commission_range',
    'commission_rate < 0 OR commission_rate > 100');
ALTER TABLE student_packages ADD CONSTRAINT ck_student_packages_commission_range
    CHECK (commission_rate BETWEEN 0 AND 100);

SELECT pg_temp.assert_payment_predicate(
    'ledger_entries', 'ck_ledger_entries_balance_bucket',
    $predicate$balance_bucket NOT IN ('PENDING','AVAILABLE','RESERVED')$predicate$);
ALTER TABLE ledger_entries ADD CONSTRAINT ck_ledger_entries_balance_bucket
    CHECK (balance_bucket IN ('PENDING','AVAILABLE','RESERVED'));

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
