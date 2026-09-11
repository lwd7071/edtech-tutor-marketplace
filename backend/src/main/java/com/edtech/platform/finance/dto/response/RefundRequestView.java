package com.edtech.platform.finance.dto.response;

import com.edtech.platform.finance.domain.RefundStatus;

import java.time.Instant;
import java.util.UUID;

public record RefundRequestView(
        UUID id,
        UUID studentPackageId,
        UUID studentId,
        String reason,
        int requestedSessions,
        int approvedSessions,
        Long refundAmountVnd,
        RefundStatus status,
        String adminNote,
        String bankName,
        String bankBin,
        String accountNumberMasked,
        String accountHolderName,
        String bankReference,
        String proofUrl,
        Instant transferredAt,
        UUID processedBy,
        Instant processedAt,
        long version,
        Instant createdAt
) {
    public RefundRequestView(UUID id, UUID studentPackageId, UUID studentId, String reason,
                             int requestedSessions, int approvedSessions, Long refundAmountVnd,
                             RefundStatus status, String adminNote, String bankName, String bankBin,
                             String accountNumberMasked, String accountHolderName, String bankReference,
                             String proofUrl, UUID processedBy, Instant processedAt, long version,
                             Instant createdAt) {
        this(id, studentPackageId, studentId, reason, requestedSessions, approvedSessions, refundAmountVnd,
                status, adminNote, bankName, bankBin, accountNumberMasked, accountHolderName, bankReference,
                proofUrl, null, processedBy, processedAt, version, createdAt);
    }
}
