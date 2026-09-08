package com.edtech.platform.finance.service;

import com.edtech.platform.finance.command.CompleteTransferCommand;
import com.edtech.platform.finance.command.ProcessPayoutCommand;
import com.edtech.platform.finance.command.RejectFinanceCommand;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.finance.domain.*;
import com.edtech.platform.finance.dto.request.CreatePayoutRequest;
import com.edtech.platform.finance.dto.response.PayoutRequestView;
import com.edtech.platform.finance.repository.LedgerEntryRepository;
import com.edtech.platform.finance.repository.PayoutRequestRepository;
import com.edtech.platform.finance.repository.TeacherBankAccountRepository;
import com.edtech.platform.finance.repository.WalletRepository;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.teacher.facade.dto.TeacherSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PayoutService {

    private final PayoutRequestRepository payoutRequestRepository;
    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final TeacherBankAccountRepository bankAccountRepository;
    private final TeacherFacade teacherFacade;

    private UUID resolveTeacherId(UUID teacherUserId) {
        TeacherSnapshot teacher = teacherFacade.getTeacherByUserId(teacherUserId);
        if (teacher == null) {
            throw new BusinessException(ErrorCode.TEACHER_PROFILE_NOT_FOUND);
        }
        return teacher.id();
    }

    @Transactional
    public PayoutRequestView createPayout(UUID teacherUserId, CreatePayoutRequest request) {
        UUID teacherId = resolveTeacherId(teacherUserId);
        if (request == null || request.amountVnd() <= 0) {
            throw new BusinessException(ErrorCode.PAYOUT_INVALID_AMOUNT);
        }

        TeacherBankAccount bankAccount = bankAccountRepository.findByIdAndTeacherId(request.bankAccountId(), teacherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_ACCOUNT_NOT_FOUND));

        if (payoutRequestRepository.existsByTeacherIdAndStatusIn(teacherId, List.of(PayoutStatus.PENDING, PayoutStatus.PROCESSING))) {
            throw new BusinessException(ErrorCode.PAYOUT_ALREADY_PENDING);
        }

        Wallet wallet = walletRepository.findByTeacherIdForUpdate(teacherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));

        if (wallet.getVersion() != request.walletVersion()) {
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
        }

        if (wallet.getAvailableBalanceVnd() < request.amountVnd()) {
            throw new BusinessException(ErrorCode.PAYOUT_INSUFFICIENT_BALANCE);
        }

        // 1. Move from available to reserved
        wallet.reserveAvailable(request.amountVnd());

        // 2. Create payout request
        PayoutRequest payout = PayoutRequest.create(
                teacherId,
                wallet.getId(),
                bankAccount.getId(),
                request.amountVnd(),
                request.teacherNote()
        );
        payout = payoutRequestRepository.save(payout);
        UUID payoutId = payout.getId();

        // 3. Write 2 ledger entries
        ledgerEntryRepository.append(new LedgerEntry(
                wallet.getId(),
                LedgerEntryType.PAYOUT_RESERVED,
                request.amountVnd(),
                BalanceBucket.AVAILABLE,
                LedgerDirection.DEBIT,
                "PAYOUT_REQUEST",
                payoutId,
                payoutId + ":DEBIT_AVAIL",
                "Khóa số dư rút tiền"
        ));

        ledgerEntryRepository.append(new LedgerEntry(
                wallet.getId(),
                LedgerEntryType.PAYOUT_RESERVED,
                request.amountVnd(),
                BalanceBucket.RESERVED,
                LedgerDirection.CREDIT,
                "PAYOUT_REQUEST",
                payoutId,
                payoutId + ":CREDIT_RSVD",
                "Ghi nhận số dư chờ chuyển khoản"
        ));

        return PayoutRequestView.from(payout);
    }

    @Transactional(readOnly = true)
    public Page<PayoutRequestView> findTeacherPayouts(UUID teacherUserId, PayoutStatus status, Pageable pageable) {
        UUID teacherId = resolveTeacherId(teacherUserId);
        Page<PayoutRequest> page = status != null
                ? payoutRequestRepository.findByTeacherIdAndStatusOrderByCreatedAtDesc(teacherId, status, pageable)
                : payoutRequestRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId, pageable);
        return page.map(PayoutRequestView::from);
    }

    @Transactional(readOnly = true)
    public Page<PayoutRequestView> findAdminPayouts(String status, Pageable pageable) {
        PayoutStatus parsedStatus = (status != null && !status.isBlank()) ? PayoutStatus.valueOf(status.trim().toUpperCase()) : null;
        return findAdminPayouts(parsedStatus, pageable);
    }

    @Transactional(readOnly = true)
    public Page<PayoutRequestView> findAdminPayouts(PayoutStatus status, Pageable pageable) {
        Page<PayoutRequest> page = status != null
                ? payoutRequestRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                : payoutRequestRepository.findAllByOrderByCreatedAtDesc(pageable);
        return page.map(PayoutRequestView::from);
    }

    @Transactional
    public PayoutRequestView processPayout(UUID adminId, UUID payoutId, ProcessPayoutCommand request) {
        PayoutRequest payout = payoutRequestRepository.findByIdForUpdate(payoutId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (payout.getVersion() != request.version()) {
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
        }

        payout.process(adminId, Instant.now());
        return PayoutRequestView.from(payout);
    }

    @Transactional
    public PayoutRequestView completePayout(UUID adminId, UUID payoutId, CompleteTransferCommand request) {
        PayoutRequest payout = payoutRequestRepository.findByIdForUpdate(payoutId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (payout.getVersion() != request.version()) {
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
        }

        Wallet wallet = walletRepository.findByTeacherIdForUpdate(payout.getTeacherId())
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));

        if (wallet.getReservedBalanceVnd() < payout.getAmountVnd()) {
            throw new BusinessException(ErrorCode.WALLET_BALANCE_INVARIANT_VIOLATION);
        }

        wallet.debitReserved(payout.getAmountVnd());

        payout.complete(
                adminId,
                request.bankReference(),
                request.transferredAt(),
                request.proofPublicId(),
                request.proofUrl(),
                Instant.now()
        );

        ledgerEntryRepository.append(new LedgerEntry(
                wallet.getId(),
                LedgerEntryType.PAYOUT_SUCCEEDED,
                payout.getAmountVnd(),
                BalanceBucket.RESERVED,
                LedgerDirection.DEBIT,
                "PAYOUT_REQUEST",
                payoutId,
                payoutId + ":PAYOUT_SUCCEEDED",
                "Hoàn tất chuyển khoản rút tiền"
        ));

        return PayoutRequestView.from(payout);
    }

    @Transactional
    public PayoutRequestView rejectPayout(UUID adminId, UUID payoutId, RejectFinanceCommand request) {
        PayoutRequest payout = payoutRequestRepository.findByIdForUpdate(payoutId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (payout.getVersion() != request.version()) {
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
        }

        Wallet wallet = walletRepository.findByTeacherIdForUpdate(payout.getTeacherId())
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));

        wallet.releaseReserved(payout.getAmountVnd());

        payout.reject(adminId, request.reason(), Instant.now());

        ledgerEntryRepository.append(new LedgerEntry(
                wallet.getId(),
                LedgerEntryType.PAYOUT_RELEASED,
                payout.getAmountVnd(),
                BalanceBucket.RESERVED,
                LedgerDirection.DEBIT,
                "PAYOUT_REQUEST",
                payoutId,
                payoutId + ":RELEASE_RSVD",
                "Hoàn trả số dư tạm giữ do từ chối yêu cầu"
        ));

        ledgerEntryRepository.append(new LedgerEntry(
                wallet.getId(),
                LedgerEntryType.PAYOUT_RELEASED,
                payout.getAmountVnd(),
                BalanceBucket.AVAILABLE,
                LedgerDirection.CREDIT,
                "PAYOUT_REQUEST",
                payoutId,
                payoutId + ":RELEASE_AVAIL",
                "Khôi phục số dư khả dụng do từ chối rút tiền"
        ));

        return PayoutRequestView.from(payout);
    }
}
