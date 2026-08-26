package com.edtech.platform.auth.dto.response;

import java.time.Instant;

public record ParentContactResponse(
        String parentFullName,
        String parentPhone,
        String parentEmail,
        Boolean notifyParent,
        Instant updatedAt
) {
}
