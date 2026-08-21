package com.edtech.platform.subject.dto;

import com.edtech.platform.subject.domain.EducationLevel;

import java.util.UUID;

public record SubjectSummary(
        UUID id,
        String code,
        String name,
        String slug,
        EducationLevel educationLevel,
        String description
) {
}
