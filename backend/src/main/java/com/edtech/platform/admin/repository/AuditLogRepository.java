package com.edtech.platform.admin.repository;

import com.edtech.platform.admin.domain.AuditLog;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AuditLogRepository {
    private final EntityManager entityManager;

    public AuditLog append(AuditLog auditLog) {
        entityManager.persist(auditLog);
        return auditLog;
    }
}
