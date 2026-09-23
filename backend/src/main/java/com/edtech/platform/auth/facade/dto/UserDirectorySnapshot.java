package com.edtech.platform.auth.facade.dto;


import java.time.Instant;
import java.util.UUID;

public record UserDirectorySnapshot(UUID id, String fullName, String email, String role, String status,
                                    Instant createdAt, Instant lastLoginAt) {}
