package com.edtech.platform.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceCreationRequest {
    /** Ignored by the controller; ownership comes from the authenticated principal. */
    private UUID studentId;

    @NotNull(message = "pricingPackageId is required")
    private UUID pricingPackageId;

    private String returnUrl;
    private String cancelUrl;
}
