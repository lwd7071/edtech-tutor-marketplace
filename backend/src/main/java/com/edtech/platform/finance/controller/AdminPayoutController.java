package com.edtech.platform.finance.controller;

import com.edtech.platform.finance.dto.request.ProcessPayoutRequest;
import com.edtech.platform.finance.dto.request.RejectFinanceRequest;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.response.PageMeta;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.RequireRole;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.finance.dto.response.PayoutRequestView;
import com.edtech.platform.finance.command.CompleteTransferCommand;
import com.edtech.platform.finance.command.ProcessPayoutCommand;
import com.edtech.platform.finance.command.RejectFinanceCommand;
import com.edtech.platform.finance.service.PayoutService;
import com.edtech.platform.finance.service.FinanceProofStorage;
import com.edtech.platform.finance.dto.request.CompleteTransferMetadata;
import org.springframework.web.multipart.MultipartFile;
import com.edtech.platform.finance.idempotency.FinanceIdempotent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/payout-requests")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class AdminPayoutController {

    private final PayoutService payoutService;
    private final FinanceProofStorage proofStorage;

    @GetMapping
    public ApiResponse<List<PayoutRequestView>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (page < 0 || size < 1 || size > 100) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        Page<PayoutRequestView> result = payoutService.findAdminPayouts(status, PageRequest.of(page, size));
        return ApiResponse.page(result.getContent(), PageMeta.from(result));
    }

    @PostMapping("/{id}/process")
    @FinanceIdempotent(operation = "ADMIN_PAYOUT_PROCESS", responseType = PayoutRequestView.class)
    public ApiResponse<PayoutRequestView> process(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @Valid @RequestBody ProcessPayoutRequest request
    ) {
        return ApiResponse.ok(payoutService.processPayout(user.id(), id,
                new ProcessPayoutCommand(request.version())));
    }

    @PostMapping(value = "/{id}/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    @FinanceIdempotent(operation = "ADMIN_PAYOUT_COMPLETE", responseType = PayoutRequestView.class)
    public ApiResponse<PayoutRequestView> complete(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @Valid @RequestPart("metadata") CompleteTransferMetadata request,
            @RequestPart("proof") MultipartFile proof
    ) {
        var uploaded = proofStorage.upload(proof, "payout", id);
        return ApiResponse.ok(payoutService.completePayout(user.id(), id,
                new CompleteTransferCommand(request.bankReference(), request.transferredAt(),
                        uploaded.publicId(), uploaded.secureUrl(), request.version())));
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
