package com.edtech.platform.common.event.finance;

import com.edtech.platform.common.event.AbstractDomainEvent;
import lombok.Getter;

import java.util.UUID;

/** Published when an administrator approves a teacher payout. */
@Getter
public class PayoutApprovedEvent extends AbstractDomainEvent {
    private final UUID payoutRequestId;
    private final UUID teacherId;
    private final long amountVnd;

    public PayoutApprovedEvent(UUID payoutRequestId, UUID teacherId, long amountVnd) {
        this.payoutRequestId = payoutRequestId;
        this.teacherId = teacherId;
        this.amountVnd = amountVnd;
    }
}
