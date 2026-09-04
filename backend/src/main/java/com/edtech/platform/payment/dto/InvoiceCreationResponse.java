package com.edtech.platform.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceCreationResponse {
    private String invoiceNumber;
    private String status;
    private String checkoutUrl;
    private String qrCode;
    private long amountVnd;
}
