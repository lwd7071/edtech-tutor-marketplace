package com.edtech.platform.finance.domain;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "refund_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE refund_requests SET is_deleted = true WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
public class RefundRequest extends BaseEntity {

    @Column(name = "student_package_id", nullable = false)
    private UUID studentPackageId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(columnDefinition = "text")
    private String reason;

    @Column(name = "requested_sessions", nullable = false)
    private int requestedSessions;

    @Column(name = "approved_sessions", nullable = false)
    private int approvedSessions;

    @Column(name = "refund_amount_vnd")
    private Long refundAmountVnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RefundStatus status;

    @Column(name = "admin_note", columnDefinition = "text")
    private String adminNote;

    @Column(name = "bank_name", length = 150)
    private String bankName;

    @Column(name = "bank_bin", length = 20)
    private String bankBin;

    @Column(name = "account_number_encrypted", columnDefinition = "text")
    private String accountNumberEncrypted;

    @Column(name = "account_holder_name", length = 150)
    private String accountHolderName;

    @Column(name = "bank_reference")
    private String bankReference;

    @Column(name = "proof_public_id")
    private String proofPublicId;

    @Column(name = "proof_url", length = 500)
    private String proofUrl;

    @Column(name = "processed_by")
    private UUID processedBy;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Version
    @Column(nullable = false)
    private long version;

    public static RefundRequest create(
            UUID studentPackageId,
            UUID studentId,
            String reason,
            int requestedSessions,
            String bankName,
            String bankBin,
            String accountNumberEncrypted,
            String accountHolderName
    ) {
        if (requestedSessions <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        RefundRequest r = new RefundRequest();
        r.studentPackageId = Objects.requireNonNull(studentPackageId);
        r.studentId = Objects.requireNonNull(studentId);
        r.reason = reason;
        r.requestedSessions = requestedSessions;
        r.approvedSessions = 0;
        r.status = RefundStatus.PENDING;
        r.bankName = bankName;
        r.bankBin = bankBin;
        r.accountNumberEncrypted = accountNumberEncrypted;
        r.accountHolderName = accountHolderName;
        return r;
    }

    public void approve(UUID adminId, int approvedSessions, long refundAmountVnd, String adminNote, Instant at) {
        if (status != RefundStatus.PENDING) {
            throw new BusinessException(ErrorCode.REFUND_INVALID_STATE);
        }
        this.status = RefundStatus.APPROVED;
        this.approvedSessions = approvedSessions;
        this.refundAmountVnd = refundAmountVnd;
        this.adminNote = adminNote;
        this.processedBy = Objects.requireNonNull(adminId);
        this.processedAt = Objects.requireNonNull(at);
    }

    public void reject(UUID adminId, String adminNote, Instant at) {
        if (status != RefundStatus.PENDING && status != RefundStatus.APPROVED) {
            throw new BusinessException(ErrorCode.REFUND_INVALID_STATE);
        }
        this.status = RefundStatus.REJECTED;
        this.adminNote = adminNote;
        this.processedBy = Objects.requireNonNull(adminId);
        this.processedAt = Objects.requireNonNull(at);
    }

    public void complete(UUID adminId, String bankReference, String proofPublicId, String proofUrl, Instant at) {
        if (status != RefundStatus.APPROVED && status != RefundStatus.PENDING) {
            throw new BusinessException(ErrorCode.REFUND_INVALID_STATE);
        }
        this.status = RefundStatus.REFUNDED;
        this.bankReference = bankReference;
        this.proofPublicId = proofPublicId;
        this.proofUrl = proofUrl;
        this.processedBy = Objects.requireNonNull(adminId);
        this.processedAt = Objects.requireNonNull(at);
    }
}