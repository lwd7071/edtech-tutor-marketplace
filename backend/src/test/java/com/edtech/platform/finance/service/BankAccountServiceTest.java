package com.edtech.platform.finance.service;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.config.properties.AccountEncryptionProperties;
import com.edtech.platform.finance.domain.TeacherBankAccount;
import com.edtech.platform.finance.dto.request.UpsertBankAccountRequest;
import com.edtech.platform.finance.dto.response.BankAccountView;
import com.edtech.platform.finance.repository.TeacherBankAccountRepository;
import com.edtech.platform.finance.mapper.BankAccountViewMapper;
import com.edtech.platform.finance.security.AccountNumberProtector;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.teacher.facade.dto.TeacherSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankAccountServiceTest {

    @Mock
    private TeacherBankAccountRepository bankAccountRepository;

    @Mock
    private TeacherFacade teacherFacade;

    private BankAccountService bankAccountService;

    private final UUID teacherUserId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        var protector = new AccountNumberProtector(
                new AccountEncryptionProperties("01234567890123456789012345678901"), new SecureRandom());
        bankAccountService = new BankAccountService(bankAccountRepository, teacherFacade,
                protector, new BankAccountViewMapper(protector));
    }

    private TeacherSnapshot mockTeacherSnapshot() {
        return new TeacherSnapshot(
                teacherId, teacherUserId, "APPROVED", true, true,
                "Teacher Name", "avatar.png", "Bio", 5, true, false,
                List.of("VIETNAMESE"), "Address", "video.mp4"
        );
    }

    @Test
    void createAccount_shouldSucceed_andMaskNumberInResponse() {
        when(teacherFacade.getTeacherByUserId(teacherUserId)).thenReturn(mockTeacherSnapshot());
        when(bankAccountRepository.existsByTeacherId(teacherId)).thenReturn(false);

        UpsertBankAccountRequest req = new UpsertBankAccountRequest(
                "970436", "Vietcombank", "0123456789", "NGUYEN VAN A", true
        );

        when(bankAccountRepository.save(any(TeacherBankAccount.class))).thenAnswer(invocation -> {
            TeacherBankAccount acc = invocation.getArgument(0);
            ReflectionTestUtils.setField(acc, "id", UUID.randomUUID());
            return acc;
        });

        BankAccountView view = bankAccountService.createAccount(teacherUserId, req);

        assertThat(view.bankBin()).isEqualTo("970436");
        assertThat(view.bankName()).isEqualTo("Vietcombank");
        assertThat(view.accountHolderName()).isEqualTo("NGUYEN VAN A");
        assertThat(view.accountNumberMasked()).isEqualTo("******6789");
        assertThat(view.isDefault()).isTrue();
    }

    @Test
    void updateAccount_shouldSucceed() {
        UUID accountId = UUID.randomUUID();
        when(teacherFacade.getTeacherByUserId(teacherUserId)).thenReturn(mockTeacherSnapshot());

        TeacherBankAccount existing = TeacherBankAccount.create(
                teacherId, "970436", "Vietcombank", "enc", "NGUYEN VAN A", false
        );
        ReflectionTestUtils.setField(existing, "id", accountId);

        when(bankAccountRepository.findByIdAndTeacherId(accountId, teacherId)).thenReturn(Optional.of(existing));

        UpsertBankAccountRequest req = new UpsertBankAccountRequest(
                "970415", "VietinBank", "9876543210", "NGUYEN VAN B", true
        );

        BankAccountView view = bankAccountService.updateAccount(teacherUserId, accountId, req);

        assertThat(view.bankBin()).isEqualTo("970415");
        assertThat(view.accountHolderName()).isEqualTo("NGUYEN VAN B");
        assertThat(view.accountNumberMasked()).isEqualTo("******3210");
        assertThat(view.isDefault()).isTrue();
        verify(bankAccountRepository).unsetDefaultExcept(eq(teacherId), eq(accountId));
    }

    @Test
    void deleteAccount_shouldSucceed() {
        UUID accountId = UUID.randomUUID();
        when(teacherFacade.getTeacherByUserId(teacherUserId)).thenReturn(mockTeacherSnapshot());

        TeacherBankAccount existing = TeacherBankAccount.create(
                teacherId, "970436", "Vietcombank", "enc", "NGUYEN VAN A", false
        );
        when(bankAccountRepository.findByIdAndTeacherId(accountId, teacherId)).thenReturn(Optional.of(existing));

        bankAccountService.deleteAccount(teacherUserId, accountId, "\"0\"");

        verify(bankAccountRepository).delete(existing);
    }

    @Test
    void deleteRejectsMalformedOrOverflowingIfMatchAsValidationError() {
        UUID accountId = UUID.randomUUID();
        when(teacherFacade.getTeacherByUserId(teacherUserId)).thenReturn(mockTeacherSnapshot());
        TeacherBankAccount existing = TeacherBankAccount.create(
                teacherId, "970436", "Vietcombank", "enc", "NGUYEN VAN A", false);
        when(bankAccountRepository.findByIdAndTeacherId(accountId, teacherId)).thenReturn(Optional.of(existing));

        for (String invalid : List.of("\"0", "0\"", "0", "\"999999999999999999999999\"")) {
            assertThatThrownBy(() -> bankAccountService.deleteAccount(teacherUserId, accountId, invalid))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(error -> assertThat(((BusinessException) error).getErrorCode())
                            .isEqualTo(ErrorCode.VALIDATION_ERROR));
        }
        verify(bankAccountRepository, never()).delete(any());
    }
}
