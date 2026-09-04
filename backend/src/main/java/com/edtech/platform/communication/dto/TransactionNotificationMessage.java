package com.edtech.platform.communication.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionNotificationMessage {
    private String eventType;
    private String message;
    private UUID referenceId;
    private Instant timestamp;

    public TransactionNotificationMessage(String eventType, String message, UUID referenceId) {
        this.eventType = eventType;
        this.message = message;
        this.referenceId = referenceId;
        this.timestamp = Instant.now();
    }
}
