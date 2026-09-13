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
        if (request.version() != 0L) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Create version must be 0");

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
        requireVersion(account.getVersion(), request.version());

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

    /**
     * Deletes after ownership lookup, so an IDOR cannot be turned into a version/header oracle.
     * The If-Match syntax is validated only after the resource is known to belong to the caller.
     */
    @Transactional
    public void deleteAccount(UUID teacherUserId, UUID accountId, String ifMatch) {
        UUID teacherId = resolveTeacherId(teacherUserId);
        TeacherBankAccount account = bankAccountRepository.findByIdAndTeacherId(accountId, teacherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_ACCOUNT_NOT_FOUND));
        if (ifMatch == null || !ifMatch.matches("\\\"\\d+\\\"")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "If-Match phải là version hiện tại");
        }
        final long requestedVersion;
        try {
            requestedVersion = Long.parseLong(ifMatch.substring(1, ifMatch.length() - 1));
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "If-Match phải là version hiện tại");
        }
        requireVersion(account.getVersion(), requestedVersion);
        bankAccountRepository.delete(account);
    }

    private void requireVersion(long current, long requested) {
        if (current != requested) throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
    }

    private void validateRequest(UpsertBankAccountRequest request) {
        if (request == null) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        if (request.bankBin() == null || request.bankBin().isBlank()) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        if (request.bankName() == null || request.bankName().isBlank()) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        if (request.accountNumber() == null || request.accountNumber().isBlank()) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        if (request.accountHolderName() == null || request.accountHolderName().isBlank()) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
    }
}
