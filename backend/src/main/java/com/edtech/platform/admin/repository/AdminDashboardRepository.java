package com.edtech.platform.admin.repository;

import com.edtech.platform.admin.dto.response.AdminDashboardView;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AdminDashboardRepository {

    private final JdbcTemplate jdbcTemplate;

    public AdminDashboardView getDashboardStats() {
        Long totalGmvVnd = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount_vnd), 0) FROM invoices WHERE status = 'PAID' AND is_deleted = false",
                Long.class);

        Long totalCommissionVnd = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount_vnd), 0) FROM ledger_entries WHERE entry_type = 'COMMISSION_RECOGNIZED'",
                Long.class);

        Long totalTeachers = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM teacher_profiles WHERE profile_status = 'APPROVED' AND is_deleted = false",
                Long.class);

        Long totalStudents = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE role = 'STUDENT' AND is_deleted = false",
                Long.class);

        Long totalBookings = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM bookings WHERE is_deleted = false",
                Long.class);

        Long completedBookings = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM bookings WHERE status = 'COMPLETED' AND is_deleted = false",
                Long.class);

        Long scheduledBookings = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM bookings WHERE status = 'SCHEDULED' AND is_deleted = false",
                Long.class);

        Long cancelledBookings = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM bookings WHERE status = 'CANCELLED' AND is_deleted = false",
                Long.class);

        Long pendingPayoutsCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payout_requests WHERE status = 'PENDING' AND is_deleted = false",
                Long.class);

        Long pendingPayoutsAmountVnd = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount_vnd), 0) FROM payout_requests WHERE status = 'PENDING' AND is_deleted = false",
                Long.class);

        Long pendingRefundsCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refund_requests WHERE status = 'PENDING' AND is_deleted = false",
                Long.class);

        return new AdminDashboardView(
                totalGmvVnd != null ? totalGmvVnd : 0L,
                totalCommissionVnd != null ? totalCommissionVnd : 0L,
                totalTeachers != null ? totalTeachers : 0L,
                totalStudents != null ? totalStudents : 0L,
                totalBookings != null ? totalBookings : 0L,
                completedBookings != null ? completedBookings : 0L,
                scheduledBookings != null ? scheduledBookings : 0L,
                cancelledBookings != null ? cancelledBookings : 0L,
                pendingPayoutsCount != null ? pendingPayoutsCount : 0L,
                pendingPayoutsAmountVnd != null ? pendingPayoutsAmountVnd : 0L,
                pendingRefundsCount != null ? pendingRefundsCount : 0L
        );
    }
}