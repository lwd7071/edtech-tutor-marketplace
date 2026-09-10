package com.edtech.platform.finance.service;

import com.edtech.platform.finance.command.ApproveRefundCommand;
import com.edtech.platform.finance.command.CompleteTransferCommand;
import com.edtech.platform.finance.command.RejectFinanceCommand;
import com.edtech.platform.booking.facade.BookingEligibilityFacade;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.enrollment.facade.EnrollmentFacade;
import com.edtech.platform.enrollment.facade.dto.EnrollmentPackageSnapshot;
import com.edtech.platform.finance.domain.*;
import com.edtech.platform.finance.facade.PackageMoneyAllocator;
import com.edtech.platform.finance.dto.request.CreateRefundRequest;
import com.edtech.platform.finance.dto.response.RefundRequestView;
import com.edtech.platform.finance.mapper.RefundRequestViewMapper;
import com.edtech.platform.finance.repository.LedgerEntryRepository;
import com.edtech.platform.finance.repository.RefundRequestRepository;
import com.edtech.platform.finance.repository.WalletRepository;
import com.edtech.platform.finance.security.AccountNumberProtector;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final AccountNumberProtector accountNumbers;
    private final RefundRequestViewMapper views;
    private final PackageMoneyAllocator packageMoneyAllocator;

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
        String encryptedAccNumber = accountNumbers.encrypt(request.accountNumber());

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

        return views.toView(refund);
    }

    @Transactional(readOnly = true)
    public Page<RefundRequestView> findStudentRefunds(UUID studentId, Pageable pageable) {
        return refundRequestRepository.findByStudentIdOrderByCreatedAtDesc(studentId, pageable)
                .map(views::toView);
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
        return page.map(views::toView);
    }

    @Transactional
    public RefundRequestView approveRefund(UUID adminId, UUID refundId, ApproveRefundCommand request) {
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

        int resolvedBefore = pkg.completedSessions() + pkg.refundedSessions();
        long refundAmountVnd = packageMoneyAllocator.allocationForRange(
                pkg.purchasePriceVnd(), pkg.totalSessions(), resolvedBefore, request.approvedSessions());

        refund.approve(adminId, request.approvedSessions(), refundAmountVnd, request.adminNote(), Instant.now());
        return views.toView(refund);
    }

    @Transactional
    public RefundRequestView rejectRefund(UUID adminId, UUID refundId, RejectFinanceCommand request) {
        RefundRequest refund = refundRequestRepository.findByIdForUpdate(refundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFUND_NOT_FOUND));

        if (refund.getVersion() != request.version()) {
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
        }
        refund.reject(adminId, request.reason(), Instant.now());

        // Restore package status
        enrollmentFacade.restoreFromRefundPending(refund.getStudentPackageId());

        return views.toView(refund);
    }

    @Transactional
    public RefundRequestView completeRefund(UUID adminId, UUID refundId, CompleteTransferCommand request) {
        RefundRequest refund = refundRequestRepository.findByIdForUpdate(refundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFUND_NOT_FOUND));

        if (refund.getVersion() != request.version()) {
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
        }
        if (refund.getStatus() != RefundStatus.APPROVED) {
            throw new BusinessException(ErrorCode.REFUND_INVALID_STATE);
        }

        EnrollmentPackageSnapshot pkg = enrollmentFacade.inspect(refund.getStudentPackageId(), null);
        if (pkg == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        // 1. Apply refund to package
        enrollmentFacade.applyRefund(pkg.id(), refund.getApprovedSessions());

        // 2. Deduct Teacher Wallet Pending balance
        BigDecimal commRate = pkg.commissionRate() != null ? pkg.commissionRate() : BigDecimal.ZERO;
        long teacherNetTotal = packageMoneyAllocator.teacherNetTotal(pkg.purchasePriceVnd(), commRate);
        int resolvedBefore = pkg.completedSessions() + pkg.refundedSessions();
        long teacherRefundNet = packageMoneyAllocator.allocationForRange(
                teacherNetTotal, pkg.totalSessions(), resolvedBefore, refund.getApprovedSessions());

        if (teacherRefundNet > 0) {
            Wallet wallet = walletRepository.findByTeacherIdForUpdate(pkg.teacherId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));
            if (wallet.getPendingBalanceVnd() < teacherRefundNet) {
                throw new BusinessException(ErrorCode.REFUND_WALLET_INSUFFICIENT);
            }
            wallet.debitPending(teacherRefundNet);

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

        return views.toView(refund);
    }
}
