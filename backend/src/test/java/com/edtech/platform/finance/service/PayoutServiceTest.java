package com.edtech.platform.finance.service;

import com.edtech.platform.finance.command.CompleteTransferCommand;
import com.edtech.platform.finance.command.RejectFinanceCommand;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.finance.domain.PayoutRequest;
import com.edtech.platform.finance.domain.PayoutStatus;
import com.edtech.platform.finance.domain.TeacherBankAccount;
import com.edtech.platform.finance.domain.Wallet;
import com.edtech.platform.finance.dto.request.CreatePayoutRequest;
import com.edtech.platform.finance.dto.response.PayoutRequestView;
import com.edtech.platform.finance.repository.LedgerEntryRepository;
import com.edtech.platform.finance.repository.PayoutRequestRepository;
import com.edtech.platform.finance.repository.TeacherBankAccountRepository;
import com.edtech.platform.finance.repository.WalletRepository;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.teacher.facade.dto.TeacherSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayoutServiceTest {

    @Mock private PayoutRequestRepository payoutRequestRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private LedgerEntryRepository ledgerEntryRepository;
    @Mock private TeacherBankAccountRepository bankAccountRepository;
    @Mock private TeacherFacade teacherFacade;

    private PayoutService payoutService;

    private final UUID teacherUserId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();
    private final UUID bankAccountId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        payoutService = new PayoutService(
                payoutRequestRepository, walletRepository, ledgerEntryRepository,
                bankAccountRepository, teacherFacade
        );
    }

    private TeacherSnapshot mockTeacherSnapshot() {
        return new TeacherSnapshot(
                teacherId, teacherUserId, "APPROVED", true, true,
                "Teacher Name", "avatar.png", "Bio", 5, true, false,
                List.of("VIETNAMESE"), "Address", "video.mp4"
        );
    }

    @Test
    void createPayout_shouldSucceed_andReserveBalance() {
        when(teacherFacade.getTeacherByUserId(teacherUserId)).thenReturn(mockTeacherSnapshot());

        TeacherBankAccount bankAccount = TeacherBankAccount.create(
                teacherId, "970436", "Vietcombank", "enc", "NGUYEN VAN A", true
        );
        ReflectionTestUtils.setField(bankAccount, "id", bankAccountId);
        when(bankAccountRepository.findByIdAndTeacherId(bankAccountId, teacherId)).thenReturn(Optional.of(bankAccount));
        when(payoutRequestRepository.existsByTeacherIdAndStatusIn(eq(teacherId), any())).thenReturn(false);

        Wallet wallet = Wallet.forTeacher(teacherId);
        ReflectionTestUtils.setField(wallet, "id", UUID.randomUUID());
        wallet.creditAvailable(1000000L);
        when(walletRepository.findByTeacherIdForUpdate(teacherId)).thenReturn(Optional.of(wallet));

        when(payoutRequestRepository.save(any(PayoutRequest.class))).thenAnswer(inv -> {
            PayoutRequest p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
            return p;
        });

        CreatePayoutRequest req = new CreatePayoutRequest(bankAccountId, 500000L, "Rut tien", 0L);
        PayoutRequestView view = payoutService.createPayout(teacherUserId, req);

        assertThat(view.amountVnd()).isEqualTo(500000L);
        assertThat(view.status()).isEqualTo(PayoutStatus.PENDING);
        assertThat(wallet.getAvailableBalanceVnd()).isEqualTo(500000L);
        assertThat(wallet.getReservedBalanceVnd()).isEqualTo(500000L);
        verify(ledgerEntryRepository, times(2)).append(any());
    }

    @Test
    void completePayout_shouldDebitReserved_andSetSucceeded() {
        UUID payoutId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();

        PayoutRequest payout = PayoutRequest.create(teacherId, walletId, bankAccountId, 500000L, "Rut tien");
        ReflectionTestUtils.setField(payout, "id", payoutId);
        when(payoutRequestRepository.findByIdForUpdate(payoutId)).thenReturn(Optional.of(payout));

        Wallet wallet = Wallet.forTeacher(teacherId);
        ReflectionTestUtils.setField(wallet, "id", walletId);
        wallet.creditAvailable(500000L);
        wallet.reserveAvailable(500000L);
        when(walletRepository.findByTeacherIdForUpdate(teacherId)).thenReturn(Optional.of(wallet));

        CompleteTransferCommand req = new CompleteTransferCommand(
                "VCB123456", Instant.now(), "proof_pub", "https://proof.example.com", 0L
        );

        PayoutRequestView view = payoutService.completePayout(adminId, payoutId, req);

        assertThat(view.status()).isEqualTo(PayoutStatus.SUCCEEDED);
        assertThat(view.bankReference()).isEqualTo("VCB123456");
        assertThat(wallet.getReservedBalanceVnd()).isEqualTo(0L);
        verify(ledgerEntryRepository).append(any());
    }

    @Test
    void rejectPayout_shouldReleaseReserved_andSetRejected() {
        UUID payoutId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();

        PayoutRequest payout = PayoutRequest.create(teacherId, walletId, bankAccountId, 500000L, "Rut tien");
        ReflectionTestUtils.setField(payout, "id", payoutId);
        when(payoutRequestRepository.findByIdForUpdate(payoutId)).thenReturn(Optional.of(payout));

        Wallet wallet = Wallet.forTeacher(teacherId);
        ReflectionTestUtils.setField(wallet, "id", walletId);
        wallet.creditAvailable(500000L);
        wallet.reserveAvailable(500000L);
        when(walletRepository.findByTeacherIdForUpdate(teacherId)).thenReturn(Optional.of(wallet));

        RejectFinanceCommand req = new RejectFinanceCommand("Sai so tai khoan", 0L);
        PayoutRequestView view = payoutService.rejectPayout(adminId, payoutId, req);

        assertThat(view.status()).isEqualTo(PayoutStatus.REJECTED);
        assertThat(wallet.getReservedBalanceVnd()).isEqualTo(0L);
        assertThat(wallet.getAvailableBalanceVnd()).isEqualTo(500000L);
        verify(ledgerEntryRepository, times(2)).append(any());
    }
}
