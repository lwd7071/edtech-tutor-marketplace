package com.edtech.platform.payment.facade.dto;

import java.time.Instant;
import java.util.UUID;

public record InvoiceSnapshot(UUID id, String invoiceNumber, UUID pricingPackageId, long amountVnd,
                              String status, String checkoutUrl, String qrCode,
                              Instant paymentExpiredAt, Instant paidAt) {}
