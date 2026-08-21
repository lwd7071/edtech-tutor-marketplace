package com.edtech.platform.communication.dto.notification;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class NotificationView {
    private UUID id;
    private UUID userId;
    private String type;
    private String title;
    private String content;
    private String referenceType;
    private UUID referenceId;
    private boolean isRead;
    private Instant createdAt;
}
