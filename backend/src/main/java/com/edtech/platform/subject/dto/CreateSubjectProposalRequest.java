package com.edtech.platform.subject.dto;

import com.edtech.platform.subject.domain.EducationLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSubjectProposalRequest(
        @NotBlank String proposedName,
        @NotNull EducationLevel educationLevel,
        String description
) {
}
