package com.edtech.platform.dashboard.dto.response;

import java.time.Instant;

public record StudentDashboardView(
        Instant nextBookingStartTime,
        long remainingSessions,
        long todoAssignments,
        long unreadNotifications,
        long pendingRequests
) {
}
