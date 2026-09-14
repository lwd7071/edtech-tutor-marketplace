package com.edtech.platform.migration;

import com.edtech.platform.common.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RlsBehaviorVerificationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void restoreRlsState() {
        jdbcTemplate.update("DELETE FROM public.subjects WHERE code LIKE 'RLS_TEST_%'");
    }

    @Test
    @DisplayName("Step 2: Enable RLS on subjects - Backend CRUD succeeds unimpeded while unprivileged role is blocked")
    void verifyRlsBehaviorOnSubjects() throws Exception {
        // Verify RLS is enabled on subjects
        Map<String, Object> rlsStatus = jdbcTemplate.queryForMap(
                "SELECT rowsecurity FROM pg_tables WHERE schemaname = 'public' AND tablename = 'subjects'");
        assertThat(rlsStatus.get("rowsecurity")).isEqualTo(true);

        // 2. Test Backend CRUD operations via default connection (table owner / admin)
        String testCode = "RLS_TEST_" + UUID.randomUUID().toString().substring(0, 8);
        String testSlug = "rls-test-" + UUID.randomUUID().toString().substring(0, 8);

        // CREATE
        int inserted = jdbcTemplate.update(
                "INSERT INTO public.subjects (name, code, slug, education_level, is_active) VALUES (?, ?, ?, 'HIGH_SCHOOL', true)",
                "RLS Test Subject", testCode, testSlug
        );
        assertThat(inserted).isEqualTo(1);

        // READ
        Map<String, Object> subject = jdbcTemplate.queryForMap(
                "SELECT name, code, is_active FROM public.subjects WHERE code = ?", testCode);
        assertThat(subject.get("name")).isEqualTo("RLS Test Subject");
        assertThat(subject.get("is_active")).isEqualTo(true);

        // UPDATE
        int updated = jdbcTemplate.update(
                "UPDATE public.subjects SET name = ? WHERE code = ?",
                "RLS Test Subject Updated", testCode
        );
        assertThat(updated).isEqualTo(1);

        Map<String, Object> updatedSubject = jdbcTemplate.queryForMap(
                "SELECT name FROM public.subjects WHERE code = ?", testCode);
        assertThat(updatedSubject.get("name")).isEqualTo("RLS Test Subject Updated");

        // 3. Simulate an unprivileged role without BYPASSRLS (simulating PostgREST 'anon' role)
        String testAnonUser = "test_anon_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String testAnonPass = "P@ss_" + UUID.randomUUID().toString().substring(0, 8);

        jdbcTemplate.execute("CREATE ROLE " + testAnonUser + " WITH LOGIN PASSWORD '" + testAnonPass + "' NOINHERIT;");
        jdbcTemplate.execute("GRANT USAGE ON SCHEMA public TO " + testAnonUser + ";");
        jdbcTemplate.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON public.subjects TO " + testAnonUser + ";");

        try {
            // Connect as test_anon_user
            String jdbcUrl = POSTGRES_CONTAINER.getJdbcUrl();
            try (Connection anonConn = DriverManager.getConnection(jdbcUrl, testAnonUser, testAnonPass);
                 Statement anonStmt = anonConn.createStatement()) {

                // Query SELECT: RLS without policy should return 0 rows for unprivileged user
                try (ResultSet rs = anonStmt.executeQuery("SELECT COUNT(*) FROM public.subjects WHERE code = '" + testCode + "'")) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).as("Unprivileged role must see 0 rows due to RLS default-deny").isEqualTo(0);
                }

                // Query INSERT: RLS without INSERT policy should fail with error
                assertThatThrownBy(() -> anonStmt.executeUpdate(
                        "INSERT INTO public.subjects (name, code, slug, education_level, is_active) " +
                        "VALUES ('Hacked', 'HACK1', 'hack-1', 'HIGH_SCHOOL', true)"
                )).as("Unprivileged role must be blocked from INSERT by RLS")
                  .hasMessageContaining("violates row-level security policy");

                // Query UPDATE: 0 rows matched due to default-deny SELECT policy
                int anonUpdated = anonStmt.executeUpdate(
                        "UPDATE public.subjects SET name = 'Hacked' WHERE code = '" + testCode + "'");
                assertThat(anonUpdated).as("Unprivileged role must affect 0 rows on UPDATE due to RLS default-deny").isEqualTo(0);

                // Query DELETE: 0 rows matched due to default-deny SELECT policy
                int anonDeleted = anonStmt.executeUpdate(
                        "DELETE FROM public.subjects WHERE code = '" + testCode + "'");
                assertThat(anonDeleted).as("Unprivileged role must affect 0 rows on DELETE due to RLS default-deny").isEqualTo(0);
            }
        } finally {
            // Clean up test role
            jdbcTemplate.execute("DROP OWNED BY " + testAnonUser + ";");
            jdbcTemplate.execute("DROP ROLE " + testAnonUser + ";");
        }

        // 4. Backend DELETE (verifying cleanup by Backend owner works)
        int deleted = jdbcTemplate.update("DELETE FROM public.subjects WHERE code = ?", testCode);
        assertThat(deleted).isEqualTo(1);
    }

    @Test
    @DisplayName("Step 2: flyway_schema_history - Backend can still query and update")
    void verifyRlsBehaviorOnFlywaySchemaHistory() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM public.flyway_schema_history", Integer.class);
        assertThat(count).isNotNull().isGreaterThan(0);
    }

    @Test
    @DisplayName("V33: Enable RLS on users - Backend CRUD succeeds unimpeded while unprivileged role is blocked")
    void verifyRlsBehaviorOnUsers() throws Exception {
        // Verify RLS is enabled on users
        Map<String, Object> rlsStatus = jdbcTemplate.queryForMap(
                "SELECT rowsecurity FROM pg_tables WHERE schemaname = 'public' AND tablename = 'users'");
        assertThat(rlsStatus.get("rowsecurity")).isEqualTo(true);

        // 1. Backend CRUD operations via default connection (table owner / admin)
        String testEmail = "rls_user_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";

        int inserted = jdbcTemplate.update(
                "INSERT INTO public.users (email, password_hash, full_name, role, status) VALUES (?, 'hash123', 'RLS Test User', 'STUDENT', 'ACTIVE')",
                testEmail
        );
        assertThat(inserted).isEqualTo(1);

        Map<String, Object> user = jdbcTemplate.queryForMap(
                "SELECT email, full_name, role FROM public.users WHERE email = ?", testEmail);
        assertThat(user.get("full_name")).isEqualTo("RLS Test User");

        int updated = jdbcTemplate.update(
                "UPDATE public.users SET full_name = 'RLS Test User Updated' WHERE email = ?", testEmail);
        assertThat(updated).isEqualTo(1);

        // 2. Simulate unprivileged role without BYPASSRLS
        String testAnonUser = "test_anon_u_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String testAnonPass = "P@ss_" + UUID.randomUUID().toString().substring(0, 8);

        jdbcTemplate.execute("CREATE ROLE " + testAnonUser + " WITH LOGIN PASSWORD '" + testAnonPass + "' NOINHERIT;");
        jdbcTemplate.execute("GRANT USAGE ON SCHEMA public TO " + testAnonUser + ";");
        jdbcTemplate.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON public.users TO " + testAnonUser + ";");

        try {
            String jdbcUrl = POSTGRES_CONTAINER.getJdbcUrl();
            try (Connection anonConn = DriverManager.getConnection(jdbcUrl, testAnonUser, testAnonPass);
                 Statement anonStmt = anonConn.createStatement()) {

                // SELECT: RLS default-deny should return 0 rows
                try (ResultSet rs = anonStmt.executeQuery("SELECT COUNT(*) FROM public.users WHERE email = '" + testEmail + "'")) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).as("Unprivileged role must see 0 rows on users due to RLS default-deny").isEqualTo(0);
                }

                // INSERT: RLS default-deny should fail
                assertThatThrownBy(() -> anonStmt.executeUpdate(
                        "INSERT INTO public.users (email, password_hash, full_name, role, status) " +
                        "VALUES ('hacker@test.com', 'hack', 'Hacker', 'STUDENT', 'ACTIVE')"
                )).as("Unprivileged role must be blocked from INSERT by RLS")
                  .hasMessageContaining("violates row-level security policy");

                // UPDATE: 0 rows matched
                int anonUpdated = anonStmt.executeUpdate(
                        "UPDATE public.users SET full_name = 'Hacked' WHERE email = '" + testEmail + "'");
                assertThat(anonUpdated).as("Unprivileged role must affect 0 rows on UPDATE due to RLS default-deny").isEqualTo(0);

                // DELETE: 0 rows matched
                int anonDeleted = anonStmt.executeUpdate(
                        "DELETE FROM public.users WHERE email = '" + testEmail + "'");
                assertThat(anonDeleted).as("Unprivileged role must affect 0 rows on DELETE due to RLS default-deny").isEqualTo(0);
            }
        } finally {
            jdbcTemplate.execute("DROP OWNED BY " + testAnonUser + ";");
            jdbcTemplate.execute("DROP ROLE " + testAnonUser + ";");
        }

        // Cleanup
        int deleted = jdbcTemplate.update("DELETE FROM public.users WHERE email = ?", testEmail);
        assertThat(deleted).isEqualTo(1);
    }

    @Test
    @DisplayName("V34: Enable RLS on Finance tables - Backend CRUD succeeds unimpeded while unprivileged role is blocked")
    void verifyRlsBehaviorOnFinanceTables() throws Exception {
        // 1. Verify RLS is enabled on all 8 finance tables
        List<String> financeTables = List.of(
                "wallets", "ledger_entries", "invoices", "payment_transactions",
                "payout_requests", "refund_requests", "teacher_bank_accounts", "finance_command_receipts"
        );
        for (String table : financeTables) {
            Boolean rls = jdbcTemplate.queryForObject(
                    "SELECT rowsecurity FROM pg_tables WHERE schemaname = 'public' AND tablename = ?",
                    Boolean.class, table);
            assertThat(rls).as("Table " + table + " must have RLS enabled").isTrue();
        }

        // 2. Backend CRUD on wallets
        UUID teacherUserId = UUID.randomUUID();
        UUID teacherProfileId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        String teacherEmail = "teacher_fin_" + UUID.randomUUID().toString().substring(0, 8) + "@edtech.test";

        try {
            jdbcTemplate.update("""
                    INSERT INTO public.users (id, email, password_hash, full_name, role, status)
                    VALUES (?, ?, 'hash123', 'Finance Teacher', 'TEACHER', 'ACTIVE')
                    """, teacherUserId, teacherEmail);

            jdbcTemplate.update("""
                    INSERT INTO public.teacher_profiles (id, user_id, bio, years_of_experience, profile_status, verified_badge, is_visible)
                    VALUES (?, ?, 'Bio', 5, 'APPROVED', true, true)
                    """, teacherProfileId, teacherUserId);

            int walletInserted = jdbcTemplate.update("""
                    INSERT INTO public.wallets (id, teacher_id, pending_balance_vnd, available_balance_vnd, reserved_balance_vnd)
                    VALUES (?, ?, 0, 500000, 0)
                    """, walletId, teacherProfileId);
            assertThat(walletInserted).isEqualTo(1);

            UUID bankAccountId = UUID.randomUUID();
            jdbcTemplate.update("""
                    INSERT INTO public.teacher_bank_accounts (id, teacher_id, bank_bin, bank_name, account_number_encrypted, account_holder_name, is_verified, version)
                    VALUES (?, ?, '970407', 'Techcombank', 'enc_123456', 'NGUYEN VAN A', true, 0)
                    """, bankAccountId, teacherProfileId);

            Map<String, Object> wallet = jdbcTemplate.queryForMap(
                    "SELECT available_balance_vnd FROM public.wallets WHERE id = ?", walletId);
            assertThat(wallet.get("available_balance_vnd")).isEqualTo(500000L);

            // 3. Simulate unprivileged role
            String testAnonUser = "test_anon_fin_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            String testAnonPass = "P@ss_" + UUID.randomUUID().toString().substring(0, 8);

            jdbcTemplate.execute("CREATE ROLE " + testAnonUser + " WITH LOGIN PASSWORD '" + testAnonPass + "' NOINHERIT;");
            jdbcTemplate.execute("GRANT USAGE ON SCHEMA public TO " + testAnonUser + ";");
            jdbcTemplate.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON public.wallets, public.teacher_bank_accounts TO " + testAnonUser + ";");

            try {
                String jdbcUrl = POSTGRES_CONTAINER.getJdbcUrl();
                try (Connection anonConn = DriverManager.getConnection(jdbcUrl, testAnonUser, testAnonPass);
                     Statement anonStmt = anonConn.createStatement()) {

                    // SELECT wallets: RLS default-deny should return 0 rows
                    try (ResultSet rs = anonStmt.executeQuery("SELECT COUNT(*) FROM public.wallets WHERE id = '" + walletId + "'")) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getInt(1)).as("Unprivileged role must see 0 rows on wallets due to RLS default-deny").isEqualTo(0);
                    }

                    // SELECT teacher_bank_accounts: RLS default-deny should return 0 rows
                    try (ResultSet rs = anonStmt.executeQuery("SELECT COUNT(*) FROM public.teacher_bank_accounts WHERE id = '" + bankAccountId + "'")) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getInt(1)).as("Unprivileged role must see 0 rows on teacher_bank_accounts due to RLS default-deny").isEqualTo(0);
                    }

                    // INSERT wallets: RLS default-deny should fail
                    assertThatThrownBy(() -> anonStmt.executeUpdate(
                            "INSERT INTO public.wallets (teacher_id, pending_balance_vnd, available_balance_vnd, reserved_balance_vnd) " +
                            "VALUES ('" + teacherProfileId + "', 0, 999999, 0)"
                    )).as("Unprivileged role must be blocked from INSERT wallets by RLS")
                      .hasMessageContaining("violates row-level security policy");

                    // INSERT teacher_bank_accounts: RLS default-deny should fail
                    assertThatThrownBy(() -> anonStmt.executeUpdate(
                            "INSERT INTO public.teacher_bank_accounts (teacher_id, bank_bin, bank_name, account_number_encrypted, account_holder_name) " +
                            "VALUES ('" + teacherProfileId + "', '970407', 'Hacked Bank', 'enc_hack', 'HACKER')"
                    )).as("Unprivileged role must be blocked from INSERT teacher_bank_accounts by RLS")
                      .hasMessageContaining("violates row-level security policy");

                    // TRUNCATE: Must fail with permission denied because TRUNCATE is not granted
                    assertThatThrownBy(() -> anonStmt.executeUpdate(
                            "TRUNCATE public.teacher_bank_accounts"
                    )).as("Unprivileged role must be denied from TRUNCATE")
                      .hasMessageContaining("permission denied");

                    // UPDATE: 0 rows matched
                    int anonUpdated = anonStmt.executeUpdate(
                            "UPDATE public.wallets SET available_balance_vnd = 999999 WHERE id = '" + walletId + "'");
                    assertThat(anonUpdated).as("Unprivileged role must affect 0 rows on UPDATE due to RLS default-deny").isEqualTo(0);

                    // DELETE: 0 rows matched
                    int anonDeleted = anonStmt.executeUpdate(
                            "DELETE FROM public.wallets WHERE id = '" + walletId + "'");
                    assertThat(anonDeleted).as("Unprivileged role must affect 0 rows on DELETE due to RLS default-deny").isEqualTo(0);
                }
            } finally {
                jdbcTemplate.execute("DROP OWNED BY " + testAnonUser + ";");
                jdbcTemplate.execute("DROP ROLE " + testAnonUser + ";");
            }

        } finally {
            jdbcTemplate.update("DELETE FROM public.teacher_bank_accounts WHERE teacher_id = ?", teacherProfileId);
            jdbcTemplate.update("DELETE FROM public.wallets WHERE id = ?", walletId);
            jdbcTemplate.update("DELETE FROM public.teacher_profiles WHERE id = ?", teacherProfileId);
            jdbcTemplate.update("DELETE FROM public.users WHERE id = ?", teacherUserId);
        }
    }

    @Test
    @DisplayName("V35: Enable RLS on Booking & Learning tables - Backend CRUD succeeds while unprivileged role is blocked")
    void verifyRlsBehaviorOnBookingAndLearningTables() throws Exception {
        // 1. Verify RLS status
        List<String> tables = List.of(
                "student_packages", "package_extension_requests", "trial_requests",
                "bookings", "session_reports", "reviews", "teacher_stats",
                "assignments", "submissions");
        for (String tbl : tables) {
            Map<String, Object> rlsStatus = jdbcTemplate.queryForMap(
                    "SELECT rowsecurity FROM pg_tables WHERE schemaname = 'public' AND tablename = ?", tbl);
            assertThat(rlsStatus.get("rowsecurity")).as("Table %s must have RLS enabled", tbl).isEqualTo(true);
        }

        // 2. Setup test data via backend connection
        UUID studentUserId = UUID.randomUUID();
        UUID teacherUserId = UUID.randomUUID();
        UUID teacherProfileId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        UUID studentPackageId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        String studentEmail = "rls_student_" + studentUserId.toString().substring(0, 8) + "@test.com";
        String teacherEmail = "rls_teacher_" + teacherUserId.toString().substring(0, 8) + "@test.com";
        String subjectCode = "RLS_SBJ_" + UUID.randomUUID().toString().substring(0, 6);
        String invoiceNumber = "INV_" + UUID.randomUUID().toString().substring(0, 8);
        UUID idempotencyKey = UUID.randomUUID();

        jdbcTemplate.update("INSERT INTO public.users (id, email, password_hash, full_name, role, status) VALUES (?, ?, 'hash', 'Student', 'STUDENT', 'ACTIVE')",
                studentUserId, studentEmail);
        jdbcTemplate.update("INSERT INTO public.users (id, email, password_hash, full_name, role, status) VALUES (?, ?, 'hash', 'Teacher', 'TEACHER', 'ACTIVE')",
                teacherUserId, teacherEmail);
        jdbcTemplate.update("INSERT INTO public.teacher_profiles (id, user_id, bio, years_of_experience, profile_status, verified_badge, is_visible) VALUES (?, ?, 'Bio', 5, 'APPROVED', true, true)",
                teacherProfileId, teacherUserId);
        jdbcTemplate.update("INSERT INTO public.subjects (id, name, code, slug, education_level, is_active) VALUES (?, 'RLS Subj', ?, ?, 'HIGH_SCHOOL', true)",
                subjectId, subjectCode, "slug-" + subjectCode.toLowerCase());
        jdbcTemplate.update("INSERT INTO public.pricing_packages (id, teacher_id, subject_id, name, total_sessions, duration_days, price_vnd, session_duration_minutes, status) VALUES (?, ?, ?, 'Pkg', 10, 30, 1000000, 60, 'ACTIVE')",
                packageId, teacherProfileId, subjectId);
        jdbcTemplate.update("INSERT INTO public.invoices (id, invoice_number, student_id, teacher_id, pricing_package_id, amount_vnd, status, payos_order_code, idempotency_key, request_fingerprint, subject_id_snapshot, package_name_snapshot, total_sessions_snapshot, duration_days_snapshot, session_duration_minutes_snapshot, commission_rate_snapshot, version) VALUES (?, ?, ?, ?, ?, 1000000, 'PAID', nextval('payos_order_code_seq'), ?, 'fp_rls_test', ?, 'Pkg', 10, 30, 60, 15.00, 0)",
                invoiceId, invoiceNumber, studentUserId, teacherProfileId, packageId, idempotencyKey, subjectId);
        jdbcTemplate.update("INSERT INTO public.student_packages (id, student_id, teacher_id, subject_id, pricing_package_id, invoice_id, package_name_snapshot, total_sessions, remaining_sessions, reserved_sessions, completed_sessions, refunded_sessions, purchase_price_vnd, commission_rate, starts_at, expires_at, status) VALUES (?, ?, ?, ?, ?, ?, 'Pkg', 10, 10, 0, 0, 0, 1000000, 0.1500, now(), now() + interval '30 days', 'ACTIVE')",
                studentPackageId, studentUserId, teacherProfileId, subjectId, packageId, invoiceId);
        jdbcTemplate.update("INSERT INTO public.bookings (id, student_id, teacher_id, student_package_id, subject_id, start_time, end_time, delivery_mode, status, is_trial) VALUES (?, ?, ?, ?, ?, now() + interval '1 day', now() + interval '1 day 1 hour', 'ONLINE', 'SCHEDULED', false)",
                bookingId, studentUserId, teacherProfileId, studentPackageId, subjectId);

        try {
            // 3. Verify Backend CRUD succeeds
            Map<String, Object> booking = jdbcTemplate.queryForMap(
                    "SELECT status FROM public.bookings WHERE id = ?", bookingId);
            assertThat(booking.get("status")).isEqualTo("SCHEDULED");

            // 4. Test unprivileged role behavior
            String testAnonUser = "test_anon_bkg_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            String testAnonPass = "P@ss_" + UUID.randomUUID().toString().substring(0, 8);

            jdbcTemplate.execute("CREATE ROLE " + testAnonUser + " WITH LOGIN PASSWORD '" + testAnonPass + "' NOINHERIT;");
            jdbcTemplate.execute("GRANT USAGE ON SCHEMA public TO " + testAnonUser + ";");
            jdbcTemplate.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON public.bookings, public.student_packages, public.reviews TO " + testAnonUser + ";");

            try {
                String jdbcUrl = POSTGRES_CONTAINER.getJdbcUrl();
                try (Connection anonConn = DriverManager.getConnection(jdbcUrl, testAnonUser, testAnonPass);
                     Statement anonStmt = anonConn.createStatement()) {

                    // SELECT bookings: RLS default-deny should return 0 rows
                    try (ResultSet rs = anonStmt.executeQuery("SELECT COUNT(*) FROM public.bookings WHERE id = '" + bookingId + "'")) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getInt(1)).as("Unprivileged role must see 0 rows on bookings due to RLS default-deny").isEqualTo(0);
                    }

                    // SELECT student_packages: RLS default-deny should return 0 rows
                    try (ResultSet rs = anonStmt.executeQuery("SELECT COUNT(*) FROM public.student_packages WHERE id = '" + studentPackageId + "'")) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getInt(1)).as("Unprivileged role must see 0 rows on student_packages due to RLS default-deny").isEqualTo(0);
                    }

                    // INSERT bookings: RLS default-deny should fail
                    assertThatThrownBy(() -> anonStmt.executeUpdate(
                            "INSERT INTO public.bookings (student_id, teacher_id, start_time, end_time, delivery_mode, status, is_trial) " +
                            "VALUES ('" + studentUserId + "', '" + teacherProfileId + "', now() + interval '2 days', now() + interval '2 days 1 hour', 'ONLINE', 'SCHEDULED', false)"
                    )).as("Unprivileged role must be blocked from INSERT bookings by RLS")
                      .hasMessageContaining("violates row-level security policy");

                    // TRUNCATE: Must fail with permission denied
                    assertThatThrownBy(() -> anonStmt.executeUpdate(
                            "TRUNCATE public.bookings"
                    )).as("Unprivileged role must be denied from TRUNCATE")
                      .hasMessageContaining("permission denied");

                    // UPDATE: 0 rows affected
                    int anonUpdated = anonStmt.executeUpdate(
                            "UPDATE public.bookings SET status = 'CANCELLED' WHERE id = '" + bookingId + "'");
                    assertThat(anonUpdated).as("Unprivileged role must affect 0 rows on UPDATE due to RLS default-deny").isEqualTo(0);

                    // DELETE: 0 rows affected
                    int anonDeleted = anonStmt.executeUpdate(
                            "DELETE FROM public.bookings WHERE id = '" + bookingId + "'");
                    assertThat(anonDeleted).as("Unprivileged role must affect 0 rows on DELETE due to RLS default-deny").isEqualTo(0);
                }
            } finally {
                jdbcTemplate.execute("DROP OWNED BY " + testAnonUser + ";");
                jdbcTemplate.execute("DROP ROLE " + testAnonUser + ";");
            }

        } finally {
            jdbcTemplate.update("DELETE FROM public.bookings WHERE id = ?", bookingId);
            jdbcTemplate.update("DELETE FROM public.student_packages WHERE id = ?", studentPackageId);
            jdbcTemplate.update("DELETE FROM public.invoices WHERE id = ?", invoiceId);
            jdbcTemplate.update("DELETE FROM public.pricing_packages WHERE id = ?", packageId);
            jdbcTemplate.update("DELETE FROM public.subjects WHERE id = ?", subjectId);
            jdbcTemplate.update("DELETE FROM public.teacher_profiles WHERE id = ?", teacherProfileId);
            jdbcTemplate.update("DELETE FROM public.users WHERE id IN (?, ?)", studentUserId, teacherUserId);
        }
    }

    @Test
    @DisplayName("V36: Enable RLS on communication tables - Backend succeeds unimpeded while unprivileged role is blocked")
    void verifyRlsBehaviorOnCommunicationTables() throws Exception {
        // 1. Verify rowsecurity = true on all 4 communication tables
        List<String> rlsTables = jdbcTemplate.queryForList(
                "SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename IN ('conversations', 'messages', 'attachments', 'notifications') AND rowsecurity = true",
                String.class
        );
        assertThat(rlsTables).containsExactlyInAnyOrder("conversations", "messages", "attachments", "notifications");

        // 2. Setup test data via backend connection
        UUID studentUserId = UUID.randomUUID();
        UUID teacherUserId = UUID.randomUUID();
        UUID teacherProfileId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID clientMsgId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO public.users (id, email, password_hash, full_name, role, status) VALUES (?, ?, 'hash', 'Comm Student', 'STUDENT', 'ACTIVE')",
                studentUserId, "comm_std_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com"
        );
        jdbcTemplate.update(
                "INSERT INTO public.users (id, email, password_hash, full_name, role, status) VALUES (?, ?, 'hash', 'Comm Teacher', 'TEACHER', 'ACTIVE')",
                teacherUserId, "comm_tch_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com"
        );
        jdbcTemplate.update(
                "INSERT INTO public.teacher_profiles (id, user_id, bio, years_of_experience, profile_status, verified_badge, is_visible) VALUES (?, ?, 'Bio', 5, 'APPROVED', true, true)",
                teacherProfileId, teacherUserId
        );

        // Backend creates attachment metadata
        jdbcTemplate.update(
                "INSERT INTO public.attachments (id, owner_id, attachable_type, attachable_id, secure_url, original_filename, mime_type, file_size) " +
                "VALUES (?, ?, 'MESSAGE', ?, 'https://res.cloudinary.com/test/raw/upload/file.pdf', 'file.pdf', 'application/pdf', 1024)",
                attachmentId, studentUserId, messageId
        );

        // Backend creates conversation
        jdbcTemplate.update(
                "INSERT INTO public.conversations (id, teacher_id, student_id, last_message_at) VALUES (?, ?, ?, now())",
                conversationId, teacherProfileId, studentUserId
        );

        // Backend sends message
        jdbcTemplate.update(
                "INSERT INTO public.messages (id, conversation_id, sender_id, client_message_id, message_type, content, attachment_id) " +
                "VALUES (?, ?, ?, ?, 'FILE', 'Xin chao thay', ?)",
                messageId, conversationId, studentUserId, clientMsgId, attachmentId
        );

        // Backend creates notification
        jdbcTemplate.update(
                "INSERT INTO public.notifications (id, user_id, type, title, content, reference_type, reference_id, is_read) " +
                "VALUES (?, ?, 'CHAT_MESSAGE', 'Tin nhan moi', 'Ban co tin nhan moi', 'CONVERSATION', ?, false)",
                notificationId, teacherUserId, conversationId
        );

        // Verify backend can query all 4 tables
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM public.conversations WHERE id = ?", Long.class, conversationId)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM public.messages WHERE id = ?", Long.class, messageId)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM public.attachments WHERE id = ?", Long.class, attachmentId)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM public.notifications WHERE id = ?", Long.class, notificationId)).isEqualTo(1L);

        // 3. Test unprivileged role behavior
        String testAnonUser = "test_anon_comm_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String testAnonPass = "P@ss_" + UUID.randomUUID().toString().substring(0, 8);

        jdbcTemplate.execute("CREATE ROLE " + testAnonUser + " WITH LOGIN PASSWORD '" + testAnonPass + "' NOINHERIT;");
        jdbcTemplate.execute("GRANT USAGE ON SCHEMA public TO " + testAnonUser + ";");
        jdbcTemplate.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON public.conversations, public.messages, public.attachments, public.notifications TO " + testAnonUser + ";");

        try {
            String jdbcUrl = POSTGRES_CONTAINER.getJdbcUrl();
            try (Connection anonConn = DriverManager.getConnection(jdbcUrl, testAnonUser, testAnonPass);
                 Statement anonStmt = anonConn.createStatement()) {

                // SELECT conversations: RLS default-deny should return 0 rows
                try (ResultSet rs = anonStmt.executeQuery("SELECT COUNT(*) FROM public.conversations WHERE id = '" + conversationId + "'")) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).as("Unprivileged role must see 0 rows on conversations").isEqualTo(0);
                }

                // SELECT messages: RLS default-deny should return 0 rows
                try (ResultSet rs = anonStmt.executeQuery("SELECT COUNT(*) FROM public.messages WHERE id = '" + messageId + "'")) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).as("Unprivileged role must see 0 rows on messages").isEqualTo(0);
                }

                // SELECT notifications: RLS default-deny should return 0 rows
                try (ResultSet rs = anonStmt.executeQuery("SELECT COUNT(*) FROM public.notifications WHERE id = '" + notificationId + "'")) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).as("Unprivileged role must see 0 rows on notifications").isEqualTo(0);
                }

                // INSERT messages: RLS default-deny should fail
                UUID maliciousMsgId = UUID.randomUUID();
                assertThatThrownBy(() -> anonStmt.executeUpdate(
                        "INSERT INTO public.messages (id, conversation_id, sender_id, client_message_id, message_type, content) " +
                        "VALUES ('" + maliciousMsgId + "', '" + conversationId + "', '" + studentUserId + "', gen_random_uuid(), 'TEXT', 'Hacked')"
                )).as("Unprivileged role must be blocked from INSERT messages by RLS")
                  .hasMessageContaining("violates row-level security policy");

                // TRUNCATE conversations: Must fail with permission denied (REVOKE TRUNCATE)
                assertThatThrownBy(() -> anonStmt.executeUpdate(
                        "TRUNCATE public.conversations"
                )).as("Unprivileged role must be denied from TRUNCATE conversations")
                  .hasMessageContaining("permission denied");

                // UPDATE conversations: 0 rows affected
                int anonUpdated = anonStmt.executeUpdate(
                        "UPDATE public.conversations SET is_deleted = true WHERE id = '" + conversationId + "'");
                assertThat(anonUpdated).as("Unprivileged role must affect 0 rows on UPDATE conversations").isEqualTo(0);

                // DELETE messages: 0 rows affected
                int anonDeleted = anonStmt.executeUpdate(
                        "DELETE FROM public.messages WHERE id = '" + messageId + "'");
                assertThat(anonDeleted).as("Unprivileged role must affect 0 rows on DELETE messages").isEqualTo(0);
            }
        } finally {
            jdbcTemplate.execute("DROP OWNED BY " + testAnonUser + ";");
            jdbcTemplate.execute("DROP ROLE " + testAnonUser + ";");

            // Clean up test data
            jdbcTemplate.update("DELETE FROM public.notifications WHERE id = ?", notificationId);
            jdbcTemplate.update("DELETE FROM public.messages WHERE id = ?", messageId);
            jdbcTemplate.update("DELETE FROM public.attachments WHERE id = ?", attachmentId);
            jdbcTemplate.update("DELETE FROM public.conversations WHERE id = ?", conversationId);
            jdbcTemplate.update("DELETE FROM public.teacher_profiles WHERE id = ?", teacherProfileId);
            jdbcTemplate.update("DELETE FROM public.users WHERE id IN (?, ?)", studentUserId, teacherUserId);
        }
    }

    @Test
    @DisplayName("V37: Enable RLS on system tables - Backend succeeds unimpeded while unprivileged role is blocked")
    void verifyRlsBehaviorOnSystemTables() throws Exception {
        // 1. Verify rowsecurity = true on all 3 system tables
        List<String> rlsTables = jdbcTemplate.queryForList(
                "SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename IN ('platform_settings', 'audit_logs', 'email_outbox') AND rowsecurity = true",
                String.class
        );
        assertThat(rlsTables).containsExactlyInAnyOrder("platform_settings", "audit_logs", "email_outbox");

        // 2. Setup test data via backend connection
        UUID auditId = UUID.randomUUID();
        UUID emailId = UUID.randomUUID();
        UUID testUserId = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO public.users (id, email, password_hash, full_name, role, status) VALUES (?, ?, 'hash', 'Admin Sys', 'ADMIN', 'ACTIVE')",
                testUserId, "sys_adm_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com"
        );

        // Platform settings is a singleton table: use existing row or create if absent
        List<UUID> existingSettingIds = jdbcTemplate.queryForList("SELECT id FROM public.platform_settings LIMIT 1", UUID.class);
        UUID settingId;
        if (existingSettingIds.isEmpty()) {
            settingId = UUID.randomUUID();
            jdbcTemplate.update(
                    "INSERT INTO public.platform_settings (id, commission_rate, bayesian_minimum_reviews, booking_reminder_hours, booking_expiration_hours) " +
                    "VALUES (?, 15.00, 5, 24, 48)",
                    settingId
            );
        } else {
            settingId = existingSettingIds.get(0);
        }

        // Backend creates audit_log
        jdbcTemplate.update(
                "INSERT INTO public.audit_logs (id, actor_id, action, target_type, target_id) VALUES (?, ?, 'UPDATE_SETTINGS', 'PLATFORM_SETTINGS', ?)",
                auditId, testUserId, settingId
        );

        // Backend creates email_outbox
        jdbcTemplate.update(
                "INSERT INTO public.email_outbox (id, recipient, subject, body, status) VALUES (?, 'user@test.com', 'Welcome', 'Hello', 'PENDING')",
                emailId
        );

        // Verify backend can query all tables
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM public.platform_settings WHERE id = ?", Long.class, settingId)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM public.audit_logs WHERE id = ?", Long.class, auditId)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM public.email_outbox WHERE id = ?", Long.class, emailId)).isEqualTo(1L);

        // 3. Test unprivileged role behavior
        String testAnonUser = "test_anon_sys_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String testAnonPass = "P@ss_" + UUID.randomUUID().toString().substring(0, 8);

        jdbcTemplate.execute("CREATE ROLE " + testAnonUser + " WITH LOGIN PASSWORD '" + testAnonPass + "' NOINHERIT;");
        jdbcTemplate.execute("GRANT USAGE ON SCHEMA public TO " + testAnonUser + ";");
        // Grant DML to test RLS default-deny behavior
        jdbcTemplate.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON public.platform_settings, public.audit_logs, public.email_outbox TO " + testAnonUser + ";");

        try {
            String jdbcUrl = POSTGRES_CONTAINER.getJdbcUrl();
            try (Connection anonConn = DriverManager.getConnection(jdbcUrl, testAnonUser, testAnonPass);
                 Statement anonStmt = anonConn.createStatement()) {

                // SELECT platform_settings: RLS default-deny should return 0 rows
                try (ResultSet rs = anonStmt.executeQuery("SELECT COUNT(*) FROM public.platform_settings WHERE id = '" + settingId + "'")) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).as("Unprivileged role must see 0 rows on platform_settings").isEqualTo(0);
                }

                // SELECT audit_logs: RLS default-deny should return 0 rows
                try (ResultSet rs = anonStmt.executeQuery("SELECT COUNT(*) FROM public.audit_logs WHERE id = '" + auditId + "'")) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).as("Unprivileged role must see 0 rows on audit_logs").isEqualTo(0);
                }

                // SELECT email_outbox: RLS default-deny should return 0 rows
                try (ResultSet rs = anonStmt.executeQuery("SELECT COUNT(*) FROM public.email_outbox WHERE id = '" + emailId + "'")) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).as("Unprivileged role must see 0 rows on email_outbox").isEqualTo(0);
                }

                // INSERT platform_settings: RLS default-deny should fail
                UUID maliciousSettingId = UUID.randomUUID();
                assertThatThrownBy(() -> anonStmt.executeUpdate(
                        "INSERT INTO public.platform_settings (id, commission_rate) VALUES ('" + maliciousSettingId + "', 0.00)"
                )).as("Unprivileged role must be blocked from INSERT platform_settings by RLS")
                  .hasMessageContaining("violates row-level security policy");

                // TRUNCATE platform_settings: Must fail with permission denied (REVOKE TRUNCATE)
                assertThatThrownBy(() -> anonStmt.executeUpdate(
                        "TRUNCATE public.platform_settings"
                )).as("Unprivileged role must be denied from TRUNCATE platform_settings")
                  .hasMessageContaining("permission denied");

                // UPDATE platform_settings: 0 rows affected
                int anonUpdated = anonStmt.executeUpdate(
                        "UPDATE public.platform_settings SET commission_rate = 0.00 WHERE id = '" + settingId + "'");
                assertThat(anonUpdated).as("Unprivileged role must affect 0 rows on UPDATE platform_settings").isEqualTo(0);

                // DELETE audit_logs: 0 rows affected
                int anonDeleted = anonStmt.executeUpdate(
                        "DELETE FROM public.audit_logs WHERE id = '" + auditId + "'");
                assertThat(anonDeleted).as("Unprivileged role must affect 0 rows on DELETE audit_logs").isEqualTo(0);
            }
        } finally {
            jdbcTemplate.execute("DROP OWNED BY " + testAnonUser + ";");
            jdbcTemplate.execute("DROP ROLE " + testAnonUser + ";");

            // Clean up test data
            jdbcTemplate.update("DELETE FROM public.audit_logs WHERE id = ?", auditId);
            jdbcTemplate.update("DELETE FROM public.email_outbox WHERE id = ?", emailId);
            jdbcTemplate.update("DELETE FROM public.users WHERE id = ?", testUserId);
        }
    }
}
