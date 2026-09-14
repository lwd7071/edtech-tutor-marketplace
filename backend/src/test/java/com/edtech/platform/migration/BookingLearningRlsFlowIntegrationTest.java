package com.edtech.platform.migration;

import com.edtech.platform.common.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class BookingLearningRlsFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("V35 Cross-Table Flow: Package -> Booking -> SessionReport -> Review & Stats -> Assignment -> Submission")
    void verifyBookingLearningFlowWithRlsEnabled() {
        UUID studentUserId = UUID.randomUUID();
        UUID teacherUserId = UUID.randomUUID();
        UUID teacherProfileId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        UUID studentPackageId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();

        String studentEmail = "flow_student_" + studentUserId.toString().substring(0, 8) + "@test.com";
        String teacherEmail = "flow_teacher_" + teacherUserId.toString().substring(0, 8) + "@test.com";
        String subjectCode = "FLW_SBJ_" + UUID.randomUUID().toString().substring(0, 6);
        String invoiceNumber = "INV_" + UUID.randomUUID().toString().substring(0, 8);
        UUID idempotencyKey = UUID.randomUUID();

        try {
            // 1. Users & Teacher Profile
            jdbcTemplate.update("INSERT INTO public.users (id, email, password_hash, full_name, role, status) VALUES (?, ?, 'hash', 'Flow Student', 'STUDENT', 'ACTIVE')",
                    studentUserId, studentEmail);
            jdbcTemplate.update("INSERT INTO public.users (id, email, password_hash, full_name, role, status) VALUES (?, ?, 'hash', 'Flow Teacher', 'TEACHER', 'ACTIVE')",
                    teacherUserId, teacherEmail);
            jdbcTemplate.update("INSERT INTO public.teacher_profiles (id, user_id, bio, years_of_experience, profile_status, verified_badge, is_visible) VALUES (?, ?, 'Teacher Bio', 5, 'APPROVED', true, true)",
                    teacherProfileId, teacherUserId);

            // 2. Subject & PricingPackage
            jdbcTemplate.update("INSERT INTO public.subjects (id, name, code, slug, education_level, is_active) VALUES (?, 'Flow Subject', ?, ?, 'HIGH_SCHOOL', true)",
                    subjectId, subjectCode, "slug-" + subjectCode.toLowerCase());
            jdbcTemplate.update("INSERT INTO public.pricing_packages (id, teacher_id, subject_id, name, total_sessions, duration_days, price_vnd, session_duration_minutes, status) VALUES (?, ?, ?, '10 Session Package', 10, 30, 2000000, 60, 'ACTIVE')",
                    packageId, teacherProfileId, subjectId);

            // 3. Invoice & StudentPackage (Purchase)
            jdbcTemplate.update("INSERT INTO public.invoices (id, invoice_number, student_id, teacher_id, pricing_package_id, amount_vnd, status, payos_order_code, idempotency_key, request_fingerprint, subject_id_snapshot, package_name_snapshot, total_sessions_snapshot, duration_days_snapshot, session_duration_minutes_snapshot, commission_rate_snapshot, version) VALUES (?, ?, ?, ?, ?, 2000000, 'PAID', nextval('payos_order_code_seq'), ?, 'fp_flow_test', ?, '10 Session Package', 10, 30, 60, 15.00, 0)",
                    invoiceId, invoiceNumber, studentUserId, teacherProfileId, packageId, idempotencyKey, subjectId);
            jdbcTemplate.update("INSERT INTO public.student_packages (id, student_id, teacher_id, subject_id, pricing_package_id, invoice_id, package_name_snapshot, total_sessions, remaining_sessions, reserved_sessions, completed_sessions, refunded_sessions, purchase_price_vnd, commission_rate, starts_at, expires_at, status) VALUES (?, ?, ?, ?, ?, ?, '10 Session Package', 10, 10, 0, 0, 0, 2000000, 0.1500, now(), now() + interval '30 days', 'ACTIVE')",
                    studentPackageId, studentUserId, teacherProfileId, subjectId, packageId, invoiceId);

            // 4. Booking (SCHEDULED -> COMPLETED)
            jdbcTemplate.update("INSERT INTO public.bookings (id, student_id, teacher_id, student_package_id, subject_id, start_time, end_time, delivery_mode, status, is_trial) VALUES (?, ?, ?, ?, ?, now() + interval '1 hour', now() + interval '2 hours', 'ONLINE', 'SCHEDULED', false)",
                    bookingId, studentUserId, teacherProfileId, studentPackageId, subjectId);

            jdbcTemplate.update("UPDATE public.bookings SET status = 'COMPLETED' WHERE id = ?", bookingId);
            jdbcTemplate.update("UPDATE public.student_packages SET remaining_sessions = remaining_sessions - 1, completed_sessions = completed_sessions + 1 WHERE id = ?", studentPackageId);

            // 5. SessionReport by Teacher
            jdbcTemplate.update("INSERT INTO public.session_reports (id, booking_id, record_link, content, feedback, follow_up_note, teacher_self_rating) VALUES (?, ?, 'https://record.link', 'Session content', 'Great progress today', 'Review exercises', 5)",
                    reportId, bookingId);

            // 6. Review by Student & TeacherStats
            jdbcTemplate.update("INSERT INTO public.reviews (id, booking_id, student_id, teacher_id, rating, comment) VALUES (?, ?, ?, ?, 5, 'Excellent teacher!')",
                    reviewId, bookingId, studentUserId, teacherProfileId);

            // Initialize or update teacher_stats
            jdbcTemplate.update("""
                    INSERT INTO public.teacher_stats (teacher_id, average_rating, bayesian_rating, review_count, completed_session_count, completion_rate, trial_session_count, trial_conversion_rate)
                    VALUES (?, 5.0, 5.0, 1, 1, 1.0, 0, 0.0)
                    ON CONFLICT (teacher_id) DO UPDATE
                    SET review_count = teacher_stats.review_count + 1,
                        average_rating = 5.0,
                        completed_session_count = teacher_stats.completed_session_count + 1
                    """, teacherProfileId);

            // 7. Assignment & Submission
            jdbcTemplate.update("INSERT INTO public.assignments (id, teacher_id, student_id, subject_id, title, assignment_type, status) VALUES (?, ?, ?, ?, 'Homework 1', 'FREEFORM', 'PUBLISHED')",
                    assignmentId, teacherProfileId, studentUserId, subjectId);

            jdbcTemplate.update("INSERT INTO public.submissions (id, assignment_id, student_id, status, score, feedback_text) VALUES (?, ?, ?, 'GRADED', 95.00, 'Good job')",
                    submissionId, assignmentId, studentUserId);

            // 8. Assertions: Everything queried through Backend connection succeeds
            Map<String, Object> pkgMap = jdbcTemplate.queryForMap("SELECT remaining_sessions FROM public.student_packages WHERE id = ?", studentPackageId);
            assertThat(pkgMap.get("remaining_sessions")).isEqualTo(9);

            Map<String, Object> bkgMap = jdbcTemplate.queryForMap("SELECT status FROM public.bookings WHERE id = ?", bookingId);
            assertThat(bkgMap.get("status")).isEqualTo("COMPLETED");

            Map<String, Object> rptMap = jdbcTemplate.queryForMap("SELECT feedback FROM public.session_reports WHERE id = ?", reportId);
            assertThat(rptMap.get("feedback")).isEqualTo("Great progress today");

            Map<String, Object> revMap = jdbcTemplate.queryForMap("SELECT rating, comment FROM public.reviews WHERE id = ?", reviewId);
            assertThat(revMap.get("rating")).isEqualTo(5);

            Map<String, Object> statMap = jdbcTemplate.queryForMap("SELECT review_count, completed_session_count FROM public.teacher_stats WHERE teacher_id = ?", teacherProfileId);
            assertThat(statMap.get("review_count")).isEqualTo(1);
            assertThat(statMap.get("completed_session_count")).isEqualTo(1);

            Map<String, Object> subMap = jdbcTemplate.queryForMap("SELECT score, status FROM public.submissions WHERE id = ?", submissionId);
            assertThat(((BigDecimal) subMap.get("score")).doubleValue()).isEqualTo(95.0);
            assertThat(subMap.get("status")).isEqualTo("GRADED");

        } finally {
            // Clean up
            jdbcTemplate.update("DELETE FROM public.submissions WHERE id = ?", submissionId);
            jdbcTemplate.update("DELETE FROM public.assignments WHERE id = ?", assignmentId);
            jdbcTemplate.update("DELETE FROM public.reviews WHERE id = ?", reviewId);
            jdbcTemplate.update("DELETE FROM public.teacher_stats WHERE teacher_id = ?", teacherProfileId);
            jdbcTemplate.update("DELETE FROM public.session_reports WHERE id = ?", reportId);
            jdbcTemplate.update("DELETE FROM public.bookings WHERE id = ?", bookingId);
            jdbcTemplate.update("DELETE FROM public.student_packages WHERE id = ?", studentPackageId);
            jdbcTemplate.update("DELETE FROM public.invoices WHERE id = ?", invoiceId);
            jdbcTemplate.update("DELETE FROM public.pricing_packages WHERE id = ?", packageId);
            jdbcTemplate.update("DELETE FROM public.subjects WHERE id = ?", subjectId);
            jdbcTemplate.update("DELETE FROM public.teacher_profiles WHERE id = ?", teacherProfileId);
            jdbcTemplate.update("DELETE FROM public.users WHERE id IN (?, ?)", studentUserId, teacherUserId);
        }
    }
}
