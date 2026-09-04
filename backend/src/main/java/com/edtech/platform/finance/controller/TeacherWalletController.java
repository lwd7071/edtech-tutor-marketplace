package com.edtech.platform.finance.controller;

import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.response.PageMeta;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.RequireRole;
import com.edtech.platform.finance.dto.response.LedgerEntryView;
import com.edtech.platform.finance.dto.response.WalletView;
import com.edtech.platform.finance.service.WalletQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/teacher/wallet")
@RequiredArgsConstructor
@RequireRole("TEACHER")
public class TeacherWalletController {

    private final WalletQueryService walletQueryService;

    @GetMapping
    public ApiResponse<WalletView> getWallet(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(walletQueryService.getOrCreateWallet(user.id()));
    }

    @GetMapping("/ledger")
    public ApiResponse<List<LedgerEntryView>> getLedger(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        size = Math.min(size, 100);
        Page<LedgerEntryView> result = walletQueryService.getLedgerEntries(user.id(), PageRequest.of(page, size));
        return ApiResponse.page(result.getContent(), PageMeta.from(result));
    }
}
