package com.edtech.platform.communication.dto.chat;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ConversationView {
    private UUID id;
    private UUID teacherId;
    private UUID studentId;
    private Instant lastMessageAt;
    private Instant createdAt;
}
