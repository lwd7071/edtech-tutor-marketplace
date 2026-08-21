package com.edtech.platform.catalog.dto;

import java.util.List;
import java.util.UUID;

public record TeacherCard(
        UUID id,
        String fullName,
        String avatarUrl,
        String bio,
        int yearsOfExperience,
        List<String> subjects,
        long minPriceVnd,
        double averageRating,
        double bayesianRating,
        int reviewCount,
        Integer globalRank
) {}
