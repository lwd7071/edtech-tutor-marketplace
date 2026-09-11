package com.edtech.platform.payment.controller;

import com.edtech.platform.payment.domain.Invoice;
import com.edtech.platform.payment.dto.InvoiceCreationRequest;
import com.edtech.platform.payment.dto.InvoiceCreationResponse;
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
    public ResponseEntity<ApiResponse<InvoiceCreationResponse>> createInvoice(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody InvoiceCreationRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal AuthenticatedUser user
    ) {
        String limitKey = (user != null && user.id() != null) ? user.id().toString() : "anonymous";
        rateLimiterService.checkRateLimit("create_invoice", limitKey, 10, 60);
        Invoice invoice = invoiceService.createInvoiceAndPaymentLink(
                user.id(),
                request.getPricingPackageId(),
                idempotencyKey,
                request.getReturnUrl(),
                request.getCancelUrl()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(toResponse(invoice)));
    }

    private InvoiceCreationResponse toResponse(Invoice invoice) {
        return InvoiceCreationResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .status(invoice.getStatus().name())
                .checkoutUrl(invoice.getCheckoutUrl())
                .qrCode(invoice.getQrCode())
                .amountVnd(invoice.getAmountVnd())
                .build();
    }
}
