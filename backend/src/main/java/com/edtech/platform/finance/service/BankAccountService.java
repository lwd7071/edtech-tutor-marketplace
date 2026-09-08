package com.edtech.platform.finance.service;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.finance.domain.TeacherBankAccount;
import com.edtech.platform.finance.dto.request.UpsertBankAccountRequest;
import com.edtech.platform.finance.dto.response.BankAccountView;
import com.edtech.platform.finance.mapper.BankAccountViewMapper;
import com.edtech.platform.finance.repository.TeacherBankAccountRepository;
import com.edtech.platform.finance.security.AccountNumberProtector;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.teacher.facade.dto.TeacherSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BankAccountService {

    private final TeacherBankAccountRepository bankAccountRepository;
    private final TeacherFacade teacherFacade;
    private final AccountNumberProtector accountNumbers;
    private final BankAccountViewMapper views;

    private UUID resolveTeacherId(UUID teacherUserId) {
        TeacherSnapshot teacher = teacherFacade.getTeacherByUserId(teacherUserId);
        if (teacher == null) {
            throw new BusinessException(ErrorCode.TEACHER_PROFILE_NOT_FOUND);
        }
        return teacher.id();
    }

    @Transactional(readOnly = true)
    public List<BankAccountView> findTeacherAccounts(UUID teacherUserId) {
        UUID teacherId = resolveTeacherId(teacherUserId);
        return bankAccountRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId)
                .stream()
                .map(views::toView)
                .toList();
    }

    @Transactional
    public BankAccountView createAccount(UUID teacherUserId, UpsertBankAccountRequest request) {
        UUID teacherId = resolveTeacherId(teacherUserId);
        validateRequest(request);

        boolean isDefault = request.isDefault() != null && request.isDefault();
        boolean hasAccounts = bankAccountRepository.existsByTeacherId(teacherId);
        if (!hasAccounts) {
            isDefault = true;
        }

        String encrypted = accountNumbers.encrypt(request.accountNumber());
        TeacherBankAccount account = TeacherBankAccount.create(
                teacherId,
                request.bankBin(),
                request.bankName(),
                encrypted,
                request.accountHolderName().trim().toUpperCase(),
                isDefault
        );
        account = bankAccountRepository.save(account);

        if (isDefault) {
            bankAccountRepository.unsetDefaultExcept(teacherId, account.getId());
        }

        return views.toView(account);
    }

    @Transactional
    public BankAccountView updateAccount(UUID teacherUserId, UUID accountId, UpsertBankAccountRequest request) {
        UUID teacherId = resolveTeacherId(teacherUserId);
        validateRequest(request);

        TeacherBankAccount account = bankAccountRepository.findByIdAndTeacherId(accountId, teacherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_ACCOUNT_NOT_FOUND));

        boolean isDefault = request.isDefault() != null && request.isDefault();
        String encrypted = accountNumbers.encrypt(request.accountNumber());
        account.update(
                request.bankBin(),
                request.bankName(),
                encrypted,
                request.accountHolderName().trim().toUpperCase(),
                isDefault
        );

        if (isDefault) {
            bankAccountRepository.unsetDefaultExcept(teacherId, account.getId());
        }

        return views.toView(account);
    }

    @Transactional
    public void deleteAccount(UUID teacherUserId, UUID accountId) {
        UUID teacherId = resolveTeacherId(teacherUserId);
        TeacherBankAccount account = bankAccountRepository.findByIdAndTeacherId(accountId, teacherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_ACCOUNT_NOT_FOUND));

        bankAccountRepository.delete(account);
    }

    private void validateRequest(UpsertBankAccountRequest request) {
        if (request == null) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        if (request.bankBin() == null || request.bankBin().isBlank()) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        if (request.bankName() == null || request.bankName().isBlank()) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        if (request.accountNumber() == null || request.accountNumber().isBlank()) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        if (request.accountHolderName() == null || request.accountHolderName().isBlank()) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
    }
}
