package com.edtech.platform.admin.repository;

import com.edtech.platform.admin.domain.AuditAction;
import com.edtech.platform.admin.service.AuditContext;
import com.edtech.platform.admin.service.AuditLogService;
import com.edtech.platform.common.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogPersistenceIntegrationTest extends AbstractIntegrationTest {
    @Autowired AuditLogService auditLogs;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbc;

    @Test
    @Transactional
    void appendPersistsJsonSnapshotsAndInetAddress() {
        UUID targetId = UUID.randomUUID();

        auditLogs.append(null, AuditAction.USER_LOCKED, "USER", targetId,
                Map.of("status", "ACTIVE"), Map.of("status", "LOCKED"),
                new AuditContext("127.0.0.1", "integration-test"));
        entityManager.flush();

        Map<String, Object> row = jdbc.queryForMap("""
                select action, before_data->>'status' before_status,
                       after_data->>'status' after_status, host(ip_address) ip
                from audit_logs where target_id = ?
                """, targetId);
        assertThat(row).containsEntry("action", "USER_LOCKED")
                .containsEntry("before_status", "ACTIVE")
                .containsEntry("after_status", "LOCKED")
                .containsEntry("ip", "127.0.0.1");
    }
}
