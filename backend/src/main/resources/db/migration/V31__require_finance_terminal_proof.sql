-- V31: terminal finance records must contain server-owned transfer evidence.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM payout_requests
               WHERE status = 'SUCCEEDED'
                 AND (bank_reference IS NULL OR btrim(bank_reference) = ''
                   OR transferred_at IS NULL OR proof_public_id IS NULL OR btrim(proof_public_id) = ''
                   OR proof_url IS NULL OR btrim(proof_url) = '')) THEN
        RAISE EXCEPTION 'V31 preflight failed: succeeded payout missing transfer proof';
    END IF;
    IF EXISTS (SELECT 1 FROM refund_requests
               WHERE status = 'REFUNDED'
                 AND (bank_reference IS NULL OR btrim(bank_reference) = ''
                   OR transferred_at IS NULL OR proof_public_id IS NULL OR btrim(proof_public_id) = ''
                   OR proof_url IS NULL OR btrim(proof_url) = '')) THEN
        RAISE EXCEPTION 'V31 preflight failed: refunded request missing transfer proof';
    END IF;
END $$;

ALTER TABLE payout_requests
    ADD CONSTRAINT ck_payout_succeeded_transfer_proof
    CHECK (status <> 'SUCCEEDED' OR
           (bank_reference IS NOT NULL AND btrim(bank_reference) <> ''
            AND transferred_at IS NOT NULL
            AND proof_public_id IS NOT NULL AND btrim(proof_public_id) <> ''
            AND proof_url IS NOT NULL AND btrim(proof_url) <> ''));

ALTER TABLE refund_requests
    ADD CONSTRAINT ck_refund_refunded_transfer_proof
    CHECK (status <> 'REFUNDED' OR
           (bank_reference IS NOT NULL AND btrim(bank_reference) <> ''
            AND transferred_at IS NOT NULL
            AND proof_public_id IS NOT NULL AND btrim(proof_public_id) <> ''
            AND proof_url IS NOT NULL AND btrim(proof_url) <> ''));
