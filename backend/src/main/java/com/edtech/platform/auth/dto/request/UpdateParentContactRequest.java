package com.edtech.platform.auth.dto.request;

public record UpdateParentContactRequest(
        String parentFullName,
        String parentPhone,
        String parentEmail,
        Boolean notifyParent
) {
}
