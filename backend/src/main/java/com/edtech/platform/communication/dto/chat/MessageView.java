package com.edtech.platform.communication.dto.chat;

import com.edtech.platform.communication.domain.MessageType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class MessageView {
    private UUID id;
    private UUID conversationId;
    private UUID senderId;
    private UUID clientMessageId;
    private MessageType messageType;
    private String content;
    private UUID attachmentId;
    private String attachmentUrl;
    private Instant sentAt;
    private Instant readAt;
}
