package com.edtech.platform.finance.dto.response;

import com.edtech.platform.finance.domain.ExtensionStatus;
import com.edtech.platform.finance.domain.PackageExtensionRequest;

import java.time.Instant;
import java.util.UUID;

public record ExtensionRequestView(
        UUID id,
        UUID studentPackageId,
        UUID studentId,
        String reason,
        Instant requestedExpiryDate,
        Instant approvedExpiryDate,
        ExtensionStatus status,
        String adminNote,
        UUID reviewedBy,
        Instant reviewedAt,
        Instant createdAt
) {
    public static ExtensionRequestView from(PackageExtensionRequest e) {
        return new ExtensionRequestView(
                e.getId(),
                e.getStudentPackageId(),
                e.getStudentId(),
                e.getReason(),
                e.getRequestedExpiryDate(),
                e.getApprovedExpiryDate(),
                e.getStatus(),
                e.getAdminNote(),
                e.getReviewedBy(),
                e.getReviewedAt(),
                e.getCreatedAt()
        );
    }
}