package com.edtech.platform.common.security;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import java.io.Serializable;

public record AuthenticatedUser(
    UUID id,
    String email,
    String role
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public UUID getId() { return id(); }
    public String getEmail() { return email(); }
    public String getRole() { return role(); }
}
