package com.edtech.platform.enrollment.facade;

import java.util.UUID;
import com.edtech.platform.enrollment.domain.StudentPackage;
import com.edtech.platform.enrollment.domain.StudentPackageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EnrollmentFacade {
    java.util.Map<UUID, java.time.Instant> latestPurchaseTimes(UUID teacherId);
    
    /**
     * Checks if a valid learning relationship exists between a teacher and a student.
     *
     * @param teacherId the UUID of the teacher
     * @param studentId the UUID of the student
     * @return true if an ACTIVE or COMPLETED student package exists, false otherwise
     */
    boolean hasValidRelationship(UUID teacherId, UUID studentId);

    /**
     * Checks if a student package exists for a given pricing package.
     *
     * @param pricingPackageId the UUID of the pricing package
     * @return true if a student package exists, false otherwise
     */
    boolean hasStudentPackage(UUID pricingPackageId);

    UUID activateStudentPackage(
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
            java.time.Instant paidAt
    );

    Page<StudentPackage> findStudentPackages(UUID studentId, StudentPackageStatus status, Pageable pageable);
    default Page<StudentPackage> findStudentPackages(UUID studentId, String status, Pageable pageable) {
        return findStudentPackages(studentId, status == null ? null : StudentPackageStatus.valueOf(status.toUpperCase()), pageable);
    }
    java.util.Optional<StudentPackage> findStudentPackage(UUID packageId, UUID studentId);

    com.edtech.platform.enrollment.facade.dto.EnrollmentPackageSnapshot inspect(UUID packageId, UUID studentId);
    void markRefundPending(UUID packageId);
    void restoreFromRefundPending(UUID packageId);
    void applyRefund(UUID packageId, int approvedSessions);
    void extendPackage(UUID packageId, java.time.Instant newExpiryDate);
}
