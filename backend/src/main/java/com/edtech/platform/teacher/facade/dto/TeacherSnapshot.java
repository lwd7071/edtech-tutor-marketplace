package com.edtech.platform.teacher.facade.dto;


import java.util.UUID;

public record TeacherSnapshot(
        UUID id,
        UUID userId,
        String status,
        boolean isVerified,
        boolean isVisible,
        String fullName,
        String avatarUrl,
        String bioExcerpt
) {
}
