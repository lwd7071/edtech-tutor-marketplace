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
@Table(name = "payout_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE payout_requests SET is_deleted = true WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
public class PayoutRequest extends BaseEntity {

    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "bank_account_id", nullable = false)
    private UUID bankAccountId;

    @Column(name = "amount_vnd", nullable = false)
    private long amountVnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PayoutStatus status;

    @Column(name = "teacher_note", columnDefinition = "text")
    private String teacherNote;

    @Column(name = "admin_note", columnDefinition = "text")
    private String adminNote;

    @Column(name = "bank_reference")
    private String bankReference;

    @Column(name = "proof_public_id")
    private String proofPublicId;

    @Column(name = "proof_url")
    private String proofUrl;

    @Column(name = "transferred_at")
    private Instant transferredAt;

    @Column(name = "processed_by")
    private UUID processedBy;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Version
    @Column(nullable = false)
    private long version;

    public static PayoutRequest create(UUID teacherId, UUID walletId, UUID bankAccountId, long amountVnd, String teacherNote) {
        if (amountVnd <= 0) {
            throw new BusinessException(ErrorCode.PAYOUT_INVALID_AMOUNT);
        }
        PayoutRequest p = new PayoutRequest();
        p.teacherId = Objects.requireNonNull(teacherId);
        p.walletId = Objects.requireNonNull(walletId);
        p.bankAccountId = Objects.requireNonNull(bankAccountId);
        p.amountVnd = amountVnd;
        p.teacherNote = teacherNote;
        p.status = PayoutStatus.PENDING;
        return p;
    }

    public void process(UUID adminId, Instant at) {
        if (status != PayoutStatus.PENDING) {
            throw new BusinessException(ErrorCode.PAYOUT_INVALID_STATE);
        }
        this.status = PayoutStatus.PROCESSING;
        this.processedBy = Objects.requireNonNull(adminId);
        this.processedAt = Objects.requireNonNull(at);
    }

    public void complete(UUID adminId, String bankReference, Instant transferredAt, String proofPublicId, String proofUrl, Instant at) {
        if (status != PayoutStatus.PENDING && status != PayoutStatus.PROCESSING) {
            throw new BusinessException(ErrorCode.PAYOUT_INVALID_STATE);
        }
        this.status = PayoutStatus.SUCCEEDED;
        this.bankReference = bankReference;
        this.transferredAt = transferredAt != null ? transferredAt : at;
        this.proofPublicId = proofPublicId;
        this.proofUrl = proofUrl;
        this.processedBy = Objects.requireNonNull(adminId);
        this.processedAt = Objects.requireNonNull(at);
    }

    public void reject(UUID adminId, String adminNote, Instant at) {
        if (status != PayoutStatus.PENDING && status != PayoutStatus.PROCESSING) {
            throw new BusinessException(ErrorCode.PAYOUT_INVALID_STATE);
        }
        this.status = PayoutStatus.REJECTED;
        this.adminNote = adminNote;
        this.processedBy = Objects.requireNonNull(adminId);
        this.processedAt = Objects.requireNonNull(at);
    }
}