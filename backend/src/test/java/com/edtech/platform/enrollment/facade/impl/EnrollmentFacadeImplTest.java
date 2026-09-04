package com.edtech.platform.enrollment.facade.impl;

import com.edtech.platform.enrollment.domain.StudentPackageStatus;
import com.edtech.platform.enrollment.repository.StudentPackageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentFacadeImplTest {

    @Mock
    private StudentPackageRepository studentPackageRepository;

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
        when(studentPackageRepository.existsByTeacherIdAndStudentIdAndStatusInAndDeletedFalse(
                eq(teacherId), eq(studentId), eq(List.of(StudentPackageStatus.ACTIVE, StudentPackageStatus.COMPLETED))))
                .thenReturn(true);

        boolean result = enrollmentFacade.hasValidRelationship(teacherId, studentId);

        assertTrue(result);
        verify(studentPackageRepository).existsByTeacherIdAndStudentIdAndStatusInAndDeletedFalse(
                teacherId, studentId, List.of(StudentPackageStatus.ACTIVE, StudentPackageStatus.COMPLETED));
    }

    @Test
    void hasValidRelationship_shouldReturnFalse_whenNotExists() {
        when(studentPackageRepository.existsByTeacherIdAndStudentIdAndStatusInAndDeletedFalse(
                eq(teacherId), eq(studentId), eq(List.of(StudentPackageStatus.ACTIVE, StudentPackageStatus.COMPLETED))))
                .thenReturn(false);

        boolean result = enrollmentFacade.hasValidRelationship(teacherId, studentId);

        assertFalse(result);
    }
    
    @Test
    void hasStudentPackage_shouldReturnTrue_whenExists() {
        when(studentPackageRepository.existsByPricingPackageIdAndDeletedFalse(eq(pricingPackageId)))
                .thenReturn(true);

        boolean result = enrollmentFacade.hasStudentPackage(pricingPackageId);

        assertTrue(result);
        verify(studentPackageRepository).existsByPricingPackageIdAndDeletedFalse(pricingPackageId);
    }
    
    @Test
    void hasStudentPackage_shouldReturnFalse_whenNotExists() {
        when(studentPackageRepository.existsByPricingPackageIdAndDeletedFalse(eq(pricingPackageId)))
                .thenReturn(false);

        boolean result = enrollmentFacade.hasStudentPackage(pricingPackageId);

        assertFalse(result);
    }
}
