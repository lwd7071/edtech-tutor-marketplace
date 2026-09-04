package com.edtech.platform.enrollment.facade.impl;

import com.edtech.platform.enrollment.facade.EnrollmentFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.edtech.platform.enrollment.domain.StudentPackageStatus;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentFacadeImpl implements EnrollmentFacade {

    private final com.edtech.platform.enrollment.repository.StudentPackageRepository studentPackageRepository;

    @Override
    public boolean hasValidRelationship(UUID teacherId, UUID studentId) {
        return studentPackageRepository.existsByTeacherIdAndStudentIdAndStatusInAndDeletedFalse(
                teacherId,
                studentId,
                List.of(StudentPackageStatus.ACTIVE, StudentPackageStatus.COMPLETED)
        );
    }

    @Override
    public boolean hasStudentPackage(UUID pricingPackageId) {
        return studentPackageRepository.existsByPricingPackageIdAndDeletedFalse(pricingPackageId);
    }

    @Override
    public UUID activateStudentPackage(
            UUID studentId,
            UUID teacherId,
            UUID subjectId,
            UUID pricingPackageId,
            UUID invoiceId,
            String packageName,
            int totalSessions,
            int durationDays,
            long purchasePriceVnd,
            java.math.BigDecimal commissionRate,
            java.time.Instant paidAt) {
        com.edtech.platform.enrollment.domain.StudentPackage studentPackage =
                com.edtech.platform.enrollment.domain.StudentPackage.activateAfterPayment(
                        studentId,
                        teacherId,
                        subjectId,
                        pricingPackageId,
                        invoiceId,
                        packageName,
                        totalSessions,
                        durationDays,
                        purchasePriceVnd,
                        commissionRate,
                        paidAt
                );
        studentPackageRepository.save(studentPackage);
        return studentPackage.getId();
    }

    @Override
    public Page<com.edtech.platform.enrollment.domain.StudentPackage> findStudentPackages(UUID studentId, StudentPackageStatus status, Pageable pageable) {
        return status == null ? studentPackageRepository.findByStudentId(studentId, pageable)
                : studentPackageRepository.findByStudentIdAndStatus(studentId, status, pageable);
    }

    @Override
    public java.util.Optional<com.edtech.platform.enrollment.domain.StudentPackage> findStudentPackage(UUID packageId, UUID studentId) {
        return studentPackageRepository.findByIdAndStudentId(packageId, studentId);
    }

    @Override
    public com.edtech.platform.enrollment.facade.dto.EnrollmentPackageSnapshot inspect(UUID packageId, UUID studentId) {
        var opt = (studentId == null)
                ? studentPackageRepository.findById(packageId)
                : studentPackageRepository.findByIdAndStudentId(packageId, studentId);
        return opt.map(com.edtech.platform.enrollment.facade.dto.EnrollmentPackageSnapshot::from).orElse(null);
    }

    @Override
    public void markRefundPending(UUID packageId) {
        var pkg = studentPackageRepository.findByIdForUpdate(packageId)
                .orElseThrow(() -> new IllegalArgumentException("package not found: " + packageId));
        pkg.markRefundPending();
    }

    @Override
    public void restoreFromRefundPending(UUID packageId) {
        var pkg = studentPackageRepository.findByIdForUpdate(packageId)
                .orElseThrow(() -> new IllegalArgumentException("package not found: " + packageId));
        pkg.restoreFromRefundPending();
    }

    @Override
    public void applyRefund(UUID packageId, int approvedSessions) {
        var pkg = studentPackageRepository.findByIdForUpdate(packageId)
                .orElseThrow(() -> new IllegalArgumentException("package not found: " + packageId));
        pkg.applyRefund(approvedSessions);
    }

    @Override
    public void extendPackage(UUID packageId, java.time.Instant newExpiryDate) {
        var pkg = studentPackageRepository.findByIdForUpdate(packageId)
                .orElseThrow(() -> new IllegalArgumentException("package not found: " + packageId));
        pkg.extendExpiry(newExpiryDate);
    }
}
