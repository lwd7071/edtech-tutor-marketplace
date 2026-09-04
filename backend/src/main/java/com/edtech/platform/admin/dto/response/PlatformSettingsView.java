package com.edtech.platform.admin.dto.response;

import com.edtech.platform.admin.domain.PlatformSettings;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PlatformSettingsView(
        UUID id,
        BigDecimal commissionRate,
        int bayesianMinimumReviews,
        int bookingReminderHours,
        int bookingExpirationHours,
        Instant updatedAt
) {
    public static PlatformSettingsView from(PlatformSettings settings) {
        return new PlatformSettingsView(
                settings.getId(),
                settings.getCommissionRate(),
                settings.getBayesianMinimumReviews(),
                settings.getBookingReminderHours(),
                settings.getBookingExpirationHours(),
                settings.getUpdatedAt()
        );
    }
}