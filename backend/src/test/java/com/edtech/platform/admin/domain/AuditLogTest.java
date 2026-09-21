package com.edtech.platform.admin.domain;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertSame;

class AuditLogTest {

    @Test
    void snapshotPreservesNullableValuesAndCopiesOuterMap() {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("source", "fixture");
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("provinceCode", null);
        before.put("wardCode", null);
        before.put("nested", nested);
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("provinceCode", "82");
        after.put("wardCode", null);

        AuditLog audit = new AuditLog(UUID.randomUUID(), AuditAction.TEACHER_RESIDENCE_UPDATED,
                "TEACHER_PROFILE", UUID.randomUUID(), before, after, null, null);

        assertThat(audit.getBeforeData()).containsKeys("provinceCode", "wardCode", "nested");
        assertThat(audit.getBeforeData().get("provinceCode")).isNull();
        assertThat(audit.getBeforeData().get("wardCode")).isNull();
        assertThat(audit.getAfterData()).containsKey("wardCode");
        assertThat(audit.getAfterData().get("wardCode")).isNull();
        assertSame(nested, audit.getBeforeData().get("nested"));

        before.put("provinceCode", "79");
        after.put("wardCode", "00004");
        assertThat(audit.getBeforeData().get("provinceCode")).isNull();
        assertThat(audit.getAfterData().get("wardCode")).isNull();
        assertThatThrownBy(() -> audit.getBeforeData().put("newKey", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void snapshotAllowsNullMapsAndEmptyMaps() {
        assertThatCode(() -> new AuditLog(UUID.randomUUID(), AuditAction.USER_LOCKED,
                "USER", UUID.randomUUID(), null, null, null, null)).doesNotThrowAnyException();

        AuditLog audit = new AuditLog(UUID.randomUUID(), AuditAction.USER_LOCKED,
                "USER", UUID.randomUUID(), Map.of(), Map.of(), null, null);

        assertThat(audit.getBeforeData()).isEmpty();
        assertThat(audit.getAfterData()).isEmpty();
    }

    @Test
    void snapshotRejectsNullKeyBeforeCopying() {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("valid", "value");
        source.put(null, "invalid");

        assertThatThrownBy(() -> new AuditLog(UUID.randomUUID(), AuditAction.USER_LOCKED,
                "USER", UUID.randomUUID(), source, Map.of(), null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
