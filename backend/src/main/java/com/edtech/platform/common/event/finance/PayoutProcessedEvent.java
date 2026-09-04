package com.edtech.platform.common.event.finance;

import com.edtech.platform.common.event.AbstractDomainEvent;
import lombok.Getter;

import java.util.UUID;

@Getter
public class PayoutProcessedEvent extends AbstractDomainEvent {
    private final UUID payoutRequestId;
    private final UUID teacherId;
    private final long amountVnd;
    private final String status;

    public PayoutProcessedEvent(UUID payoutRequestId, UUID teacherId, long amountVnd, String status) {
        super();
        this.payoutRequestId = payoutRequestId;
        this.teacherId = teacherId;
        this.amountVnd = amountVnd;
        this.status = status;
    }
}
