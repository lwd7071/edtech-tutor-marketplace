package com.edtech.platform.finance.dto.response;

import com.edtech.platform.finance.domain.Wallet;
import java.util.UUID;

public record WalletView(
        UUID id,
        UUID teacherId,
        long pendingBalanceVnd,
        long availableBalanceVnd,
        long reservedBalanceVnd,
        long version,
        long heldBalanceVnd
) {
    public WalletView(UUID id, UUID teacherId, long pendingBalanceVnd, long availableBalanceVnd,
                      long reservedBalanceVnd, long version) {
        this(id, teacherId, pendingBalanceVnd, availableBalanceVnd, reservedBalanceVnd, version, 0);
    }

    public static WalletView from(Wallet wallet, long heldBalanceVnd) {
        return new WalletView(
                wallet.getId(),
                wallet.getTeacherId(),
                wallet.getPendingBalanceVnd(),
                wallet.getAvailableBalanceVnd(),
                wallet.getReservedBalanceVnd(),
                wallet.getVersion(),
                heldBalanceVnd
        );
    }

    public static WalletView from(Wallet wallet) { return from(wallet, 0); }
}
