package com.edtech.platform.catalog.dto;

import java.util.List;
import java.util.UUID;
import com.edtech.platform.teacher.dto.PublicCredentialBadge;

public record TeacherPublicDetail(
        UUID id,
        String fullName,
        String avatarUrl,
        String bio,
        int yearsOfExperience,
        List<String> languages,
        boolean supportsOnline,
        boolean supportsOffline,
        String provinceName,
        String wardName,
        String introductionVideoUrl,
        List<String> subjects,
        double averageRating,
        double bayesianRating,
        int reviewCount,
        Integer globalRank,
        List<PublicCredentialBadge> credentials
) {}
