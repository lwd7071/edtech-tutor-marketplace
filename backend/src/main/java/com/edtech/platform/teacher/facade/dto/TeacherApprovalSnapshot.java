package com.edtech.platform.teacher.facade.dto;

import java.time.Instant;
import java.util.UUID;

public record TeacherApprovalSnapshot(
        UUID teacherProfileId,
        UUID userId,
        String status,
        String rejectionReason,
        UUID approvedBy,
        Instant approvedAt
) {
}
