package com.edtech.platform.catalog.repository;

import com.edtech.platform.catalog.domain.PackageStatus;
import com.edtech.platform.catalog.domain.PricingPackage;
import com.edtech.platform.common.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PricingPackageSoftDeleteIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private PricingPackageRepository repository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void versionedDeleteUpdatesFlagAndDefaultQueryHidesRow() {
        UUID userId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO users (id,email,password_hash,full_name,role,status) VALUES (?,?,?,?,?,?)",
                userId, userId + "@example.test", "hash", "Teacher", "TEACHER", "ACTIVE");
        jdbcTemplate.update("INSERT INTO teacher_profiles (id,user_id,profile_status) VALUES (?,?,?)",
                teacherId, userId, "APPROVED");
        jdbcTemplate.update("INSERT INTO subjects (id,code,name,slug) VALUES (?,?,?,?)",
                subjectId, "SUB-" + subjectId, "Subject", "subject-" + subjectId);

        PricingPackage pricingPackage = repository.saveAndFlush(PricingPackage.builder()
                .teacherId(teacherId).subjectId(subjectId).name("Gói học")
                .totalSessions(5).durationDays(30).priceVnd(500_000L)
                .sessionDurationMinutes(60).status(PackageStatus.DRAFT).build());
        UUID packageId = pricingPackage.getId();

        repository.delete(pricingPackage);
        repository.flush();
        entityManager.clear();

        assertThat(jdbcTemplate.queryForObject(
                "SELECT is_deleted FROM pricing_packages WHERE id = ?", Boolean.class, packageId)).isTrue();
        assertThat(repository.findById(packageId)).isEmpty();
    }
}
