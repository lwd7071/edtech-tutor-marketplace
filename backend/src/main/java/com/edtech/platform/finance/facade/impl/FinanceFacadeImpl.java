package com.edtech.platform.finance.facade.impl;

import com.edtech.platform.finance.domain.BalanceBucket;
import com.edtech.platform.finance.domain.LedgerDirection;
import com.edtech.platform.finance.domain.LedgerEntry;
import com.edtech.platform.finance.domain.LedgerEntryType;
import com.edtech.platform.finance.domain.Wallet;
import com.edtech.platform.finance.facade.FinanceFacade;
import com.edtech.platform.finance.repository.LedgerEntryRepository;
import com.edtech.platform.finance.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceFacadeImpl implements FinanceFacade {

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    @Override
    @Transactional
    public void creditTeacherPendingBalance(UUID teacherId, long netAmount, UUID invoiceId, String invoiceNumber) {
        walletRepository.ensureForTeacher(teacherId);
        Wallet wallet = walletRepository.findByTeacherIdForUpdate(teacherId)
                .orElseThrow(() -> new IllegalStateException("Wallet unavailable for teacher " + teacherId));

        wallet.creditPending(netAmount);
        walletRepository.save(wallet);

        LedgerEntry ledgerEntry = new LedgerEntry(
                wallet.getId(),
                LedgerEntryType.PACKAGE_FUNDED,
                netAmount,
                BalanceBucket.PENDING,
                LedgerDirection.CREDIT,
                "INVOICE",
                invoiceId,
                "WH-PAY-" + invoiceId.toString(),
                "Thanh toan goi hoc cho hoa don " + invoiceNumber
        );
        ledgerEntryRepository.append(ledgerEntry);
    }

    @Override @Transactional
    public void settleBookingSession(UUID teacherId, UUID bookingId, long amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        String pendingKey = "booking:" + bookingId + ":settlement:pending";
        String availableKey = "booking:" + bookingId + ":settlement:available";
        if (ledgerEntryRepository.existsByIdempotencyKey(availableKey)) return;
        walletRepository.ensureForTeacher(teacherId);
        Wallet wallet = walletRepository.findByTeacherIdForUpdate(teacherId).orElseThrow();
        wallet.debitPending(amount); wallet.creditAvailable(amount); walletRepository.save(wallet);
        ledgerEntryRepository.append(new LedgerEntry(wallet.getId(), LedgerEntryType.SESSION_RELEASE_PENDING, amount, BalanceBucket.PENDING, LedgerDirection.DEBIT, "BOOKING", bookingId, pendingKey, "Booking settlement release"));
        ledgerEntryRepository.append(new LedgerEntry(wallet.getId(), LedgerEntryType.SESSION_CREDIT_AVAILABLE, amount, BalanceBucket.AVAILABLE, LedgerDirection.CREDIT, "BOOKING", bookingId, availableKey, "Booking settlement available"));
    }
}
