package com.edtech.platform.catalog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LearningRelationshipChecker {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Checks if a valid learning relationship exists between a student and a teacher.
     * This relies on a direct query to the 'student_packages' table.
     * TODO: Refactor to use a proper facade when Developer B implements the Enrollment/Booking module.
     *
     * @param teacherId the UUID of the teacher
     * @param studentId the UUID of the student
     * @return true if an ACTIVE or COMPLETED student package exists, false otherwise
     */
    public boolean hasValidRelationship(UUID teacherId, UUID studentId) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1 FROM student_packages 
                    WHERE teacher_id = ? 
                      AND student_id = ? 
                      AND status IN ('ACTIVE', 'COMPLETED')
                )
                """;

        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, teacherId, studentId);
        return exists != null && exists;
    }
}
