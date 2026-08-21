package com.edtech.platform.catalog.dto;

import com.edtech.platform.catalog.domain.PackageStatus;
import java.util.UUID;

public record PricingPackageView(
        UUID id,
        UUID subjectId,
        String subjectName,
        String name,
        String description,
        int totalSessions,
        int durationDays,
        long priceVnd,
        int sessionDurationMinutes,
        PackageStatus status,
        long version
) {}
