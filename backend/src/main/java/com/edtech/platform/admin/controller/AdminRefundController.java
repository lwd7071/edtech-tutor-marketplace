package com.edtech.platform.admin.controller;

import com.edtech.platform.admin.dto.request.ApproveRefundRequest;
import com.edtech.platform.admin.dto.request.CompleteTransferRequest;
import com.edtech.platform.admin.dto.request.RejectRequest;
import com.edtech.platform.admin.dto.request.RejectFinanceRequest;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.response.PageMeta;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.RequireRole;
import com.edtech.platform.finance.dto.response.RefundRequestView;
import com.edtech.platform.finance.command.ApproveRefundCommand;
import com.edtech.platform.finance.command.CompleteTransferCommand;
import com.edtech.platform.finance.command.RejectFinanceCommand;
import com.edtech.platform.finance.service.RefundService;
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
@RequestMapping("/api/admin/refund-requests")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class AdminRefundController {

    private final RefundService refundService;

    @GetMapping
    public ApiResponse<List<RefundRequestView>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        size = Math.min(size, 100);
        Page<RefundRequestView> result = refundService.findAdminRefunds(status, PageRequest.of(page, size));
        return ApiResponse.page(result.getContent(), PageMeta.from(result));
    }

    @PostMapping("/{id}/approve")
    @FinanceIdempotent(operation = "ADMIN_REFUND_APPROVE", responseType = RefundRequestView.class)
    public ApiResponse<RefundRequestView> approve(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @Valid @RequestBody ApproveRefundRequest request
    ) {
        return ApiResponse.ok(refundService.approveRefund(user.id(), id,
                new ApproveRefundCommand(request.approvedSessions(), request.adminNote(), request.version())));
    }

    @PostMapping("/{id}/reject")
    @FinanceIdempotent(operation = "ADMIN_REFUND_REJECT", responseType = RefundRequestView.class)
    public ApiResponse<RefundRequestView> reject(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @Valid @RequestBody RejectFinanceRequest request
    ) {
        return ApiResponse.ok(refundService.rejectRefund(user.id(), id,
                new RejectFinanceCommand(request.reason(), request.version())));
    }

    @PostMapping("/{id}/complete")
    @FinanceIdempotent(operation = "ADMIN_REFUND_COMPLETE", responseType = RefundRequestView.class)
    public ApiResponse<RefundRequestView> complete(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @Valid @RequestBody CompleteTransferRequest request
    ) {
        return ApiResponse.ok(refundService.completeRefund(user.id(), id,
                new CompleteTransferCommand(request.bankReference(), request.transferredAt(),
                        request.proofPublicId(), request.proofUrl(), request.version())));
    }
}
