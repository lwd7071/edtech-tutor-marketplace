package com.edtech.platform.teacher.integration;

import com.edtech.platform.common.AbstractIntegrationTest;
import com.edtech.platform.common.cache.PublicCacheRevalidationClient;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.teacher.dto.UpdateTeacherResidenceRequest;
import com.edtech.platform.teacher.service.TeacherProfileService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class TeacherResidenceIntegrationTest extends AbstractIntegrationTest {
    private static final String PROVINCE = "82";

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper objectMapper;
    @Autowired jakarta.persistence.EntityManager entityManager;
    @Autowired TeacherProfileService profileService;
    @Autowired PlatformTransactionManager transactionManager;

    @MockBean PublicCacheRevalidationClient revalidationClient;

    @Test
    void emptyProfile_toFullResidence_returns200_persistsAndAuditsNullableSnapshot() throws Exception {
        Fixture f = fixture(null, null);
        String ward = activeWard();

        update(f, "{\"provinceCode\":\"82\",\"wardCode\":\"" + ward + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provinceCode").value(PROVINCE))
                .andExpect(jsonPath("$.data.wardCode").value(ward));
        assertResidence(f.profileId, PROVINCE, ward, "APPROVED");
        getProfile(f).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provinceCode").value(PROVINCE))
                .andExpect(jsonPath("$.data.wardCode").value(ward));
        assertThat(auditCount(f.profileId)).isEqualTo(1);
        assertAudit(f.profileId, "null", ward);
    }

    @Test
    void emptyProfile_toProvinceOnly_keepsWardNull() throws Exception {
        Fixture f = fixture(null, null);
        update(f, "{\"provinceCode\":\"82\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provinceCode").value(PROVINCE));
        assertResidence(f.profileId, PROVINCE, null, "APPROVED");
        assertThat(auditCount(f.profileId)).isEqualTo(1);
    }

    @Test
    void provinceOnly_toFullResidence_addsWard() throws Exception {
        String ward = activeWard();
        Fixture f = fixture(PROVINCE, null);
        update(f, "{\"provinceCode\":\"82\",\"wardCode\":\"" + ward + "\"}").andExpect(status().isOk());
        assertResidence(f.profileId, PROVINCE, ward, "APPROVED");
        assertThat(auditCount(f.profileId)).isEqualTo(1);
    }

    @Test
    void explicitNullAndMissingWard_produceIdenticalBusinessAndAuditResult() throws Exception {
        String ward = activeWard();
        Fixture explicitNull = fixture(PROVINCE, ward);
        Fixture missing = fixture(PROVINCE, ward);

        update(explicitNull, "{\"provinceCode\":\"82\",\"wardCode\":null}").andExpect(status().isOk());
        update(missing, "{\"provinceCode\":\"82\"}").andExpect(status().isOk());
        assertResidence(explicitNull.profileId, PROVINCE, null, "APPROVED");
        assertResidence(missing.profileId, PROVINCE, null, "APPROVED");
        assertThat(audit(explicitNull.profileId).get("before")).isEqualTo(audit(missing.profileId).get("before"));
        assertThat(audit(explicitNull.profileId).get("after")).isEqualTo(audit(missing.profileId).get("after"));
        assertAuditContainsBothKeys(explicitNull.profileId);
        assertAuditContainsBothKeys(missing.profileId);
    }

    @Test
    void fullResidence_toExplicitNull_clearsBoth() throws Exception {
        Fixture f = fixture(PROVINCE, activeWard());
        update(f, "{\"provinceCode\":null,\"wardCode\":null}").andExpect(status().isOk());
        assertResidence(f.profileId, null, null, "APPROVED");
        assertThat(auditCount(f.profileId)).isEqualTo(1);
    }

    @Test
    void fullResidence_toEmptyObject_clearsBoth() throws Exception {
        Fixture f = fixture(PROVINCE, activeWard());
        update(f, "{}").andExpect(status().isOk());
        assertResidence(f.profileId, null, null, "APPROVED");
        assertThat(auditCount(f.profileId)).isEqualTo(1);
    }

    @Test
    void noChange_fullResidence_doesNotAppendAudit() throws Exception {
        String ward = activeWard();
        Fixture f = fixture(PROVINCE, ward);
        update(f, "{\"provinceCode\":\"82\",\"wardCode\":\"" + ward + "\"}").andExpect(status().isOk());
        assertThat(auditCount(f.profileId)).isZero();
    }

    @Test
    void noChange_provinceOnly_assertsStateThenResendsProvinceWithoutWard() throws Exception {
        Fixture f = fixture(PROVINCE, null);
        assertResidence(f.profileId, PROVINCE, null, "APPROVED");
        update(f, "{\"provinceCode\":\"82\"}").andExpect(status().isOk());
        assertThat(auditCount(f.profileId)).isZero();
    }

    @Test
    void noChange_emptyProfile_assertsBothNullThenSendsEmptyObject() throws Exception {
        Fixture f = fixture(null, null);
        assertResidence(f.profileId, null, null, "APPROVED");
        update(f, "{}").andExpect(status().isOk());
        assertThat(auditCount(f.profileId)).isZero();
    }

    @Test
    void wardWithoutProvince_isRejectedFromEmptyAndFullStates() throws Exception {
        String ward = activeWard();
        assertInvalidResidence(fixture(null, null), "{\"wardCode\":\"" + ward + "\"}");
        assertInvalidResidence(fixture(PROVINCE, ward), "{\"wardCode\":\"" + ward + "\"}");
    }

    @Test
    void explicitNullProvinceWithWard_isRejectedFromEmptyAndFullStates() throws Exception {
        String ward = activeWard();
        assertInvalidResidence(fixture(null, null), "{\"provinceCode\":null,\"wardCode\":\"" + ward + "\"}");
        assertInvalidResidence(fixture(PROVINCE, ward), "{\"provinceCode\":null,\"wardCode\":\"" + ward + "\"}");
    }

    @Test
    void rolledBackResidenceUpdate_leavesProfileAndAuditUnchanged() {
        String ward = activeWard();
        Fixture f = fixture(null, null);
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            profileService.updateResidence(f.userId, new UpdateTeacherResidenceRequest(PROVINCE, ward));
            entityManager.flush();
            tx.setRollbackOnly();
        });
        assertResidence(f.profileId, null, null, "APPROVED");
        assertThat(auditCount(f.profileId)).isZero();
    }

    private void assertInvalidResidence(Fixture f, String body) throws Exception {
        update(f, body).andExpect(status().isBadRequest());
        assertResidence(f.profileId, f.province, f.ward, "APPROVED");
        assertThat(auditCount(f.profileId)).isZero();
    }

    private org.springframework.test.web.servlet.ResultActions update(Fixture f, String body) throws Exception {
        return mockMvc.perform(put("/api/teacher/profile/residence")
                .header("Authorization", f.token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private org.springframework.test.web.servlet.ResultActions getProfile(Fixture f) throws Exception {
        return mockMvc.perform(get("/api/teacher/profile").header("Authorization", f.token));
    }

    private Fixture fixture(String province, String ward) {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id,email,password_hash,full_name,role,status) VALUES (?,?,?,?,?,?)",
                userId, "residence-" + userId + "@test.com", "hash", "Residence Test", "TEACHER", "ACTIVE");
        jdbc.update("INSERT INTO teacher_profiles (id,user_id,bio,profile_status,is_visible,province_code,ward_code) VALUES (?,?,?,?,?,?,?)",
                profileId, userId, "Bio", "APPROVED", true, province, ward);
        String token = "Bearer " + jwtTokenProvider.generateAccessToken(
                new AuthenticatedUser(userId, "residence-" + userId + "@test.com", "TEACHER"));
        return new Fixture(userId, profileId, token, province, ward);
    }

    private String activeWard() {
        var codes = jdbc.query("SELECT code FROM wards WHERE province_code=? AND is_active=true ORDER BY code ASC LIMIT 1",
                (rs, rowNum) -> rs.getString("code"), PROVINCE);
        if (!codes.isEmpty()) {
            return codes.getFirst();
        }
        Integer provinceCount = jdbc.queryForObject("SELECT count(*) FROM provinces WHERE code=?", Integer.class, PROVINCE);
        if (provinceCount == null || provinceCount == 0) {
            jdbc.update("INSERT INTO provinces(code,name,is_active) VALUES (?,?,true)", PROVINCE, "Test province 82");
        }
        for (int suffix = 0; suffix < 10000; suffix++) {
            String candidate = String.format("9%04d", suffix);
            Integer count = jdbc.queryForObject("SELECT count(*) FROM wards WHERE code=?", Integer.class, candidate);
            if (count != null && count == 0) {
                jdbc.update("INSERT INTO wards(code,province_code,name,is_active) VALUES (?,?,?,true)",
                        candidate, PROVINCE, "Test ward " + candidate);
                return candidate;
            }
        }
        throw new IllegalStateException("No deterministic test ward code available for province 82");
    }

    private void assertResidence(UUID profileId, String province, String ward, String status) {
        Map<String, Object> row = jdbc.queryForMap("SELECT province_code,ward_code,profile_status FROM teacher_profiles WHERE id=?", profileId);
        assertThat(row.get("province_code")).isEqualTo(province);
        assertThat(row.get("ward_code")).isEqualTo(ward);
        assertThat(row.get("profile_status")).isEqualTo(status);
    }

    private int auditCount(UUID profileId) {
        return jdbc.queryForObject("SELECT count(*) FROM audit_logs WHERE target_id=?", Integer.class, profileId);
    }

    private void assertAudit(UUID profileId, String beforeWard, String afterWard) throws Exception {
        Map<String, Object> row = jdbc.queryForMap("SELECT before_data::text before_data,after_data::text after_data FROM audit_logs WHERE target_id=?", profileId);
        JsonNode before = objectMapper.readTree((String) row.get("before_data"));
        JsonNode after = objectMapper.readTree((String) row.get("after_data"));
        assertThat(before.get("wardCode").isNull()).isTrue();
        assertThat(after.get("wardCode").asText()).isEqualTo(afterWard);
    }

    private Map<String, JsonNode> audit(UUID profileId) throws Exception {
        Map<String, Object> row = jdbc.queryForMap("SELECT before_data::text before_data,after_data::text after_data FROM audit_logs WHERE target_id=?", profileId);
        return Map.of("before", objectMapper.readTree((String) row.get("before_data")), "after", objectMapper.readTree((String) row.get("after_data")));
    }

    private void assertAuditContainsBothKeys(UUID profileId) {
        Map<String, Object> row = jdbc.queryForMap("SELECT before_data::text before_data,after_data::text after_data FROM audit_logs WHERE target_id=?", profileId);
        assertThat(row.get("before_data").toString()).contains("\"provinceCode\"").contains("\"wardCode\"");
        assertThat(row.get("after_data").toString()).contains("\"provinceCode\"").contains("\"wardCode\"");
    }

    private record Fixture(UUID userId, UUID profileId, String token, String province, String ward) {}
}
