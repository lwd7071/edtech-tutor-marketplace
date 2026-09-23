package com.edtech.platform.admin.repository;

import com.edtech.platform.admin.domain.AuditAction;
import com.edtech.platform.admin.service.AuditContext;
import com.edtech.platform.admin.service.AuditLogService;
import com.edtech.platform.admin.service.AuditLogQueryService;
import com.edtech.platform.common.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogPersistenceIntegrationTest extends AbstractIntegrationTest {
    @Autowired AuditLogService auditLogs;
    @Autowired AuditLogQueryService auditQueries;
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

    @Test
    @Transactional
    void appendPersistsNullableSnapshotKeysAsJsonNull() {
        UUID targetId = UUID.randomUUID();
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("provinceCode", null);
        before.put("wardCode", null);
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("provinceCode", "82");
        after.put("wardCode", null);

        auditLogs.append(null, AuditAction.USER_LOCKED, "USER", targetId, before, after,
                new AuditContext("127.0.0.1", "integration-test"));
        entityManager.flush();

        Map<String, Object> row = jdbc.queryForMap("""
                select jsonb_exists(before_data, 'wardCode') before_has_ward,
                       jsonb_exists(after_data, 'wardCode') after_has_ward,
                       jsonb_typeof(before_data -> 'wardCode') before_ward_type,
                       jsonb_typeof(after_data -> 'wardCode') after_ward_type
                from audit_logs where target_id = ?
                """, targetId);
        assertThat(row).containsEntry("before_has_ward", true)
                .containsEntry("after_has_ward", true)
                .containsEntry("before_ward_type", "null")
                .containsEntry("after_ward_type", "null");
    }

    @Test
    @Transactional
    void auditHistoryCanBeFilteredByExactTargetIdAndType() {
        UUID userId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        auditLogs.append(null, AuditAction.USER_LOCKED, "USER", userId, Map.of(), Map.of("moderationNote", "lock"),
                new AuditContext("127.0.0.1", "integration-test"));
        auditLogs.append(null, AuditAction.USER_UNLOCKED, "USER", userId, Map.of(), Map.of("moderationNote", "unlock"),
                new AuditContext("127.0.0.1", "integration-test"));
        auditLogs.append(null, AuditAction.USER_LOCKED, "USER", otherId, Map.of(), Map.of(),
                new AuditContext("127.0.0.1", "integration-test"));
        auditLogs.append(null, AuditAction.USER_LOCKED, "TEACHER_PROFILE", userId, Map.of(), Map.of(),
                new AuditContext("127.0.0.1", "integration-test"));
        entityManager.flush();

        var result = auditQueries.findAuditLogs(null, null, "USER", userId, PageRequest.of(0, 20));
        assertThat(result.getContent()).hasSize(2).allSatisfy(log -> {
            assertThat(log.targetId()).isEqualTo(userId);
            assertThat(log.targetType()).isEqualTo("USER");
        });
    }
}
