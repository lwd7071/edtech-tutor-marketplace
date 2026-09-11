package com.edtech.platform.enrollment.dto;

import com.edtech.platform.enrollment.domain.StudentPackage;
import java.time.Instant;
import java.util.UUID;

public record StudentOwnedPackageView(UUID id, UUID pricingPackageId, UUID invoiceId, String packageName,
        int totalSessions, int remainingSessions, int reservedSessions, int completedSessions, int refundedSessions,
        long purchasePriceVnd, Instant startsAt, Instant expiresAt, String status, String lockedReason, long version,
        TeacherRef teacher, SubjectRef subject) {
    public record TeacherRef(UUID id, String fullName, String avatarUrl) {}
    public record SubjectRef(UUID id, String name) {}
    public static StudentOwnedPackageView from(StudentPackage p, TeacherRef teacher, SubjectRef subject) {
        return new StudentOwnedPackageView(p.getId(), p.getPricingPackageId(), p.getInvoiceId(), p.getPackageNameSnapshot(),
                p.getTotalSessions(), p.getRemainingSessions(), p.getReservedSessions(), p.getCompletedSessions(),
                p.getRefundedSessions(), p.getPurchasePriceVnd(), p.getStartsAt(), p.getExpiresAt(), p.getStatus().name(),
                p.getLockedReason(), p.getVersion(), teacher, subject);
    }
}
