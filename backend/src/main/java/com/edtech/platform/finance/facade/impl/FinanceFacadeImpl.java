package com.edtech.platform.finance.facade.impl;

import com.edtech.platform.finance.domain.BalanceBucket;
import com.edtech.platform.finance.domain.LedgerDirection;
import com.edtech.platform.finance.domain.LedgerEntry;
import com.edtech.platform.finance.domain.LedgerEntryType;
import com.edtech.platform.finance.domain.Wallet;
import com.edtech.platform.finance.facade.FinanceFacade;
import com.edtech.platform.finance.repository.LedgerEntryRepository;
import com.edtech.platform.finance.repository.WalletRepository;
import com.edtech.platform.finance.repository.PlatformLedgerEntryRepository;
import com.edtech.platform.finance.domain.PlatformLedgerEntry;
import com.edtech.platform.finance.domain.PlatformLedgerBucket;
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
    private final PlatformLedgerEntryRepository platformLedger;

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

    @Override @Transactional
    public void holdBookingSession(UUID teacherId, UUID bookingId, long amount) {
        if (amount < 0) throw new IllegalArgumentException("amount cannot be negative");
        if (amount == 0) return;
        if (platformLedger.existsByIdempotencyKey("booking:"+bookingId+":escrow:credit")) {
            requireHeldAmount(bookingId, amount);
            return;
        }
        walletRepository.ensureForTeacher(teacherId);
        Wallet wallet=walletRepository.findByTeacherIdForUpdate(teacherId).orElseThrow();
        wallet.debitPending(amount); walletRepository.save(wallet);
        ledgerEntryRepository.append(new LedgerEntry(wallet.getId(), LedgerEntryType.SESSION_ESCROW_HELD,
                amount, BalanceBucket.PENDING, LedgerDirection.DEBIT, "BOOKING", bookingId,
                "booking:"+bookingId+":escrow:pending", "Booking amount moved into escrow"));
        platformLedger.save(new PlatformLedgerEntry(bookingId, PlatformLedgerBucket.ESCROW, LedgerDirection.CREDIT, amount, "booking:"+bookingId+":escrow:credit", "Booking amount held in escrow"));
    }

    @Override @Transactional
    public void releaseHeldBookingSession(UUID teacherId, UUID bookingId, long amount) {
        if (amount < 0) throw new IllegalArgumentException("amount cannot be negative");
        if (amount == 0 || platformLedger.existsByIdempotencyKey("booking:"+bookingId+":escrow:debit")) return;
        requireHeldAmount(bookingId, amount);
        if (platformLedger.existsByIdempotencyKey("booking:"+bookingId+":revenue"))
            throw new IllegalStateException("Booking escrow has already been retained");
        walletRepository.ensureForTeacher(teacherId);
        Wallet wallet=walletRepository.findByTeacherIdForUpdate(teacherId).orElseThrow();
        wallet.creditAvailable(amount); walletRepository.save(wallet);
        platformLedger.save(new PlatformLedgerEntry(bookingId, PlatformLedgerBucket.ESCROW, LedgerDirection.DEBIT, amount, "booking:"+bookingId+":escrow:debit", "Release held booking amount"));
        ledgerEntryRepository.append(new LedgerEntry(wallet.getId(), LedgerEntryType.SESSION_ESCROW_RELEASED,
                amount, BalanceBucket.AVAILABLE, LedgerDirection.CREDIT, "BOOKING", bookingId,
                "booking:"+bookingId+":escrow:available", "Booking escrow released to teacher"));
    }

    @Override @Transactional
    public void retainHeldBookingSession(UUID bookingId, long amount) {
        if (amount < 0) throw new IllegalArgumentException("amount cannot be negative");
        if (amount == 0 || platformLedger.existsByIdempotencyKey("booking:"+bookingId+":revenue")) return;
        requireHeldAmount(bookingId, amount);
        if (platformLedger.existsByIdempotencyKey("booking:"+bookingId+":escrow:debit"))
            throw new IllegalStateException("Booking escrow has already been released");
        platformLedger.save(new PlatformLedgerEntry(bookingId, PlatformLedgerBucket.ESCROW, LedgerDirection.DEBIT, amount, "booking:"+bookingId+":escrow:retain", "Close escrow obligation"));
        platformLedger.save(new PlatformLedgerEntry(bookingId, PlatformLedgerBucket.REVENUE, LedgerDirection.CREDIT, amount, "booking:"+bookingId+":revenue", "Platform retained booking amount"));
    }

    private void requireHeldAmount(UUID bookingId, long amount) {
        PlatformLedgerEntry credit = platformLedger.findByIdempotencyKey("booking:"+bookingId+":escrow:credit")
                .orElseThrow(() -> new IllegalStateException("Booking escrow credit is missing"));
        if (credit.getAmountVnd() != amount) {
            throw new IllegalStateException("Booking escrow amount does not match the credit");
        }
    }
}
