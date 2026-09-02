package com.edtech.platform.admin.service;

import com.edtech.platform.auth.facade.dto.IdentitySnapshot;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditSnapshotMapperTest {

    @Test
    void userSnapshotContainsOnlyModerationSafeFields() {
        UUID userId = UUID.randomUUID();
        IdentitySnapshot identity = new IdentitySnapshot(
                userId, "private@example.com", "Nguyen Van A", "STUDENT",
                "ACTIVE", "https://private/avatar", true, "parent@example.com");

        Map<String, Object> snapshot = new AuditSnapshotMapper().user(identity);

        assertThat(snapshot).containsExactlyInAnyOrderEntriesOf(Map.of(
                "userId", userId,
                "role", "STUDENT",
                "status", "ACTIVE"
        ));
    }
}
