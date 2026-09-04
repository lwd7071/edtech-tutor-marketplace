package com.edtech.platform.admin.repository;

import com.edtech.platform.admin.domain.AuditAction;
import com.edtech.platform.admin.domain.AuditLog;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AuditLogRepository {
    private final EntityManager entityManager;

    public AuditLog append(AuditLog auditLog) {
        entityManager.persist(auditLog);
        return auditLog;
    }

    public Page<AuditLog> findAuditLogs(UUID actorId, AuditAction action, String targetType, Pageable pageable) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (actorId != null) where.append(" AND a.actorId = :actorId");
        if (action != null) where.append(" AND a.action = :action");
        if (targetType != null && !targetType.isBlank()) where.append(" AND a.targetType = :targetType");

        String ql = "SELECT a FROM AuditLog a" + where + " ORDER BY a.createdAt DESC";
        TypedQuery<AuditLog> query = entityManager.createQuery(ql, AuditLog.class);
        if (actorId != null) query.setParameter("actorId", actorId);
        if (action != null) query.setParameter("action", action);
        if (targetType != null && !targetType.isBlank()) query.setParameter("targetType", targetType);

        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        List<AuditLog> list = query.getResultList();

        String countQl = "SELECT COUNT(a) FROM AuditLog a" + where;
        TypedQuery<Long> countQuery = entityManager.createQuery(countQl, Long.class);
        if (actorId != null) countQuery.setParameter("actorId", actorId);
        if (action != null) countQuery.setParameter("action", action);
        if (targetType != null && !targetType.isBlank()) countQuery.setParameter("targetType", targetType);
        Long total = countQuery.getSingleResult();

        return new PageImpl<>(list, pageable, total != null ? total : 0);
    }
}