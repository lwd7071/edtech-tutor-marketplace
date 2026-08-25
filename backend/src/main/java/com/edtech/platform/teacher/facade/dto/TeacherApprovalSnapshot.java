package com.edtech.platform.teacher.facade.dto;

import java.time.Instant;
import java.util.UUID;
import java.util.List;

public record TeacherApprovalSnapshot(
        UUID teacherProfileId,
        UUID userId,
        String status,
        String rejectionReason,
        UUID approvedBy,
        Instant approvedAt,
        List<TeacherDocumentSnapshot> documents
) {
    public TeacherApprovalSnapshot(UUID teacherProfileId, UUID userId, String status,
                                   String rejectionReason, UUID approvedBy, Instant approvedAt) {
        this(teacherProfileId, userId, status, rejectionReason, approvedBy, approvedAt, List.of());
    }
}
