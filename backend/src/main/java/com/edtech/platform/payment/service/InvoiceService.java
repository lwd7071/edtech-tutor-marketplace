package com.edtech.platform.payment.service;

import com.edtech.platform.payment.domain.Invoice;
import com.edtech.platform.payment.dto.InvoiceDetail;

import java.util.UUID;

public interface InvoiceService {
    
    /**
     * Creates an invoice in PENDING state and calls the payment gateway to generate a payment link.
     * The payment gateway call MUST NOT hold the database transaction.
     *
     * @param studentId the student UUID
     * @param pricingPackageId the pricing package UUID
     * @param idempotencyKey unique key to prevent duplicate creation
     * @return the created Invoice with checkout details attached
     */
    InvoiceDetail createInvoiceAndPaymentLink(UUID studentId, UUID pricingPackageId, UUID idempotencyKey);
    InvoiceDetail createInvoiceAndPaymentLink(UUID studentId, UUID pricingPackageId, UUID idempotencyKey, String returnUrl, String cancelUrl);
}
