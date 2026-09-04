package com.edtech.platform.finance.dto.response;

import com.edtech.platform.finance.domain.PayoutRequest;
import com.edtech.platform.finance.domain.PayoutStatus;

import java.time.Instant;
import java.util.UUID;

public record PayoutRequestView(
        UUID id,
        UUID teacherId,
        UUID walletId,
        UUID bankAccountId,
        long amountVnd,
        PayoutStatus status,
        String teacherNote,
        String adminNote,
        String bankReference,
        String proofUrl,
        Instant transferredAt,
        UUID processedBy,
        Instant processedAt,
        long version,
        Instant createdAt
) {
    public static PayoutRequestView from(PayoutRequest p) {
        return new PayoutRequestView(
                p.getId(),
                p.getTeacherId(),
                p.getWalletId(),
                p.getBankAccountId(),
                p.getAmountVnd(),
                p.getStatus(),
                p.getTeacherNote(),
                p.getAdminNote(),
                p.getBankReference(),
                p.getProofUrl(),
                p.getTransferredAt(),
                p.getProcessedBy(),
                p.getProcessedAt(),
                p.getVersion(),
                p.getCreatedAt()
        );
    }
}