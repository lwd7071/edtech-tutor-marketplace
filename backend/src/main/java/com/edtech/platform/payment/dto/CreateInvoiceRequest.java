package com.edtech.platform.payment.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateInvoiceRequest(
        @NotNull(message = "pricingPackageId is required") UUID pricingPackageId,
        String returnUrl,
        String cancelUrl
) {}
