package com.edtech.platform.admin.dto.response;

import com.edtech.platform.admin.domain.AuditAction;
import com.edtech.platform.admin.domain.AuditLog;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditLogView(
        UUID id,
        UUID actorId,
        AuditAction action,
        String targetType,
        UUID targetId,
        Map<String, Object> beforeData,
        Map<String, Object> afterData,
        String ipAddress,
        String userAgent,
        Instant createdAt
) {
    public static AuditLogView from(AuditLog log) {
        return new AuditLogView(
                log.getId(),
                log.getActorId(),
                log.getAction(),
                log.getTargetType(),
                log.getTargetId(),
                log.getBeforeData(),
                log.getAfterData(),
                log.getIpAddress(),
                log.getUserAgent(),
                log.getCreatedAt()
        );
    }
}