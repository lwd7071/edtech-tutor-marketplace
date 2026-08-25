package com.edtech.platform.subject.facade.dto;

import java.time.Instant;
import java.util.UUID;

public record SubjectProposalSnapshot(
        UUID proposalId, UUID teacherId, String proposedName, String educationLevel,
        String description, String status, String reviewNote, UUID reviewedBy,
        Instant reviewedAt, UUID subjectId) {
}
