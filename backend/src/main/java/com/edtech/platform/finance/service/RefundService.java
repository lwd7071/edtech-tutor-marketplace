package com.edtech.platform.finance.service;

import com.edtech.platform.admin.dto.request.ApproveRefundRequest;
import com.edtech.platform.admin.dto.request.CompleteTransferRequest;
import com.edtech.platform.admin.dto.request.RejectRequest;
import com.edtech.platform.booking.facade.BookingEligibilityFacade;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.enrollment.facade.EnrollmentFacade;
import com.edtech.platform.enrollment.facade.dto.EnrollmentPackageSnapshot;
import com.edtech.platform.finance.domain.*;
import com.edtech.platform.finance.dto.request.CreateRefundRequest;
import com.edtech.platform.finance.dto.response.RefundRequestView;
import com.edtech.platform.finance.repository.LedgerEntryRepository;
import com.edtech.platform.finance.repository.RefundRequestRepository;
import com.edtech.platform.finance.repository.WalletRepository;
import com.edtech.platform.finance.util.AccountNumberCipher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRequestRepository refundRequestRepository;
    private final EnrollmentFacade enrollmentFacade;
    private final BookingEligibilityFacade bookingEligibilityFacade;
    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    @Transactional
    public RefundRequestView createRefund(UUID studentId, CreateRefundRequest request) {
        if (request == null || request.requestedSessions() <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        EnrollmentPackageSnapshot pkg = enrollmentFacade.inspect(request.studentPackageId(), studentId);
        if (pkg == null) {
            throw new BusinessException(ErrorCode.PRICING_PACKAGE_NOT_FOUND);
        }

        if (!"ACTIVE".equalsIgnoreCase(pkg.status()) && !"LOCKED_EXPIRED".equalsIgnoreCase(pkg.status())) {
            throw new BusinessException(ErrorCode.PACKAGE_INVALID_STATE);
        }

        if (pkg.remainingSessions() <= 0) {
            throw new BusinessException(ErrorCode.REFUND_NO_REMAINING_SESSION);
        }

        if (request.requestedSessions() > pkg.remainingSessions()) {
            throw new BusinessException(ErrorCode.REFUND_AMOUNT_EXCEEDED);
        }

        if (bookingEligibilityFacade.hasScheduledBookingForPackage(pkg.id())) {
            throw new BusinessException(ErrorCode.PACKAGE_HAS_SCHEDULED_BOOKING);
        }

        if (refundRequestRepository.existsByStudentPackageIdAndStatus(pkg.id(), RefundStatus.PENDING)) {
            throw new BusinessException(ErrorCode.REFUND_ALREADY_PENDING);
        }

        // 1. Lock package and mark REFUND_PENDING
        enrollmentFacade.markRefundPending(pkg.id());

        // 2. Encrypt account number
        String encryptedAccNumber = AccountNumberCipher.encrypt(request.accountNumber());

        // 3. Create RefundRequest
        RefundRequest refund = RefundRequest.create(
                pkg.id(),
                studentId,
                request.reason(),
                request.requestedSessions(),
                request.bankName(),
                request.bankBin(),
                encryptedAccNumber,
                request.accountHolderName() != null ? request.accountHolderName().trim().toUpperCase() : null
        );
        refund = refundRequestRepository.save(refund);

        return RefundRequestView.from(refund);
    }

    @Transactional(readOnly = true)
    public Page<RefundRequestView> findStudentRefunds(UUID studentId, Pageable pageable) {
        return refundRequestRepository.findByStudentIdOrderByCreatedAtDesc(studentId, pageable)
                .map(RefundRequestView::from);
    }

    @Transactional(readOnly = true)
    public Page<RefundRequestView> findAdminRefunds(String status, Pageable pageable) {
        RefundStatus parsedStatus = (status != null && !status.isBlank()) ? RefundStatus.valueOf(status.trim().toUpperCase()) : null;
        return findAdminRefunds(parsedStatus, pageable);
    }

    @Transactional(readOnly = true)
    public Page<RefundRequestView> findAdminRefunds(RefundStatus status, Pageable pageable) {
        Page<RefundRequest> page = (status != null)
                ? refundRequestRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                : refundRequestRepository.findAllByOrderByCreatedAtDesc(pageable);
        return page.map(RefundRequestView::from);
    }

    @Transactional
    public RefundRequestView approveRefund(UUID adminId, UUID refundId, ApproveRefundRequest request) {
        RefundRequest refund = refundRequestRepository.findByIdForUpdate(refundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFUND_NOT_FOUND));

        if (refund.getVersion() != request.version()) {
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
        }

        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new BusinessException(ErrorCode.REFUND_INVALID_STATE);
        }

        EnrollmentPackageSnapshot pkg = enrollmentFacade.inspect(refund.getStudentPackageId(), null);
        if (pkg == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        if (request.approvedSessions() <= 0 || request.approvedSessions() > refund.getRequestedSessions()
                || request.approvedSessions() > pkg.remainingSessions()) {
            throw new BusinessException(ErrorCode.REFUND_AMOUNT_EXCEEDED);
        }

        // Calculate cumulative refund amount
        int totalSessions = Math.max(1, pkg.totalSessions());
        long purchasePrice = pkg.purchasePriceVnd();
        int resolvedBefore = pkg.completedSessions() + pkg.refundedSessions();
        int resolvedAfter = resolvedBefore + request.approvedSessions();

        long refundAmountVnd = BigDecimal.valueOf(resolvedAfter).multiply(BigDecimal.valueOf(purchasePrice))
                .divide(BigDecimal.valueOf(totalSessions), 0, RoundingMode.FLOOR).longValue()
                - BigDecimal.valueOf(resolvedBefore).multiply(BigDecimal.valueOf(purchasePrice))
                .divide(BigDecimal.valueOf(totalSessions), 0, RoundingMode.FLOOR).longValue();

        refund.approve(adminId, request.approvedSessions(), refundAmountVnd, request.adminNote(), Instant.now());
        return RefundRequestView.from(refund);
    }

    @Transactional
    public RefundRequestView rejectRefund(UUID adminId, UUID refundId, RejectRequest request, long version) {
        RefundRequest refund = refundRequestRepository.findByIdForUpdate(refundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFUND_NOT_FOUND));

        if (refund.getVersion() != version) {
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
        }

        refund.reject(adminId, request.reason(), Instant.now());

        // Restore package status
        enrollmentFacade.restoreFromRefundPending(refund.getStudentPackageId());

        return RefundRequestView.from(refund);
    }

    @Transactional
    public RefundRequestView completeRefund(UUID adminId, UUID refundId, CompleteTransferRequest request) {
        RefundRequest refund = refundRequestRepository.findByIdForUpdate(refundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFUND_NOT_FOUND));

        if (refund.getVersion() != request.version()) {
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
        }

        EnrollmentPackageSnapshot pkg = enrollmentFacade.inspect(refund.getStudentPackageId(), null);
        if (pkg == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        // 1. Apply refund to package
        enrollmentFacade.applyRefund(pkg.id(), refund.getApprovedSessions());

        // 2. Deduct Teacher Wallet Pending balance
        long purchasePrice = pkg.purchasePriceVnd();
        BigDecimal commRate = pkg.commissionRate() != null ? pkg.commissionRate() : BigDecimal.ZERO;
        BigDecimal feeRate = commRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        long commissionTotal = new BigDecimal(purchasePrice).multiply(feeRate).setScale(0, RoundingMode.HALF_UP).longValue();
        long teacherNetTotal = purchasePrice - commissionTotal;

        int totalSessions = Math.max(1, pkg.totalSessions());
        int resolvedBefore = pkg.completedSessions() + pkg.refundedSessions();
        int resolvedAfter = resolvedBefore + refund.getApprovedSessions();

        long teacherRefundNet = BigDecimal.valueOf(resolvedAfter).multiply(BigDecimal.valueOf(teacherNetTotal))
                .divide(BigDecimal.valueOf(totalSessions), 0, RoundingMode.FLOOR).longValue()
                - BigDecimal.valueOf(resolvedBefore).multiply(BigDecimal.valueOf(teacherNetTotal))
                .divide(BigDecimal.valueOf(totalSessions), 0, RoundingMode.FLOOR).longValue();

        if (teacherRefundNet > 0) {
            Wallet wallet = walletRepository.findByTeacherIdForUpdate(pkg.teacherId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));
            if (wallet.getPendingBalanceVnd() >= teacherRefundNet) {
                wallet.debitPending(teacherRefundNet);
            } else {
                wallet.debitPending(wallet.getPendingBalanceVnd());
            }

            ledgerEntryRepository.append(new LedgerEntry(
                    wallet.getId(),
                    LedgerEntryType.REFUND_DEBIT_PENDING,
                    teacherRefundNet,
                    BalanceBucket.PENDING,
                    LedgerDirection.DEBIT,
                    "REFUND_REQUEST",
                    refundId,
                    refundId + ":REFUND_PENDING",
                    "Khấu trừ học phí do hoàn tiền cho học viên"
            ));
        }

        // 3. Complete refund
        refund.complete(adminId, request.bankReference(), request.proofPublicId(), request.proofUrl(), Instant.now());

        return RefundRequestView.from(refund);
    }
}