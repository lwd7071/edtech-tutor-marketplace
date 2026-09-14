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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class UserRlsCrossTableFlowIntegrationTest extends AuthIntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("V33: Luồng nghiệp vụ xuyên bảng (User -> Profile -> Package -> Booking -> Invoice) hoạt động hoàn hảo khi bật RLS trên users")
    void crossTableBusinessFlowShouldSucceedWithRlsOnUsers() throws Exception {
        // 1. Verify RLS is enabled on users
        Boolean usersRls = jdbcTemplate.queryForObject(
                "SELECT rowsecurity FROM pg_tables WHERE schemaname = 'public' AND tablename = 'users'", Boolean.class);
        assertThat(usersRls).isTrue();

        UUID studentUserId = UUID.randomUUID();
        UUID teacherUserId = UUID.randomUUID();
        UUID teacherProfileId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID pricingPackageId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        UUID studentPackageId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        String studentEmail = "student_" + UUID.randomUUID().toString().substring(0, 8) + "@edtech.test";
        String teacherEmail = "teacher_" + UUID.randomUUID().toString().substring(0, 8) + "@edtech.test";
        String subjectCode = "SUBJ_" + UUID.randomUUID().toString().substring(0, 8);
        String invoiceNumber = "INV-" + UUID.randomUUID().toString().substring(0, 8);
        UUID idempotencyKey = UUID.randomUUID();

        try {
            // A. Create Student & Teacher in public.users (table with RLS)
            jdbcTemplate.update("""
                    INSERT INTO public.users (id, email, password_hash, full_name, role, status)
                    VALUES (?, ?, 'hash_pwd_123', 'Student Test', 'STUDENT', 'ACTIVE'),
                           (?, ?, 'hash_pwd_123', 'Teacher Test', 'TEACHER', 'ACTIVE')
                    """, studentUserId, studentEmail, teacherUserId, teacherEmail);

            // B. Create Teacher Profile (table with RLS)
            jdbcTemplate.update("""
                    INSERT INTO public.teacher_profiles (id, user_id, bio, years_of_experience, profile_status, verified_badge, is_visible)
                    VALUES (?, ?, 'Experienced Math Teacher', 5, 'APPROVED', true, true)
                    """, teacherProfileId, teacherUserId);

            // C. Create Subject (table with RLS)
            jdbcTemplate.update("""
                    INSERT INTO public.subjects (id, name, code, slug, education_level, is_active)
                    VALUES (?, 'Toán Học Test', ?, 'toan-hoc-test', 'HIGH_SCHOOL', true)
                    """, subjectId, subjectCode);

            // D. Create Pricing Package (table with RLS)
            jdbcTemplate.update("""
                    INSERT INTO public.pricing_packages (id, teacher_id, subject_id, name, total_sessions, duration_days, price_vnd, session_duration_minutes, status)
                    VALUES (?, ?, ?, 'Gói 10 buổi Toán', 10, 30, 2000000, 60, 'ACTIVE')
                    """, pricingPackageId, teacherProfileId, subjectId);

            // E. Create Invoice
            jdbcTemplate.update("""
                    INSERT INTO public.invoices (id, invoice_number, student_id, teacher_id, pricing_package_id,
                                                amount_vnd, status, payos_order_code, idempotency_key, request_fingerprint,
                                                subject_id_snapshot, package_name_snapshot, total_sessions_snapshot,
                                                duration_days_snapshot, session_duration_minutes_snapshot, commission_rate_snapshot, version)
                    VALUES (?, ?, ?, ?, ?, 2000000, 'PAID', nextval('payos_order_code_seq'), ?, 'fp_cross_test',
                            ?, 'Gói 10 buổi Toán', 10, 30, 60, 15.00, 0)
                    """, invoiceId, invoiceNumber, studentUserId, teacherProfileId, pricingPackageId, idempotencyKey, subjectId);

            // F. Create Student Package
            Instant startsAt = Instant.now();
            Instant expiresAt = startsAt.plus(30, ChronoUnit.DAYS);
            jdbcTemplate.update("""
                    INSERT INTO public.student_packages (id, student_id, teacher_id, subject_id, pricing_package_id, invoice_id,
                                                        package_name_snapshot, total_sessions, remaining_sessions, reserved_sessions,
                                                        completed_sessions, refunded_sessions, purchase_price_vnd, commission_rate,
                                                        starts_at, expires_at, status)
                    VALUES (?, ?, ?, ?, ?, ?, 'Gói 10 buổi Toán', 10, 10, 0, 0, 0, 2000000, 0.1500, ?, ?, 'ACTIVE')
                    """, studentPackageId, studentUserId, teacherProfileId, subjectId, pricingPackageId, invoiceId,
                    java.sql.Timestamp.from(startsAt), java.sql.Timestamp.from(expiresAt));

            // G. Create Booking
            Instant startTime = startsAt.plus(1, ChronoUnit.DAYS);
            Instant endTime = startTime.plus(60, ChronoUnit.MINUTES);
            jdbcTemplate.update("""
                    INSERT INTO public.bookings (id, teacher_id, student_id, student_package_id, subject_id,
                                                start_time, end_time, delivery_mode, status, is_trial)
                    VALUES (?, ?, ?, ?, ?, ?, ?, 'ONLINE', 'SCHEDULED', false)
                    """, bookingId, teacherProfileId, studentUserId, studentPackageId, subjectId,
                    java.sql.Timestamp.from(startTime), java.sql.Timestamp.from(endTime));

            // 2. Perform Cross-Table JOIN query via Backend Connection (table owner / bypassrls)
            Map<String, Object> joinedResult = jdbcTemplate.queryForMap("""
                    SELECT u.email AS student_email,
                           tu.email AS teacher_email,
                           tp.years_of_experience,
                           b.status AS booking_status,
                           inv.amount_vnd AS invoice_amount,
                           inv.status AS invoice_status,
                           sp.package_name_snapshot
                    FROM public.users u
                    JOIN public.student_packages sp ON sp.student_id = u.id
                    JOIN public.teacher_profiles tp ON tp.id = sp.teacher_id
                    JOIN public.users tu ON tu.id = tp.user_id
                    JOIN public.bookings b ON b.student_id = u.id AND b.teacher_id = tp.id
                    JOIN public.invoices inv ON inv.student_id = u.id AND inv.id = sp.invoice_id
                    WHERE u.id = ?
                    """, studentUserId);

            assertThat(joinedResult.get("student_email")).isEqualTo(studentEmail);
            assertThat(joinedResult.get("teacher_email")).isEqualTo(teacherEmail);
            assertThat(joinedResult.get("years_of_experience")).isEqualTo(5);
            assertThat(joinedResult.get("invoice_amount")).isEqualTo(2000000L);
            assertThat(joinedResult.get("booking_status")).isEqualTo("SCHEDULED");
            assertThat(joinedResult.get("invoice_status")).isEqualTo("PAID");
            assertThat(joinedResult.get("package_name_snapshot")).isEqualTo("Gói 10 buổi Toán");

            // 3. Verify that an unprivileged role without BYPASSRLS CANNOT query users or perform cross-table join
            String testAnonUser = "test_cross_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            String testAnonPass = "P@ss_" + UUID.randomUUID().toString().substring(0, 8);

            jdbcTemplate.execute("CREATE ROLE " + testAnonUser + " WITH LOGIN PASSWORD '" + testAnonPass + "' NOINHERIT;");
            jdbcTemplate.execute("GRANT USAGE ON SCHEMA public TO " + testAnonUser + ";");
            jdbcTemplate.execute("GRANT SELECT ON ALL TABLES IN SCHEMA public TO " + testAnonUser + ";");

            try {
                String jdbcUrl = POSTGRES_CONTAINER.getJdbcUrl();
                try (Connection anonConn = DriverManager.getConnection(jdbcUrl, testAnonUser, testAnonPass);
                     Statement anonStmt = anonConn.createStatement()) {

                    // Query cross-table join as anon: Should return 0 rows because users has RLS default-deny
                    String anonCrossQuery = "SELECT count(*) FROM public.users u WHERE u.id = '" + studentUserId + "'";
                    try (ResultSet rs = anonStmt.executeQuery(anonCrossQuery)) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getInt(1)).as("Unprivileged user must see 0 rows on users due to RLS default-deny").isEqualTo(0);
                    }

                    // Query full join as anon: Should return 0 rows
                    String anonFullJoinQuery = """
                            SELECT count(*)
                            FROM public.users u
                            JOIN public.teacher_profiles tp ON tp.user_id = u.id
                            WHERE u.id = '""" + studentUserId + "'";
                    try (ResultSet rs = anonStmt.executeQuery(anonFullJoinQuery)) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getInt(1)).as("Unprivileged user must see 0 rows in cross-table join due to users RLS").isEqualTo(0);
                    }
                }
            } finally {
                jdbcTemplate.execute("DROP OWNED BY " + testAnonUser + ";");
                jdbcTemplate.execute("DROP ROLE " + testAnonUser + ";");
            }

        } finally {
            // Cleanup in reverse dependency order
            jdbcTemplate.update("DELETE FROM public.bookings WHERE id = ?", bookingId);
            jdbcTemplate.update("DELETE FROM public.student_packages WHERE id = ?", studentPackageId);
            jdbcTemplate.update("DELETE FROM public.invoices WHERE id = ?", invoiceId);
            jdbcTemplate.update("DELETE FROM public.pricing_packages WHERE id = ?", pricingPackageId);
            jdbcTemplate.update("DELETE FROM public.subjects WHERE id = ?", subjectId);
            jdbcTemplate.update("DELETE FROM public.teacher_profiles WHERE id = ?", teacherProfileId);
            jdbcTemplate.update("DELETE FROM public.users WHERE id IN (?, ?)", studentUserId, teacherUserId);
        }
    }
}
