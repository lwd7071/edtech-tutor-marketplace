package com.edtech.platform.catalog.dto;

import java.util.List;
import java.util.UUID;

public record TeacherPublicDetail(
        UUID id,
        String fullName,
        String avatarUrl,
        String bio,
        int yearsOfExperience,
        List<String> languages,
        boolean supportsOnline,
        boolean supportsOffline,
        String locationAddress,
        String introductionVideoUrl,
        List<String> subjects,
        double averageRating,
        double bayesianRating,
        int reviewCount,
        Integer globalRank
) {}
