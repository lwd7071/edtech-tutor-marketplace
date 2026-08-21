package com.edtech.platform.teacher.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateTeacherProfileRequest(
        String bio,
        Integer yearsOfExperience,
        List<String> languages,
        @NotNull Boolean supportsOnline,
        @NotNull Boolean supportsOffline,
        String locationAddress,
        String introductionVideoUrl
) {
}
