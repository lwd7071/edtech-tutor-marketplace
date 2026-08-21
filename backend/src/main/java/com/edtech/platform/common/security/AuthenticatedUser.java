package com.edtech.platform.common.security;

import java.util.UUID;

public record AuthenticatedUser(
    UUID id,
    String email,
    String role
) {
    public UUID getId() { return id(); }
    public String getEmail() { return email(); }
    public String getRole() { return role(); }
}
