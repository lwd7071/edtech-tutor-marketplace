package com.edtech.platform.teacher.facade.dto;

import java.util.UUID;

public record TeacherDocumentSnapshot(
        UUID id, String type, String title, String secureUrl,
        String mimeType, Long fileSize, String verificationStatus) {
}
