package com.edtech.platform.admin.service;

import com.edtech.platform.admin.domain.AuditAction;
import com.edtech.platform.admin.domain.AuditLog;
import com.edtech.platform.admin.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepository repository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void append(UUID actorId, AuditAction action, String targetType, UUID targetId,
                       Map<String, Object> before, Map<String, Object> after, AuditContext context) {
        repository.append(new AuditLog(actorId, action, targetType, targetId, before, after,
                context.ipAddress(), context.userAgent()));
    }
}
