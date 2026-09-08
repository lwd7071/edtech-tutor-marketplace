package com.edtech.platform.communication.dto.notification;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class NotificationView {
    private UUID id;
    private UUID userId;
    private String type;
    private String title;
    private String content;
    private String referenceType;
    private UUID referenceId;
    private String referenceUrl;
    private boolean isRead;
    private Instant createdAt;
}
