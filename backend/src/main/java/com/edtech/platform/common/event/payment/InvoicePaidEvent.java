package com.edtech.platform.common.event.payment;

import com.edtech.platform.common.event.AbstractDomainEvent;
import lombok.Getter;

import java.util.UUID;

/** Published after an invoice is durably marked paid. */
@Getter
public class InvoicePaidEvent extends AbstractDomainEvent {
    private final UUID invoiceId;
    private final UUID studentId;
    private final long amountVnd;

    public InvoicePaidEvent(UUID invoiceId, UUID studentId, long amountVnd) {
        this.invoiceId = invoiceId;
        this.studentId = studentId;
        this.amountVnd = amountVnd;
    }
}
