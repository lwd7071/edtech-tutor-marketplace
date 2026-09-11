package com.edtech.platform.payment.controller;

import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.security.RequireRole;
import com.edtech.platform.payment.facade.InvoiceReadFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import com.edtech.platform.payment.dto.StudentInvoiceDetail;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentPaymentReadController {
    private final InvoiceReadFacade invoices;

    @GetMapping("/invoices/{id}")
    @RequireRole("STUDENT")
    public ApiResponse<StudentInvoiceDetail> invoice(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(invoices.findOwned(id, user.id()).map(StudentInvoiceDetail::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVOICE_NOT_FOUND)));
    }

}
