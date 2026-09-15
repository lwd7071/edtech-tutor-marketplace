package com.edtech.platform.teacher.dto;
import com.edtech.platform.teacher.domain.CredentialStatus;
import java.time.Instant;
import java.util.UUID;
public record AdminCredentialView(UUID id, UUID teacherId, String label, String proofUrl, CredentialStatus status, String rejectedReason, Instant createdAt, long version) {}
