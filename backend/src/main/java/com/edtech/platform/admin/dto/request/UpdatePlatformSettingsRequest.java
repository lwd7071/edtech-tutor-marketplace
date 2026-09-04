package com.edtech.platform.admin.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdatePlatformSettingsRequest(
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal commissionRate,
        @Min(0) int bayesianMinimumReviews,
        @Min(1) int bookingReminderHours,
        @Min(1) int bookingExpirationHours
) {}