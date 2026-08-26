package com.edtech.platform.catalog.dto;

import java.util.List;
import java.util.UUID;

public record TeacherCard(
        UUID id,
        String fullName,
        String avatarUrl,
        String bioExcerpt,
        int yearsOfExperience,
        boolean verifiedBadge,
        boolean supportsOnline,
        boolean supportsOffline,
        List<SubjectDto> subjects,
        long startingPriceVnd,
        double averageRating,
        double bayesianRating,
        int reviewCount,
        Integer globalRank
) {}
