package com.edtech.platform.enrollment.facade.impl;

import com.edtech.platform.enrollment.facade.EnrollmentFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentFacadeImpl implements EnrollmentFacade {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public boolean hasValidRelationship(UUID teacherId, UUID studentId) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1 FROM student_packages 
                    WHERE teacher_id = ? 
                      AND student_id = ? 
                      AND status IN ('ACTIVE', 'COMPLETED')
                      AND is_deleted = false
                )
                """;

        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, teacherId, studentId);
        return exists != null && exists;
    }

    @Override
    public boolean hasStudentPackage(UUID pricingPackageId) {
        String sql = "SELECT EXISTS (SELECT 1 FROM student_packages WHERE pricing_package_id = ? AND is_deleted = false)";
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, pricingPackageId);
        return exists != null && exists;
    }
}
