package com.edtech.platform.admin.dto.response;

import com.edtech.platform.auth.facade.dto.UserDirectorySnapshot;

import java.time.Instant;
import java.util.UUID;

public record AdminUserView(UUID id, String fullName, String email, String role, String status,
                           Instant createdAt, Instant lastLoginAt) {
    public static AdminUserView from(UserDirectorySnapshot snapshot) {
        return new AdminUserView(snapshot.id(), snapshot.fullName(), snapshot.email(), snapshot.role(),
                snapshot.status(), snapshot.createdAt(), snapshot.lastLoginAt());
    }
}
