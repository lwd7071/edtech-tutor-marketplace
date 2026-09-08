package com.edtech.platform.finance.mapper;

import com.edtech.platform.finance.domain.TeacherBankAccount;
import com.edtech.platform.finance.dto.response.BankAccountView;
import com.edtech.platform.finance.security.AccountNumberProtector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BankAccountViewMapper {
    private final AccountNumberProtector accountNumbers;

    public BankAccountView toView(TeacherBankAccount account) {
        String masked = accountNumbers.mask(accountNumbers.decrypt(account.getAccountNumberEncrypted()));
        return new BankAccountView(account.getId(), account.getBankBin(), account.getBankName(), masked,
                account.getAccountHolderName(), account.isVerified(), account.isDefault(), account.getCreatedAt());
    }
}
