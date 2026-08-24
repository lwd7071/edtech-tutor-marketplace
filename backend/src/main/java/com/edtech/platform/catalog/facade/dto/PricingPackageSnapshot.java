package com.edtech.platform.catalog.facade.dto;

import java.util.UUID;

public record PricingPackageSnapshot(
        UUID id,
        UUID teacherId,
        UUID subjectId,
        String name,
        int totalSessions,
        int durationDays,
        long priceVnd,
        int sessionDurationMinutes
) {
}
