package com.edtech.platform.payment.controller;

import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.enrollment.service.StudentPackageReadService;
import com.edtech.platform.enrollment.dto.StudentOwnedPackageView;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.response.PageMeta;
import com.edtech.platform.common.security.RequireRole;
import com.edtech.platform.payment.facade.InvoiceReadFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import com.edtech.platform.payment.dto.StudentInvoiceDetail;
import com.edtech.platform.enrollment.dto.StudentPackageDetail;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentPaymentReadController {
    private final InvoiceReadFacade invoices;
    private final StudentPackageReadService packageReads;

    @GetMapping("/invoices/{id}")
    @RequireRole("STUDENT")
    public ApiResponse<StudentInvoiceDetail> invoice(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(invoices.findOwned(id, user.id()).map(StudentInvoiceDetail::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVOICE_NOT_FOUND)));
    }

    @GetMapping("/packages")
    @RequireRole("STUDENT")
    public ApiResponse<java.util.List<StudentOwnedPackageView>> packages(@AuthenticationPrincipal AuthenticatedUser user,
                                      @RequestParam(required = false) String status,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        com.edtech.platform.enrollment.domain.StudentPackageStatus parsed = null;
        try { if (status != null) parsed = com.edtech.platform.enrollment.domain.StudentPackageStatus.valueOf(status.toUpperCase()); }
        catch (IllegalArgumentException invalid) { throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid package status"); }
        var result = packageReads.list(user.id(), parsed, PageRequest.of(page, Math.min(size, 100)));
        return ApiResponse.page(result.getContent(), PageMeta.from(result));
    }

    @GetMapping("/packages/{id}")
    @RequireRole("STUDENT")
    public ApiResponse<StudentOwnedPackageView> packageDetail(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(packageReads.detail(id, user.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)));
    }
}
