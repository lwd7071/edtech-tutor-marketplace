package com.edtech.platform.finance.controller;

import com.edtech.platform.admin.dto.request.CompleteTransferRequest;
import com.edtech.platform.admin.dto.request.ProcessPayoutRequest;
import com.edtech.platform.admin.dto.request.RejectRequest;
import com.edtech.platform.admin.dto.request.RejectFinanceRequest;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.response.PageMeta;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.RequireRole;
import com.edtech.platform.finance.dto.response.PayoutRequestView;
import com.edtech.platform.finance.command.CompleteTransferCommand;
import com.edtech.platform.finance.command.ProcessPayoutCommand;
import com.edtech.platform.finance.command.RejectFinanceCommand;
import com.edtech.platform.finance.service.PayoutService;
import com.edtech.platform.finance.idempotency.FinanceIdempotent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/payout-requests")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class AdminPayoutController {

    private final PayoutService payoutService;

    @GetMapping
    public ApiResponse<List<PayoutRequestView>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        size = Math.min(size, 100);
        Page<PayoutRequestView> result = payoutService.findAdminPayouts(status, PageRequest.of(page, size));
        return ApiResponse.page(result.getContent(), PageMeta.from(result));
    }

    @PostMapping("/{id}/process")
    @FinanceIdempotent(operation = "ADMIN_PAYOUT_PROCESS", responseType = PayoutRequestView.class)
    public ApiResponse<PayoutRequestView> process(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestBody ProcessPayoutRequest request
    ) {
        return ApiResponse.ok(payoutService.processPayout(user.id(), id,
                new ProcessPayoutCommand(request.version())));
    }

    @PostMapping("/{id}/complete")
    @FinanceIdempotent(operation = "ADMIN_PAYOUT_COMPLETE", responseType = PayoutRequestView.class)
    public ApiResponse<PayoutRequestView> complete(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @Valid @RequestBody CompleteTransferRequest request
    ) {
        return ApiResponse.ok(payoutService.completePayout(user.id(), id,
                new CompleteTransferCommand(request.bankReference(), request.transferredAt(),
                        request.proofPublicId(), request.proofUrl(), request.version())));
    }

    @PostMapping("/{id}/reject")
    @FinanceIdempotent(operation = "ADMIN_PAYOUT_REJECT", responseType = PayoutRequestView.class)
    public ApiResponse<PayoutRequestView> reject(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @Valid @RequestBody RejectFinanceRequest request
    ) {
        return ApiResponse.ok(payoutService.rejectPayout(user.id(), id,
                new RejectFinanceCommand(request.reason(), request.version())));
    }
}
