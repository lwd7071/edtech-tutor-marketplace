package com.edtech.platform.auth.facade.dto;

import com.edtech.platform.auth.domain.Role;
import com.edtech.platform.auth.domain.UserStatus;
import java.util.UUID;

public record IdentitySnapshot(
        UUID id,
        String email,
        String fullName,
        String roleName,
        String statusName,
        String avatarUrl,
        Boolean notifyParent,
        String parentEmail
) {
    public String roleName() {
        return roleName;
    }

    public String statusName() {
        return statusName;
    }
}
