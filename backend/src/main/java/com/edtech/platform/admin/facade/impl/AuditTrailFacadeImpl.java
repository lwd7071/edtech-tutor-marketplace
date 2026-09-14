package com.edtech.platform.admin.facade.impl;

import com.edtech.platform.admin.domain.AuditAction;
import com.edtech.platform.admin.service.AuditContext;
import com.edtech.platform.admin.service.AuditLogService;
import com.edtech.platform.admin.facade.AuditTrailFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditTrailFacadeImpl implements AuditTrailFacade {
    private final AuditLogService auditLogs;

    @Override
    public void append(UUID actorId, String action, String targetType, UUID targetId,
                       Map<String, Object> before, Map<String, Object> after) {
        auditLogs.append(actorId, AuditAction.valueOf(action), targetType, targetId,
                before, after, new AuditContext(null, null));
    }
}
