package com.edtech.platform.finance.dto.response;

import com.edtech.platform.finance.domain.Wallet;
import java.util.UUID;

public record WalletView(
        UUID id,
        UUID teacherId,
        long pendingBalanceVnd,
        long availableBalanceVnd,
        long reservedBalanceVnd,
        long version
) {
    public static WalletView from(Wallet wallet) {
        return new WalletView(
                wallet.getId(),
                wallet.getTeacherId(),
                wallet.getPendingBalanceVnd(),
                wallet.getAvailableBalanceVnd(),
                wallet.getReservedBalanceVnd(),
                wallet.getVersion()
        );
    }
}
