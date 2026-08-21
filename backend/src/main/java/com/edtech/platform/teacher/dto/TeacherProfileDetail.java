package com.edtech.platform.teacher.dto;

import com.edtech.platform.teacher.domain.ProfileStatus;

import java.util.List;
import java.util.UUID;

public record TeacherProfileDetail(
        UUID id,
        String bio,
        Integer yearsOfExperience,
        List<String> languages,
        boolean supportsOnline,
        boolean supportsOffline,
        String locationAddress,
        String introductionVideoUrl,
        ProfileStatus profileStatus,
        String rejectionReason,
        boolean verifiedBadge,
        boolean isVisible
) {
}
