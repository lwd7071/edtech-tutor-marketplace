package com.edtech.platform.teacher.dto;

import com.edtech.platform.subject.domain.EducationLevel;

import java.util.UUID;

public record TeacherSubjectView(
        UUID id,
        SubjectDto subject,
        String levelDescription,
        String experienceDescription,
        boolean isActive
) {
    public record SubjectDto(
            UUID id,
            String code,
            String name,
            EducationLevel educationLevel
    ) {}
}
