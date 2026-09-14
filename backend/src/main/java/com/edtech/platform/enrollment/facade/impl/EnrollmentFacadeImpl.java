package com.edtech.platform.enrollment.facade.impl;

import com.edtech.platform.enrollment.facade.EnrollmentFacade;
import com.edtech.platform.enrollment.domain.StudentPackageStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import java.time.Clock;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentFacadeImpl implements EnrollmentFacade {

    private final com.edtech.platform.enrollment.repository.StudentPackageRepository studentPackageRepository;
    private final Clock clock;

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public java.util.Map<UUID, java.time.Instant> latestPurchaseTimes(UUID teacherId) {
        var result = new java.util.HashMap<UUID, java.time.Instant>();
        for (var row : studentPackageRepository.latestPurchaseTimes(teacherId)) result.put((UUID) row[0], (java.time.Instant) row[1]);
        return result;
    }

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
    public com.edtech.platform.enrollment.facade.dto.EnrollmentPackageSnapshot inspect(UUID packageId, UUID studentId) {
        var opt = (studentId == null)
                ? studentPackageRepository.findById(packageId)
                : studentPackageRepository.findByIdAndStudentId(packageId, studentId);
        return opt.map(com.edtech.platform.enrollment.facade.dto.EnrollmentPackageSnapshot::from).orElse(null);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public com.edtech.platform.enrollment.facade.dto.EnrollmentPackageSnapshot lockOwnedPackageForFinance(UUID packageId, UUID studentId, long expectedVersion) {
        var pkg = studentPackageRepository.findByIdAndStudentIdForUpdate(packageId, studentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (pkg.getVersion() != expectedVersion) {
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
        }
        return com.edtech.platform.enrollment.facade.dto.EnrollmentPackageSnapshot.from(pkg);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public com.edtech.platform.enrollment.facade.dto.EnrollmentPackageSnapshot lockPackageForFinance(UUID packageId, long expectedVersion) {
        var pkg = studentPackageRepository.findByIdForUpdate(packageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (pkg.getVersion() != expectedVersion) {
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
        }
        return com.edtech.platform.enrollment.facade.dto.EnrollmentPackageSnapshot.from(pkg);
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
        pkg.restoreFromRefundPending(clock.instant());
    }

    @Override
    public void applyRefund(UUID packageId, int approvedSessions) {
        var pkg = studentPackageRepository.findByIdForUpdate(packageId)
                .orElseThrow(() -> new IllegalArgumentException("package not found: " + packageId));
        pkg.applyRefund(approvedSessions, clock.instant());
    }

    @Override
    public void extendPackage(UUID packageId, java.time.Instant newExpiryDate) {
        var pkg = studentPackageRepository.findByIdForUpdate(packageId)
                .orElseThrow(() -> new IllegalArgumentException("package not found: " + packageId));
        pkg.extendExpiry(newExpiryDate, clock.instant());
    }
}
