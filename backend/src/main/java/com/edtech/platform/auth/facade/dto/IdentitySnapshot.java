package com.edtech.platform.auth.facade.dto;

import com.edtech.platform.auth.domain.Role;
import com.edtech.platform.auth.domain.UserStatus;
import java.util.UUID;

public record IdentitySnapshot(
        UUID id,
        String email,
        String fullName,
        Role role,
        UserStatus status,
        String avatarUrl,
        Boolean notifyParent,
        String parentEmail
) {
}
