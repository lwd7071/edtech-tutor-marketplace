package com.edtech.platform.common.event.payment;

import com.edtech.platform.common.event.AbstractDomainEvent;
import lombok.Getter;

import java.util.UUID;

@Getter
public class PaymentSucceededEvent extends AbstractDomainEvent {
    private final UUID studentId;
    private final UUID teacherId;
    private final UUID invoiceId;
    private final UUID pricingPackageId;
    private final String packageName;
    private final long amountVnd;

    public PaymentSucceededEvent(UUID studentId, UUID teacherId, UUID invoiceId, UUID pricingPackageId, String packageName, long amountVnd) {
        super();
        this.studentId = studentId;
        this.teacherId = teacherId;
        this.invoiceId = invoiceId;
        this.pricingPackageId = pricingPackageId;
        this.packageName = packageName;
        this.amountVnd = amountVnd;
    }
}
