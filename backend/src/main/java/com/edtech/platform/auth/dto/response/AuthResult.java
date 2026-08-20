package com.edtech.platform.auth.dto.response;

import com.edtech.platform.auth.domain.Role;
import com.edtech.platform.auth.domain.UserStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

public record AuthResult(
    String accessToken,
    String refreshToken,
    String tokenType,
    long accessTokenExpiresIn,
    UserSummary user
) {
    public record UserSummary(
        UUID id,
        String email,
        String fullName,
        Role role,
        UserStatus status,
        @JsonInclude(JsonInclude.Include.NON_NULL) String avatarUrl
    ) {}
}
