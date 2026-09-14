-- =====================================================================
-- V34: ENABLE ROW LEVEL SECURITY & REVOKE TRUNCATE ON FINANCE TABLES
-- =====================================================================

-- 1. Bật Row Level Security default-deny cho 8 bảng Finance & Payments
ALTER TABLE wallets ENABLE ROW LEVEL SECURITY;
ALTER TABLE ledger_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE invoices ENABLE ROW LEVEL SECURITY;
ALTER TABLE payment_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE payout_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE refund_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE teacher_bank_accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE finance_command_receipts ENABLE ROW LEVEL SECURITY;

-- 2. Triệt tiêu nguy cơ RLS bypass qua lệnh TRUNCATE đối với unprivileged roles
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
        REVOKE TRUNCATE ON TABLE
            wallets,
            ledger_entries,
            invoices,
            payment_transactions,
            payout_requests,
            refund_requests,
            teacher_bank_accounts,
            finance_command_receipts
        FROM anon;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
        REVOKE TRUNCATE ON TABLE
            wallets,
            ledger_entries,
            invoices,
            payment_transactions,
            payout_requests,
            refund_requests,
            teacher_bank_accounts,
            finance_command_receipts
        FROM authenticated;
    END IF;
END $$;
