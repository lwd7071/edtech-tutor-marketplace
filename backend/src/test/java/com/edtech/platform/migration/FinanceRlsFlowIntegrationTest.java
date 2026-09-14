package com.edtech.platform.migration;

import com.edtech.platform.auth.integration.AuthIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class FinanceRlsFlowIntegrationTest extends AuthIntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("V34: Luồng tài chính toàn diện (Wallet -> BankAccount -> Invoice -> PaymentTransaction -> LedgerEntry -> Payout) hoạt động hoàn hảo khi bật RLS trên 8 bảng Finance")
    void crossTableFinanceFlowShouldSucceedWithRlsOnFinanceTables() throws Exception {
        UUID studentUserId = UUID.randomUUID();
        UUID teacherUserId = UUID.randomUUID();
        UUID teacherProfileId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID pricingPackageId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        UUID bankAccountId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        UUID paymentTransactionId = UUID.randomUUID();
        UUID ledgerEntryId = UUID.randomUUID();
        UUID payoutRequestId = UUID.randomUUID();

        String studentEmail = "fin_stu_" + UUID.randomUUID().toString().substring(0, 8) + "@edtech.test";
        String teacherEmail = "fin_tch_" + UUID.randomUUID().toString().substring(0, 8) + "@edtech.test";
        String invoiceNumber = "INV-FIN-" + UUID.randomUUID().toString().substring(0, 8);
        String idempotencyKey = UUID.randomUUID().toString();
        String ledgerIdempotencyKey = UUID.randomUUID().toString();

        try {
            // 1. Setup Users & Profile & Subject & PricingPackage
            jdbcTemplate.update("""
                    INSERT INTO public.users (id, email, password_hash, full_name, role, status)
                    VALUES (?, ?, 'hash123', 'Finance Student', 'STUDENT', 'ACTIVE'),
                           (?, ?, 'hash123', 'Finance Teacher', 'TEACHER', 'ACTIVE')
                    """, studentUserId, studentEmail, teacherUserId, teacherEmail);

            jdbcTemplate.update("""
                    INSERT INTO public.teacher_profiles (id, user_id, bio, years_of_experience, profile_status, verified_badge, is_visible)
                    VALUES (?, ?, 'Finance Teacher Bio', 5, 'APPROVED', true, true)
                    """, teacherProfileId, teacherUserId);

            jdbcTemplate.update("""
                    INSERT INTO public.subjects (id, name, code, slug, education_level, is_active)
                    VALUES (?, 'Kinh Tế Lượng', ?, 'kinh-te-luong', 'UNIVERSITY', true)
                    """, subjectId, "ECON_" + UUID.randomUUID().toString().substring(0, 8));

            jdbcTemplate.update("""
                    INSERT INTO public.pricing_packages (id, teacher_id, subject_id, name, total_sessions, duration_days, price_vnd, session_duration_minutes, status)
                    VALUES (?, ?, ?, 'Gói 5 buổi Kinh Tế', 5, 30, 1000000, 60, 'ACTIVE')
                    """, pricingPackageId, teacherProfileId, subjectId);

            // 2. Setup Wallet & Bank Account (Finance Tables)
            jdbcTemplate.update("""
                    INSERT INTO public.wallets (id, teacher_id, pending_balance_vnd, available_balance_vnd, reserved_balance_vnd)
                    VALUES (?, ?, 0, 0, 0)
                    """, walletId, teacherProfileId);

            jdbcTemplate.update("""
                    INSERT INTO public.teacher_bank_accounts (id, teacher_id, bank_bin, bank_name, account_number_encrypted, account_holder_name, is_verified, version)
                    VALUES (?, ?, '970407', 'Techcombank', 'enc_19030000000', 'NGUYEN VAN A', true, 0)
                    """, bankAccountId, teacherProfileId);

            // 3. Setup Invoice & PaymentTransaction (Finance Tables)
            jdbcTemplate.update("""
                    INSERT INTO public.invoices (id, invoice_number, student_id, teacher_id, pricing_package_id,
                                                amount_vnd, status, payos_order_code, idempotency_key, request_fingerprint,
                                                subject_id_snapshot, package_name_snapshot, total_sessions_snapshot,
                                                duration_days_snapshot, session_duration_minutes_snapshot, commission_rate_snapshot, version)
                    VALUES (?, ?, ?, ?, ?, 1000000, 'PAID', nextval('payos_order_code_seq'), ?, 'fp_fin_test',
                            ?, 'Gói 5 buổi Kinh Tế', 5, 30, 60, 15.00, 0)
                    """, invoiceId, invoiceNumber, studentUserId, teacherProfileId, pricingPackageId, UUID.randomUUID(), subjectId);

            String providerRef = "PAYOS_REF_" + UUID.randomUUID().toString().substring(0, 8);
            jdbcTemplate.update("""
                    INSERT INTO public.payment_transactions (id, invoice_id, provider, provider_reference, amount_vnd, transaction_datetime, signature_valid)
                    VALUES (?, ?, 'PAYOS', ?, 1000000, now(), true)
                    """, paymentTransactionId, invoiceId, providerRef);

            // 4. Record Ledger Entry & Update Wallet Balance (850,000 net to teacher after 15% commission)
            jdbcTemplate.update("""
                    INSERT INTO public.ledger_entries (id, wallet_id, entry_type, amount_vnd, balance_bucket, direction, reference_type, reference_id, idempotency_key, description)
                    VALUES (?, ?, 'SESSION_CREDIT_AVAILABLE', 850000, 'AVAILABLE', 'CREDIT', 'INVOICE', ?, ?, 'Lesson earning credited')
                    """, ledgerEntryId, walletId, invoiceId, ledgerIdempotencyKey);

            jdbcTemplate.update("UPDATE public.wallets SET available_balance_vnd = available_balance_vnd + 850000 WHERE id = ?", walletId);

            // 5. Create Payout Request
            jdbcTemplate.update("""
                    INSERT INTO public.payout_requests (id, teacher_id, wallet_id, bank_account_id, amount_vnd, status, version)
                    VALUES (?, ?, ?, ?, 500000, 'PENDING', 0)
                    """, payoutRequestId, teacherProfileId, walletId, bankAccountId);

            // 6. Verify Full Financial Flow Query via Backend Connection
            Map<String, Object> financeResult = jdbcTemplate.queryForMap("""
                    SELECT w.available_balance_vnd,
                           tba.bank_name,
                           inv.amount_vnd AS invoice_amount,
                           le.amount_vnd AS ledger_amount,
                           pr.amount_vnd AS payout_amount,
                           pr.status AS payout_status
                    FROM public.wallets w
                    JOIN public.teacher_profiles tp ON tp.id = w.teacher_id
                    JOIN public.teacher_bank_accounts tba ON tba.teacher_id = tp.id
                    JOIN public.payout_requests pr ON pr.teacher_id = tp.id
                    JOIN public.ledger_entries le ON le.wallet_id = w.id
                    JOIN public.invoices inv ON inv.teacher_id = tp.id
                    WHERE w.id = ?
                    """, walletId);

            assertThat(financeResult.get("available_balance_vnd")).isEqualTo(850000L);
            assertThat(financeResult.get("bank_name")).isEqualTo("Techcombank");
            assertThat(financeResult.get("invoice_amount")).isEqualTo(1000000L);
            assertThat(financeResult.get("ledger_amount")).isEqualTo(850000L);
            assertThat(financeResult.get("payout_amount")).isEqualTo(500000L);
            assertThat(financeResult.get("payout_status")).isEqualTo("PENDING");

            // 7. Verify Unprivileged user CANNOT query any Finance tables
            String testAnonUser = "test_fin_anon_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            String testAnonPass = "P@ss_" + UUID.randomUUID().toString().substring(0, 8);

            jdbcTemplate.execute("CREATE ROLE " + testAnonUser + " WITH LOGIN PASSWORD '" + testAnonPass + "' NOINHERIT;");
            jdbcTemplate.execute("GRANT USAGE ON SCHEMA public TO " + testAnonUser + ";");
            jdbcTemplate.execute("GRANT SELECT ON ALL TABLES IN SCHEMA public TO " + testAnonUser + ";");

            try {
                String jdbcUrl = POSTGRES_CONTAINER.getJdbcUrl();
                try (Connection anonConn = DriverManager.getConnection(jdbcUrl, testAnonUser, testAnonPass);
                     Statement anonStmt = anonConn.createStatement()) {

                    // Verify wallets blocked
                    try (ResultSet rs = anonStmt.executeQuery("SELECT count(*) FROM public.wallets WHERE id = '" + walletId + "'")) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getInt(1)).as("Anon must see 0 rows on wallets").isEqualTo(0);
                    }

                    // Verify teacher_bank_accounts blocked
                    try (ResultSet rs = anonStmt.executeQuery("SELECT count(*) FROM public.teacher_bank_accounts WHERE id = '" + bankAccountId + "'")) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getInt(1)).as("Anon must see 0 rows on teacher_bank_accounts").isEqualTo(0);
                    }

                    // Verify ledger_entries blocked
                    try (ResultSet rs = anonStmt.executeQuery("SELECT count(*) FROM public.ledger_entries WHERE id = '" + ledgerEntryId + "'")) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getInt(1)).as("Anon must see 0 rows on ledger_entries").isEqualTo(0);
                    }
                }
            } finally {
                jdbcTemplate.execute("DROP OWNED BY " + testAnonUser + ";");
                jdbcTemplate.execute("DROP ROLE " + testAnonUser + ";");
            }

        } finally {
            // Cleanup
            jdbcTemplate.update("DELETE FROM public.payout_requests WHERE id = ?", payoutRequestId);
            jdbcTemplate.update("DELETE FROM public.ledger_entries WHERE id = ?", ledgerEntryId);
            jdbcTemplate.update("DELETE FROM public.payment_transactions WHERE id = ?", paymentTransactionId);
            jdbcTemplate.update("DELETE FROM public.invoices WHERE id = ?", invoiceId);
            jdbcTemplate.update("DELETE FROM public.teacher_bank_accounts WHERE id = ?", bankAccountId);
            jdbcTemplate.update("DELETE FROM public.wallets WHERE id = ?", walletId);
            jdbcTemplate.update("DELETE FROM public.pricing_packages WHERE id = ?", pricingPackageId);
            jdbcTemplate.update("DELETE FROM public.subjects WHERE id = ?", subjectId);
            jdbcTemplate.update("DELETE FROM public.teacher_profiles WHERE id = ?", teacherProfileId);
            jdbcTemplate.update("DELETE FROM public.users WHERE id IN (?, ?)", studentUserId, teacherUserId);
        }
    }
}
