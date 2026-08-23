package com.edtech.platform.booking.facade.dto;

public record BookingStatsSnapshot(
        int completedSessions,
        int totalSessions,
        int trialSessions
) {
}
