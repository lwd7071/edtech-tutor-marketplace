package com.edtech.platform.catalog.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StudentPackageChecker {
    private final JdbcTemplate jdbcTemplate;

    // TODO: Refactor to use StudentPackageLookupPort once module B is ready.
    public boolean hasStudentPackage(UUID pricingPackageId) {
        String sql = "SELECT COUNT(1) FROM student_packages WHERE pricing_package_id = ? AND is_deleted = false LIMIT 1";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, pricingPackageId);
        return count != null && count > 0;
    }
}
