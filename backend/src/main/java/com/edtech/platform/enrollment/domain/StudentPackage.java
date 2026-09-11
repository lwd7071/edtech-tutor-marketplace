package com.edtech.platform.enrollment.domain;

import com.edtech.platform.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "student_packages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE student_packages SET is_deleted = true WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
public class StudentPackage extends BaseEntity {
    @Column(name = "student_id", nullable = false) private UUID studentId;
    @Column(name = "teacher_id", nullable = false) private UUID teacherId;
    @Column(name = "subject_id", nullable = false) private UUID subjectId;
    @Column(name = "pricing_package_id", nullable = false) private UUID pricingPackageId;
    @Column(name = "invoice_id", nullable = false, unique = true) private UUID invoiceId;
    @Column(name = "package_name_snapshot", nullable = false, length = 150) private String packageNameSnapshot;
    @Column(name = "total_sessions", nullable = false) private int totalSessions;
    @Column(name = "remaining_sessions", nullable = false) private int remainingSessions;
    @Column(name = "reserved_sessions", nullable = false) private int reservedSessions;
    @Column(name = "completed_sessions", nullable = false) private int completedSessions;
    @Column(name = "refunded_sessions", nullable = false) private int refundedSessions;
    @Column(name = "purchase_price_vnd", nullable = false) private long purchasePriceVnd;
    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 2) private BigDecimal commissionRate;
    @Column(name = "starts_at", nullable = false) private Instant startsAt;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private StudentPackageStatus status;
    @Column(name = "locked_reason", columnDefinition = "text") private String lockedReason;
    @Version @Column(nullable = false) private long version;

    /** New-flow factory. PENDING_PAYMENT is deliberately unavailable: Invoice owns the waiting phase. */
    public static StudentPackage activateAfterPayment(
            UUID studentId, UUID teacherId, UUID subjectId, UUID pricingPackageId, UUID invoiceId,
            String packageName, int totalSessions, int durationDays, long purchasePriceVnd,
            BigDecimal commissionRate, Instant paidAt) {
        StudentPackage value = new StudentPackage();
        value.studentId = Objects.requireNonNull(studentId);
        value.teacherId = Objects.requireNonNull(teacherId);
        value.subjectId = Objects.requireNonNull(subjectId);
        value.pricingPackageId = Objects.requireNonNull(pricingPackageId);
        value.invoiceId = Objects.requireNonNull(invoiceId);
        value.packageNameSnapshot = Objects.requireNonNull(packageName);
        value.totalSessions = totalSessions;
        value.remainingSessions = totalSessions;
        value.purchasePriceVnd = purchasePriceVnd;
        value.commissionRate = Objects.requireNonNull(commissionRate);
        value.startsAt = Objects.requireNonNull(paidAt);
        if (durationDays <= 0) throw new IllegalArgumentException("durationDays must be positive");
        value.expiresAt = paidAt.plus(durationDays, ChronoUnit.DAYS);
        value.status = StudentPackageStatus.ACTIVE;
        value.validateCounterTotal();
        return value;
    }

    public void validateCounterTotal() {
        if (totalSessions <= 0 || remainingSessions < 0 || reservedSessions < 0
                || completedSessions < 0 || refundedSessions < 0
                || totalSessions != remainingSessions + reservedSessions + completedSessions + refundedSessions) {
            throw new IllegalArgumentException("student package counters are invalid");
        }
        if (purchasePriceVnd <= 0 || commissionRate.signum() < 0
                || commissionRate.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("student package financial snapshot is invalid");
        }
        if (!startsAt.isBefore(expiresAt)) throw new IllegalArgumentException("package dates are invalid");
    }

    public void reserveSession() { if (status != StudentPackageStatus.ACTIVE || remainingSessions <= 0) throw new IllegalStateException("package cannot reserve"); remainingSessions--; reservedSessions++; }
    public void releaseReservedSession() {
        if (reservedSessions <= 0) throw new IllegalStateException("no reserved session");
        reservedSessions--; remainingSessions++; validateCounterTotal();
    }
    public void completeReservedSession() { if (reservedSessions <= 0) throw new IllegalStateException("no reserved session"); reservedSessions--; completedSessions++; if (completedSessions + refundedSessions == totalSessions) status = StudentPackageStatus.COMPLETED; }
    public void lockExpired(Instant now) {
        if (status == StudentPackageStatus.ACTIVE && expiresAt != null && !expiresAt.isAfter(Objects.requireNonNull(now))) {
            status = StudentPackageStatus.LOCKED_EXPIRED; lockedReason = "SYSTEM_EXPIRY";
        }
    }

    public void markRefundPending() {
        if (status != StudentPackageStatus.ACTIVE && status != StudentPackageStatus.LOCKED_EXPIRED) {
            throw new IllegalStateException("package cannot request refund in status " + status);
        }
        if (remainingSessions <= 0) {
            throw new IllegalStateException("no remaining sessions to refund");
        }
        this.status = StudentPackageStatus.REFUND_PENDING;
    }

    public void restoreFromRefundPending(Instant now) {
        if (status != StudentPackageStatus.REFUND_PENDING) throw new IllegalStateException("package is not refund pending");
        this.status = (expiresAt != null && expiresAt.isAfter(Objects.requireNonNull(now)))
                ? StudentPackageStatus.ACTIVE : StudentPackageStatus.LOCKED_EXPIRED;
    }

    public void applyRefund(int approvedSessions, Instant now) {
        if (status != StudentPackageStatus.REFUND_PENDING) throw new IllegalStateException("package is not refund pending");
        if (approvedSessions <= 0 || approvedSessions > remainingSessions) {
            throw new IllegalArgumentException("invalid approved sessions: " + approvedSessions);
        }
        this.remainingSessions -= approvedSessions;
        this.refundedSessions += approvedSessions;
        if (this.remainingSessions == 0 && this.reservedSessions == 0) {
            this.status = (this.completedSessions == 0) ? StudentPackageStatus.REFUNDED : StudentPackageStatus.COMPLETED;
        } else {
            this.status = (expiresAt != null && expiresAt.isAfter(Objects.requireNonNull(now)))
                    ? StudentPackageStatus.ACTIVE
                    : StudentPackageStatus.LOCKED_EXPIRED;
        }
        validateCounterTotal();
    }

    public void extendExpiry(Instant newExpiryDate, Instant now) {
        if (status != StudentPackageStatus.LOCKED_EXPIRED) throw new IllegalStateException("package is not locked expired");
        if (newExpiryDate == null || !newExpiryDate.isAfter(Objects.requireNonNull(now))) {
            throw new IllegalArgumentException("new expiry date must be in the future");
        }
        this.expiresAt = newExpiryDate;
        this.status = StudentPackageStatus.ACTIVE;
        this.lockedReason = null;
    }
}
