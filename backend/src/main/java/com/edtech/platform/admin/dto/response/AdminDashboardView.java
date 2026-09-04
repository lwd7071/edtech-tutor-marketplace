package com.edtech.platform.admin.dto.response;

public record AdminDashboardView(
        long totalGmvVnd,
        long totalCommissionVnd,
        long totalTeachers,
        long totalStudents,
        long totalBookings,
        long completedBookings,
        long scheduledBookings,
        long cancelledBookings,
        long pendingPayoutsCount,
        long pendingPayoutsAmountVnd,
        long pendingRefundsCount
) {}