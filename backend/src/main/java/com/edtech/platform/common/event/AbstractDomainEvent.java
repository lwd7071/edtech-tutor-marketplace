package com.edtech.platform.common.event;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public abstract class AbstractDomainEvent implements DomainEvent {
    private final UUID eventId;
    private final Instant occurredAt;

    protected AbstractDomainEvent() {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
    }
}
