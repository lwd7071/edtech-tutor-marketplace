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
        Integer globalRank,
        String provinceName,
        String wardName
) {
    public TeacherCard(UUID id, String fullName, String avatarUrl, String bioExcerpt,
                       int yearsOfExperience, boolean verifiedBadge, boolean supportsOnline,
                       boolean supportsOffline, List<SubjectDto> subjects, long startingPriceVnd,
                       double averageRating, double bayesianRating, int reviewCount, Integer globalRank) {
        this(id, fullName, avatarUrl, bioExcerpt, yearsOfExperience, verifiedBadge, supportsOnline,
                supportsOffline, subjects, startingPriceVnd, averageRating, bayesianRating, reviewCount,
                globalRank, null, null);
    }
}
