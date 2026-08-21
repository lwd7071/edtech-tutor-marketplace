package com.edtech.platform.communication.dto.chat;

import com.edtech.platform.communication.domain.MessageType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ChatMessageRequest {
    @NotNull
    private UUID clientMessageId;
    @NotNull
    private UUID conversationId;
    @NotNull
    private MessageType messageType;
    
    private String content;
    private UUID attachmentId;
}
