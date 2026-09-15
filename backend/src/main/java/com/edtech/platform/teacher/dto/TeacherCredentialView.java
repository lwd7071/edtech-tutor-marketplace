package com.edtech.platform.teacher.dto;

import com.edtech.platform.teacher.domain.CredentialStatus;
import java.time.Instant;
import java.util.UUID;

public record TeacherCredentialView(UUID id, String label, CredentialStatus status, String proofUrl,
                                   String rejectedReason, Instant verifiedAt, long version) {}
