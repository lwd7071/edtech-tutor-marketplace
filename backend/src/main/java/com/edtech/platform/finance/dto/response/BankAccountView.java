package com.edtech.platform.finance.dto.response;

import com.edtech.platform.finance.domain.TeacherBankAccount;
import com.edtech.platform.finance.util.AccountNumberCipher;

import java.time.Instant;
import java.util.UUID;

public record BankAccountView(
        UUID id,
        String bankBin,
        String bankName,
        String accountNumberMasked,
        String accountHolderName,
        boolean isVerified,
        boolean isDefault,
        Instant createdAt
) {
    public static BankAccountView from(TeacherBankAccount acc) {
        String decrypted = AccountNumberCipher.decrypt(acc.getAccountNumberEncrypted());
        String masked = AccountNumberCipher.mask(decrypted);
        return new BankAccountView(
                acc.getId(),
                acc.getBankBin(),
                acc.getBankName(),
                masked,
                acc.getAccountHolderName(),
                acc.isVerified(),
                acc.isDefault(),
                acc.getCreatedAt()
        );
    }
}