package com.edtech.platform.finance.controller;

import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.RequireRole;
import com.edtech.platform.finance.dto.request.UpsertBankAccountRequest;
import com.edtech.platform.finance.dto.response.BankAccountView;
import com.edtech.platform.finance.service.BankAccountService;
import com.edtech.platform.finance.idempotency.FinanceIdempotent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teacher/bank-accounts")
@RequiredArgsConstructor
@RequireRole("TEACHER")
public class TeacherBankAccountController {

    private final BankAccountService bankAccountService;

    @GetMapping
    public ApiResponse<List<BankAccountView>> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(bankAccountService.findTeacherAccounts(user.id()));
    }

    @PostMapping
    @FinanceIdempotent(operation = "TEACHER_BANK_ACCOUNT_CREATE", responseType = BankAccountView.class)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BankAccountView> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UpsertBankAccountRequest request
    ) {
        return ApiResponse.created(bankAccountService.createAccount(user.id(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<BankAccountView> update(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @Valid @RequestBody UpsertBankAccountRequest request
    ) {
        return ApiResponse.ok(bankAccountService.updateAccount(user.id(), id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "If-Match", required = false) String ifMatch
    ) {
        bankAccountService.deleteAccount(user.id(), id, ifMatch);
    }
}
