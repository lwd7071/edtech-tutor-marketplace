package com.edtech.platform.catalog.service;

import com.edtech.platform.catalog.domain.PackageStatus;
import com.edtech.platform.catalog.domain.PricingPackage;
import com.edtech.platform.catalog.dto.ChangePackageStatusRequest;
import com.edtech.platform.catalog.dto.PricingPackageView;
import com.edtech.platform.catalog.dto.UpsertPricingPackageRequest;
import com.edtech.platform.catalog.repository.PricingPackageRepository;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.enrollment.facade.EnrollmentFacade;
import com.edtech.platform.subject.facade.SubjectFacade;
import com.edtech.platform.subject.facade.dto.SubjectSnapshot;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.teacher.facade.dto.TeacherSnapshot;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PricingPackageServiceTest {

    @Mock private PricingPackageRepository pricingPackageRepository;
    @Mock private TeacherFacade teacherFacade;
    @Mock private SubjectFacade subjectFacade;
    @Mock private EnrollmentFacade enrollmentFacade;
    @Mock private EntityManager entityManager;
    @Mock private CacheManager cacheManager;
    @Mock private Cache cache;

    @InjectMocks
    private PricingPackageService pricingPackageService;

    private UUID userId;
    private UUID teacherId;
    private UUID subjectId;
    private TeacherSnapshot approvedTeacher;
    private TeacherSnapshot notApprovedTeacher;
    private SubjectSnapshot activeSubject;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        teacherId = UUID.randomUUID();
        subjectId = UUID.randomUUID();

        approvedTeacher = new TeacherSnapshot(teacherId, userId, "APPROVED", true, true, "Teacher A", null, null, 0, true, false, java.util.List.of(), null, null);
        notApprovedTeacher = new TeacherSnapshot(teacherId, userId, "PENDING_APPROVAL", false, false, "Teacher B", null, null, 0, true, false, java.util.List.of(), null, null);
        activeSubject = new SubjectSnapshot(subjectId, "MATH01", "Mathematics", "HIGH_SCHOOL", true);

        lenient().when(cacheManager.getCache("TEACHER_PUBLIC_PROFILE")).thenReturn(cache);
    }

    // ── Slice 1: createPackage — teacher not approved → throws ────────────────

    @ParameterizedTest
    @ValueSource(strings = {"DRAFT", "PENDING_APPROVAL", "REJECTED"})
    void createPackage_throwsTeacherNotApproved_whenStatusNotApproved(String status) {
        when(teacherFacade.getTeacherByUserId(userId)).thenReturn(teacherWithStatus(status));

        UpsertPricingPackageRequest request = new UpsertPricingPackageRequest(
                subjectId, "Basic Pack", "desc", 10, 30, 500000L, 60, PackageStatus.ACTIVE);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pricingPackageService.createPackage(userId, request));

        assertEquals(ErrorCode.TEACHER_NOT_APPROVED, ex.getErrorCode());
    }

    @Test
    void createPackage_throwsProfileNotFound_whenTeacherProfileIsMissing() {
        when(teacherFacade.getTeacherByUserId(userId)).thenReturn(null);
        UpsertPricingPackageRequest request = packageRequest(PackageStatus.ACTIVE);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pricingPackageService.createPackage(userId, request));

        assertEquals(ErrorCode.TEACHER_PROFILE_NOT_FOUND, ex.getErrorCode());
        verifyNoInteractions(pricingPackageRepository);
    }

    // ── Slice 2: createPackage — subject not assigned → throws ────────────────

    @Test
    void createPackage_throwsSubjectNotAssigned_whenSubjectNotAssigned() {
        when(teacherFacade.getTeacherByUserId(userId)).thenReturn(approvedTeacher);
        when(subjectFacade.getSubject(subjectId)).thenReturn(activeSubject);
        when(teacherFacade.hasAssignedSubject(teacherId, subjectId)).thenReturn(false);

        UpsertPricingPackageRequest request = new UpsertPricingPackageRequest(
                subjectId, "Basic Pack", "desc", 10, 30, 500000L, 60, PackageStatus.ACTIVE);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pricingPackageService.createPackage(userId, request));

        assertEquals(ErrorCode.SUBJECT_NOT_ASSIGNED, ex.getErrorCode());
    }

    // ── Slice 3: createPackage — happy path ───────────────────────────────────

    @Test
    void createPackage_success_whenTeacherApprovedAndSubjectAssigned() {
        when(teacherFacade.getTeacherByUserId(userId)).thenReturn(approvedTeacher);
        when(subjectFacade.getSubject(subjectId)).thenReturn(activeSubject);
        when(teacherFacade.hasAssignedSubject(teacherId, subjectId)).thenReturn(true);
        when(subjectFacade.getSubject(subjectId)).thenReturn(activeSubject);

        PricingPackage savedPkg = PricingPackage.builder()
                .teacherId(teacherId)
                .subjectId(subjectId)
                .name("Basic Pack")
                .description("desc")
                .totalSessions(10)
                .durationDays(30)
                .priceVnd(500000L)
                .sessionDurationMinutes(60)
                .status(PackageStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(savedPkg, "id", UUID.randomUUID());

        when(pricingPackageRepository.save(any(PricingPackage.class))).thenReturn(savedPkg);

        UpsertPricingPackageRequest request = new UpsertPricingPackageRequest(
                subjectId, "Basic Pack", "desc", 10, 30, 500000L, 60, PackageStatus.ACTIVE);

        PricingPackageView result = pricingPackageService.createPackage(userId, request);

        assertNotNull(result);
        assertEquals(subjectId, result.subjectId());
        verify(pricingPackageRepository).save(any(PricingPackage.class));
    }

    // ── Slice 4: changeStatus — package immutable after purchase ─────────────

    @Test
    void changeStatus_throwsPackageInvalidState_whenHasStudentsAndNotInactive() {
        UUID packageId = UUID.randomUUID();

        PricingPackage pkg = PricingPackage.builder()
                .teacherId(teacherId)
                .subjectId(subjectId)
                .name("Pack")
                .description("")
                .totalSessions(5)
                .durationDays(30)
                .priceVnd(100000L)
                .sessionDurationMinutes(45)
                .status(PackageStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(pkg, "id", packageId);

        when(teacherFacade.getTeacherByUserId(userId)).thenReturn(approvedTeacher);
        when(pricingPackageRepository.findById(packageId)).thenReturn(Optional.of(pkg));
        when(enrollmentFacade.hasStudentPackage(packageId)).thenReturn(true);

        // Trying to set status to ACTIVE when already has students — not INACTIVE
        ChangePackageStatusRequest req = new ChangePackageStatusRequest(PackageStatus.ACTIVE);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pricingPackageService.changeStatus(userId, packageId, req));

        assertEquals(ErrorCode.PACKAGE_INVALID_STATE, ex.getErrorCode());
    }

    // ── Slice 5: changeStatus — can set to INACTIVE even with students ────────

    @Test
    void changeStatus_allowsInactive_whenHasStudents() {
        UUID packageId = UUID.randomUUID();

        PricingPackage pkg = PricingPackage.builder()
                .teacherId(teacherId)
                .subjectId(subjectId)
                .name("Pack")
                .description("")
                .totalSessions(5)
                .durationDays(30)
                .priceVnd(100000L)
                .sessionDurationMinutes(45)
                .status(PackageStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(pkg, "id", packageId);

        when(teacherFacade.getTeacherByUserId(userId)).thenReturn(notApprovedTeacher);
        when(pricingPackageRepository.findById(packageId)).thenReturn(Optional.of(pkg));
        when(enrollmentFacade.hasStudentPackage(packageId)).thenReturn(true);
        when(pricingPackageRepository.save(any())).thenReturn(pkg);
        when(subjectFacade.getSubject(subjectId)).thenReturn(activeSubject);

        ChangePackageStatusRequest req = new ChangePackageStatusRequest(PackageStatus.INACTIVE);

        assertDoesNotThrow(() -> pricingPackageService.changeStatus(userId, packageId, req));
        verify(pricingPackageRepository).save(pkg);
    }

    @ParameterizedTest
    @ValueSource(strings = {"DRAFT", "PENDING_APPROVAL", "REJECTED"})
    void updatePackage_throwsTeacherNotApproved_beforeMutatingPackage(String status) {
        UUID packageId = UUID.randomUUID();
        when(teacherFacade.getTeacherByUserId(userId)).thenReturn(teacherWithStatus(status));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pricingPackageService.updatePackage(userId, packageId, packageRequest(PackageStatus.INACTIVE)));

        assertEquals(ErrorCode.TEACHER_NOT_APPROVED, ex.getErrorCode());
        verify(pricingPackageRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"DRAFT", "PENDING_APPROVAL", "REJECTED"})
    void changeStatus_blocksActivation_whenTeacherNotApproved(String status) {
        UUID packageId = UUID.randomUUID();
        PricingPackage pkg = ownedPackage(packageId, PackageStatus.INACTIVE);
        when(teacherFacade.getTeacherByUserId(userId)).thenReturn(teacherWithStatus(status));
        when(pricingPackageRepository.findById(packageId)).thenReturn(Optional.of(pkg));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pricingPackageService.changeStatus(
                        userId, packageId, new ChangePackageStatusRequest(PackageStatus.ACTIVE)));

        assertEquals(ErrorCode.TEACHER_NOT_APPROVED, ex.getErrorCode());
        assertEquals(PackageStatus.INACTIVE, pkg.getStatus());
        verify(pricingPackageRepository, never()).save(any());
    }

    @Test
    void changeStatus_throwsProfileNotFound_whenTeacherProfileIsMissing() {
        when(teacherFacade.getTeacherByUserId(userId)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pricingPackageService.changeStatus(
                        userId, UUID.randomUUID(), new ChangePackageStatusRequest(PackageStatus.INACTIVE)));

        assertEquals(ErrorCode.TEACHER_PROFILE_NOT_FOUND, ex.getErrorCode());
    }

    private UpsertPricingPackageRequest packageRequest(PackageStatus status) {
        return new UpsertPricingPackageRequest(
                subjectId, "Basic Pack", "desc", 10, 30, 500000L, 60, status);
    }

    private PricingPackage ownedPackage(UUID packageId, PackageStatus status) {
        PricingPackage pkg = PricingPackage.builder()
                .teacherId(teacherId)
                .subjectId(subjectId)
                .name("Pack")
                .description("")
                .totalSessions(5)
                .durationDays(30)
                .priceVnd(100000L)
                .sessionDurationMinutes(45)
                .status(status)
                .build();
        ReflectionTestUtils.setField(pkg, "id", packageId);
        return pkg;
    }

    private TeacherSnapshot teacherWithStatus(String status) {
        return new TeacherSnapshot(
                teacherId, userId, status, false, false, "Teacher", null, null, 0,
                true, false, java.util.List.of(), null, null);
    }
}
