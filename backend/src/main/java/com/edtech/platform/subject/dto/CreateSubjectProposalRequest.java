package com.edtech.platform.subject.dto;

import com.edtech.platform.subject.domain.EducationLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSubjectProposalRequest(
        @NotBlank @jakarta.validation.constraints.Pattern(regexp = "^[^<]*$", message = "Name must not contain HTML tags") String proposedName,
        @NotNull EducationLevel educationLevel,
        @jakarta.validation.constraints.Pattern(regexp = "^[^<]*$", message = "Description must not contain HTML tags") String description
) {
}
