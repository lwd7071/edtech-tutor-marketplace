package com.edtech.platform.payment.controller;

import com.edtech.platform.payment.domain.Invoice;
import com.edtech.platform.payment.dto.CreateInvoiceRequest;
import com.edtech.platform.payment.dto.InvoiceDetail;
import com.edtech.platform.payment.service.InvoiceService;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.RateLimiterService;
import com.edtech.platform.common.security.RequireRole;
import com.edtech.platform.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/student/invoices")
@RequiredArgsConstructor
@RequireRole("STUDENT")
public class StudentInvoiceController {

    private final InvoiceService invoiceService;
    private final RateLimiterService rateLimiterService;

    @PostMapping
    public ResponseEntity<ApiResponse<InvoiceDetail>> createInvoice(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody CreateInvoiceRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal AuthenticatedUser user
    ) {
        String limitKey = (user != null && user.id() != null) ? user.id().toString() : "anonymous";
        rateLimiterService.checkRateLimit("create_invoice", limitKey, 10, 60);
        Invoice invoice = invoiceService.createInvoiceAndPaymentLink(
                user.id(),
                request.pricingPackageId(),
                idempotencyKey,
                request.returnUrl(),
                request.cancelUrl()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(InvoiceDetail.from(invoice)));
    }
}
