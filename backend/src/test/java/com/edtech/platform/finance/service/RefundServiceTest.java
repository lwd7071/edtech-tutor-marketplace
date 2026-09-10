package com.edtech.platform.finance.service;

import com.edtech.platform.finance.command.ApproveRefundCommand;
import com.edtech.platform.finance.command.CompleteTransferCommand;
import com.edtech.platform.finance.command.RejectFinanceCommand;
import com.edtech.platform.booking.facade.BookingEligibilityFacade;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.config.properties.AccountEncryptionProperties;
import com.edtech.platform.enrollment.facade.EnrollmentFacade;
import com.edtech.platform.enrollment.facade.dto.EnrollmentPackageSnapshot;
import com.edtech.platform.finance.domain.RefundRequest;
import com.edtech.platform.finance.domain.RefundStatus;
import com.edtech.platform.finance.domain.Wallet;
import com.edtech.platform.finance.domain.LedgerEntry;
import com.edtech.platform.finance.facade.PackageMoneyAllocator;
import com.edtech.platform.finance.dto.request.CreateRefundRequest;
import com.edtech.platform.finance.dto.response.RefundRequestView;
import com.edtech.platform.finance.repository.LedgerEntryRepository;
import com.edtech.platform.finance.repository.RefundRequestRepository;
import com.edtech.platform.finance.repository.WalletRepository;
import com.edtech.platform.finance.mapper.RefundRequestViewMapper;
import com.edtech.platform.finance.security.AccountNumberProtector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock private RefundRequestRepository refundRequestRepository;
    @Mock private EnrollmentFacade enrollmentFacade;
    @Mock private BookingEligibilityFacade bookingEligibilityFacade;
    @Mock private WalletRepository walletRepository;
    @Mock private LedgerEntryRepository ledgerEntryRepository;

    private RefundService refundService;

    private final UUID studentId = UUID.randomUUID();
    private final UUID packageId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        var protector = new AccountNumberProtector(
                new AccountEncryptionProperties("01234567890123456789012345678901"), new SecureRandom());
        refundService = new RefundService(
                refundRequestRepository, enrollmentFacade, bookingEligibilityFacade,
                walletRepository, ledgerEntryRepository, protector, new RefundRequestViewMapper(protector),
                new PackageMoneyAllocator()
        );
    }

    private EnrollmentPackageSnapshot mockPackage(int total, int remaining, int completed, int refunded, long price) {
        return mockPackage(total, remaining, completed, refunded, price, new BigDecimal("5.00"));
    }

    private EnrollmentPackageSnapshot mockPackage(
            int total, int remaining, int completed, int refunded, long price, BigDecimal commissionRate) {
        return new EnrollmentPackageSnapshot(
                packageId, studentId, teacherId, UUID.randomUUID(), "ACTIVE",
                total, remaining, 0, completed, refunded, price,
                commissionRate, Instant.now().minusSeconds(86400), Instant.now().plusSeconds(86400 * 30), 0L
        );
    }

    @Test
    void createRefund_shouldSucceed_andMarkRefundPending() {
        EnrollmentPackageSnapshot pkg = mockPackage(10, 10, 0, 0, 1000000L);
        when(enrollmentFacade.inspect(packageId, studentId)).thenReturn(pkg);
        when(bookingEligibilityFacade.hasScheduledBookingForPackage(packageId)).thenReturn(false);
        when(refundRequestRepository.existsByStudentPackageIdAndStatus(packageId, RefundStatus.PENDING)).thenReturn(false);

        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(inv -> {
            RefundRequest r = inv.getArgument(0);
            ReflectionTestUtils.setField(r, "id", UUID.randomUUID());
            return r;
        });

        CreateRefundRequest req = new CreateRefundRequest(
                packageId, "Khong the tiep tuc", 5, "Vietcombank", "970436", "0123456789", "NGUYEN VAN A"
        );

        RefundRequestView view = refundService.createRefund(studentId, req);

        assertThat(view.studentPackageId()).isEqualTo(packageId);
        assertThat(view.requestedSessions()).isEqualTo(5);
        assertThat(view.status()).isEqualTo(RefundStatus.PENDING);
        assertThat(view.accountNumberMasked()).isEqualTo("******6789");

        verify(enrollmentFacade).markRefundPending(packageId);
    }

    @Test
    void approveRefund_shouldCalculateCumulativeAmount() {
        UUID refundId = UUID.randomUUID();
        RefundRequest refund = RefundRequest.create(
                packageId, studentId, "Reason", 5, "VCB", "970436", "enc", "NGUYEN VAN A"
        );
        ReflectionTestUtils.setField(refund, "id", refundId);
        when(refundRequestRepository.findByIdForUpdate(refundId)).thenReturn(Optional.of(refund));

        EnrollmentPackageSnapshot pkg = mockPackage(10, 8, 2, 0, 1000000L);
        when(enrollmentFacade.inspect(packageId, null)).thenReturn(pkg);

        ApproveRefundCommand req = new ApproveRefundCommand(3, "Duyet 3 buoi", 0L);

        RefundRequestView view = refundService.approveRefund(adminId, refundId, req);

        assertThat(view.status()).isEqualTo(RefundStatus.APPROVED);
        assertThat(view.approvedSessions()).isEqualTo(3);
        // resolvedBefore = 2 sessions (200k), resolvedAfter = 5 sessions (500k). Refund = 300,000
        assertThat(view.refundAmountVnd()).isEqualTo(300000L);
    }

    @Test
    void completeRefund_shouldApplyPackageRefund_andDebitTeacherWallet() {
        UUID refundId = UUID.randomUUID();
        RefundRequest refund = RefundRequest.create(
                packageId, studentId, "Reason", 5, "VCB", "970436", "enc", "NGUYEN VAN A"
        );
        ReflectionTestUtils.setField(refund, "id", refundId);
        refund.approve(adminId, 3, 300000L, "Note", Instant.now());
        when(refundRequestRepository.findByIdForUpdate(refundId)).thenReturn(Optional.of(refund));

        EnrollmentPackageSnapshot pkg = mockPackage(10, 8, 2, 0, 1000000L);
        when(enrollmentFacade.inspect(packageId, null)).thenReturn(pkg);

        Wallet wallet = Wallet.forTeacher(teacherId);
        ReflectionTestUtils.setField(wallet, "id", UUID.randomUUID());
        wallet.creditPending(760000L); // 8 * 95k = 760k
        when(walletRepository.findByTeacherIdForUpdate(teacherId)).thenReturn(Optional.of(wallet));

        CompleteTransferCommand req = new CompleteTransferCommand("VCB-REF-123", Instant.now(), "proof", "https://proof", 0L);

        RefundRequestView view = refundService.completeRefund(adminId, refundId, req);

        assertThat(view.status()).isEqualTo(RefundStatus.REFUNDED);
        verify(enrollmentFacade).applyRefund(packageId, 3);
        // Teacher refund net for 3 sessions: 3 * 95,000 = 285,000 VND
        assertThat(wallet.getPendingBalanceVnd()).isEqualTo(760000L - 285000L);
        verify(ledgerEntryRepository).append(any());
    }

    @Test
    void completeRefund_shouldDebitExactAmount_whenPendingEqualsRefundNet() {
        UUID refundId = UUID.randomUUID();
        RefundRequest refund = approvedRefund(refundId, 3, 300000L);
        when(refundRequestRepository.findByIdForUpdate(refundId)).thenReturn(Optional.of(refund));
        when(enrollmentFacade.inspect(packageId, null)).thenReturn(mockPackage(10, 8, 2, 0, 1000000L));
        Wallet wallet = walletWithPending(285000L);
        when(walletRepository.findByTeacherIdForUpdate(teacherId)).thenReturn(Optional.of(wallet));

        refundService.completeRefund(adminId, refundId, transferCommand());

        assertThat(wallet.getPendingBalanceVnd()).isZero();
        var entry = org.mockito.ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository).append(entry.capture());
        assertThat(entry.getValue().getAmountVnd()).isEqualTo(285000L);
    }

    @Test
    void completeRefund_shouldRejectInsufficientPending_withoutPartialDebitOrLedger() {
        UUID refundId = UUID.randomUUID();
        RefundRequest refund = approvedRefund(refundId, 3, 300000L);
        when(refundRequestRepository.findByIdForUpdate(refundId)).thenReturn(Optional.of(refund));
        when(enrollmentFacade.inspect(packageId, null)).thenReturn(mockPackage(10, 8, 2, 0, 1000000L));
        Wallet wallet = walletWithPending(100000L);
        when(walletRepository.findByTeacherIdForUpdate(teacherId)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> refundService.completeRefund(adminId, refundId, transferCommand()))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(com.edtech.platform.common.exception.ErrorCode.REFUND_WALLET_INSUFFICIENT));

        assertThat(wallet.getPendingBalanceVnd()).isEqualTo(100000L);
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.APPROVED);
        verify(enrollmentFacade).applyRefund(packageId, 3);
        verifyNoInteractions(ledgerEntryRepository);
    }

    @Test
    void completeRefund_shouldReturnBusinessError_whenPendingIsZero() {
        UUID refundId = UUID.randomUUID();
        RefundRequest refund = approvedRefund(refundId, 3, 300000L);
        when(refundRequestRepository.findByIdForUpdate(refundId)).thenReturn(Optional.of(refund));
        when(enrollmentFacade.inspect(packageId, null)).thenReturn(mockPackage(10, 8, 2, 0, 1000000L));
        Wallet wallet = walletWithPending(0L);
        when(walletRepository.findByTeacherIdForUpdate(teacherId)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> refundService.completeRefund(adminId, refundId, transferCommand()))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(com.edtech.platform.common.exception.ErrorCode.REFUND_WALLET_INSUFFICIENT));
        assertThat(wallet.getPendingBalanceVnd()).isZero();
        verifyNoInteractions(ledgerEntryRepository);
    }

    @Test
    void completeRefund_shouldSkipWalletAndLedger_whenTeacherRefundNetIsZero() {
        UUID refundId = UUID.randomUUID();
        RefundRequest refund = approvedRefund(refundId, 1, 1L);
        when(refundRequestRepository.findByIdForUpdate(refundId)).thenReturn(Optional.of(refund));
        when(enrollmentFacade.inspect(packageId, null)).thenReturn(
                mockPackage(10, 10, 0, 0, 1L, new BigDecimal("100")));

        RefundRequestView result = refundService.completeRefund(adminId, refundId, transferCommand());

        assertThat(result.status()).isEqualTo(RefundStatus.REFUNDED);
        verify(enrollmentFacade).applyRefund(packageId, 1);
        verifyNoInteractions(walletRepository, ledgerEntryRepository);
    }

    @Test
    void rejectRefund_shouldRestorePackageStatus() {
        UUID refundId = UUID.randomUUID();
        RefundRequest refund = RefundRequest.create(
                packageId, studentId, "Reason", 5, "VCB", "970436", "enc", "NGUYEN VAN A"
        );
        ReflectionTestUtils.setField(refund, "id", refundId);
        when(refundRequestRepository.findByIdForUpdate(refundId)).thenReturn(Optional.of(refund));

        RejectFinanceCommand req = new RejectFinanceCommand("Khong du dieu kien", 0L);

        RefundRequestView view = refundService.rejectRefund(adminId, refundId, req);

        assertThat(view.status()).isEqualTo(RefundStatus.REJECTED);
        verify(enrollmentFacade).restoreFromRefundPending(packageId);
    }

    private RefundRequest approvedRefund(UUID refundId, int sessions, long amount) {
        RefundRequest refund = RefundRequest.create(
                packageId, studentId, "Reason", sessions, "VCB", "970436", "enc", "NGUYEN VAN A");
        ReflectionTestUtils.setField(refund, "id", refundId);
        refund.approve(adminId, sessions, amount, "Note", Instant.now());
        return refund;
    }

    private Wallet walletWithPending(long amount) {
        Wallet wallet = Wallet.forTeacher(teacherId);
        ReflectionTestUtils.setField(wallet, "id", UUID.randomUUID());
        if (amount > 0) {
            wallet.creditPending(amount);
        }
        return wallet;
    }

    private CompleteTransferCommand transferCommand() {
        return new CompleteTransferCommand("VCB-REF-123", Instant.now(), "proof", "https://proof", 0L);
    }
}
