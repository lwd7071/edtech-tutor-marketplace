package com.edtech.platform.finance.dto.response;

import com.edtech.platform.finance.domain.RefundRequest;
import com.edtech.platform.finance.domain.RefundStatus;
import com.edtech.platform.finance.util.AccountNumberCipher;

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
        UUID processedBy,
        Instant processedAt,
        long version,
        Instant createdAt
) {
    public static RefundRequestView from(RefundRequest r) {
        String decrypted = AccountNumberCipher.decrypt(r.getAccountNumberEncrypted());
        String masked = AccountNumberCipher.mask(decrypted);
        return new RefundRequestView(
                r.getId(),
                r.getStudentPackageId(),
                r.getStudentId(),
                r.getReason(),
                r.getRequestedSessions(),
                r.getApprovedSessions(),
                r.getRefundAmountVnd(),
                r.getStatus(),
                r.getAdminNote(),
                r.getBankName(),
                r.getBankBin(),
                masked,
                r.getAccountHolderName(),
                r.getBankReference(),
                r.getProofUrl(),
                r.getProcessedBy(),
                r.getProcessedAt(),
                r.getVersion(),
                r.getCreatedAt()
        );
    }
}