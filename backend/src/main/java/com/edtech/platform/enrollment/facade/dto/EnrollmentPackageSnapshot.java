package com.edtech.platform.enrollment.facade.dto;

import com.edtech.platform.enrollment.domain.StudentPackage;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record EnrollmentPackageSnapshot(
        UUID id,
        UUID studentId,
        UUID teacherId,
        UUID subjectId,
        String status,
        int totalSessions,
        int remainingSessions,
        int reservedSessions,
        int completedSessions,
        int refundedSessions,
        long purchasePriceVnd,
        BigDecimal commissionRate,
        Instant startsAt,
        Instant expiresAt,
        long version
) {
    public static EnrollmentPackageSnapshot from(StudentPackage p) {
        return new EnrollmentPackageSnapshot(
                p.getId(),
                p.getStudentId(),
                p.getTeacherId(),
                p.getSubjectId(),
                p.getStatus().name(),
                p.getTotalSessions(),
                p.getRemainingSessions(),
                p.getReservedSessions(),
                p.getCompletedSessions(),
                p.getRefundedSessions(),
                p.getPurchasePriceVnd(),
                p.getCommissionRate(),
                p.getStartsAt(),
                p.getExpiresAt(),
                p.getVersion()
        );
    }
}