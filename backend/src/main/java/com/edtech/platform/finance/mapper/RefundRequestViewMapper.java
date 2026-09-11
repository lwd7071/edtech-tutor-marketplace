package com.edtech.platform.finance.mapper;

import com.edtech.platform.finance.domain.RefundRequest;
import com.edtech.platform.finance.dto.response.RefundRequestView;
import com.edtech.platform.finance.security.AccountNumberProtector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefundRequestViewMapper {
    private final AccountNumberProtector accountNumbers;

    public RefundRequestView toView(RefundRequest refund) {
        String masked = accountNumbers.mask(accountNumbers.decrypt(refund.getAccountNumberEncrypted()));
        return new RefundRequestView(refund.getId(), refund.getStudentPackageId(), refund.getStudentId(),
                refund.getReason(), refund.getRequestedSessions(), refund.getApprovedSessions(),
                refund.getRefundAmountVnd(), refund.getStatus(), refund.getAdminNote(), refund.getBankName(),
                refund.getBankBin(), masked, refund.getAccountHolderName(), refund.getBankReference(),
                refund.getProofUrl(), refund.getTransferredAt(), refund.getProcessedBy(), refund.getProcessedAt(), refund.getVersion(),
                refund.getCreatedAt());
    }
}
