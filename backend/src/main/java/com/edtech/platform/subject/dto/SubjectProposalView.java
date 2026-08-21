package com.edtech.platform.subject.dto;

import com.edtech.platform.subject.domain.EducationLevel;
import com.edtech.platform.subject.domain.ProposalStatus;

import java.time.Instant;
import java.util.UUID;

public record SubjectProposalView(
        UUID id,
        String proposedName,
        EducationLevel educationLevel,
        String description,
        ProposalStatus status,
        String reviewNote,
        Instant reviewedAt,
        UUID createdSubjectId
) {
}
