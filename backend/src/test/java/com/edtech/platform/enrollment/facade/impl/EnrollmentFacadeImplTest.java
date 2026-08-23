package com.edtech.platform.enrollment.facade.impl;

import com.edtech.platform.enrollment.facade.EnrollmentFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentFacadeImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private EnrollmentFacadeImpl enrollmentFacade;

    private UUID teacherId;
    private UUID studentId;
    private UUID pricingPackageId;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        pricingPackageId = UUID.randomUUID();
    }

    @Test
    void hasValidRelationship_shouldReturnTrue_whenExists() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), eq(teacherId), eq(studentId)))
                .thenReturn(true);

        boolean result = enrollmentFacade.hasValidRelationship(teacherId, studentId);

        assertTrue(result);
    }

    @Test
    void hasValidRelationship_shouldReturnFalse_whenNotExists() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), eq(teacherId), eq(studentId)))
                .thenReturn(false);

        boolean result = enrollmentFacade.hasValidRelationship(teacherId, studentId);

        assertFalse(result);
    }
    
    @Test
    void hasStudentPackage_shouldReturnTrue_whenExists() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), eq(pricingPackageId)))
                .thenReturn(true);

        boolean result = enrollmentFacade.hasStudentPackage(pricingPackageId);

        assertTrue(result);
    }
    
    @Test
    void hasStudentPackage_shouldReturnFalse_whenNotExists() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), eq(pricingPackageId)))
                .thenReturn(false);

        boolean result = enrollmentFacade.hasStudentPackage(pricingPackageId);

        assertFalse(result);
    }
}
