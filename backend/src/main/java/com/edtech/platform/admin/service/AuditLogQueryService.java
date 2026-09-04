package com.edtech.platform.admin.service;

import com.edtech.platform.admin.domain.AuditAction;
import com.edtech.platform.admin.dto.response.AuditLogView;
import com.edtech.platform.admin.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditLogQueryService {

    private final AuditLogRepository auditLogRepository;

    public Page<AuditLogView> findAuditLogs(UUID actorId, AuditAction action, String targetType, Pageable pageable) {
        return auditLogRepository.findAuditLogs(actorId, action, targetType, pageable)
                .map(AuditLogView::from);
    }
}