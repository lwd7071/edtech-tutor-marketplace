package com.edtech.platform.teacher.dto;

import com.edtech.platform.teacher.domain.DocumentType;
import com.edtech.platform.teacher.domain.VerificationStatus;

import java.time.Instant;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;

public record TeacherDocumentView(
        UUID id,
        DocumentType documentType,
        String title,
        @JsonIgnore String secureUrl,
        String mimeType,
        Long fileSize,
        VerificationStatus verificationStatus,
        Instant verifiedAt
) {
}
