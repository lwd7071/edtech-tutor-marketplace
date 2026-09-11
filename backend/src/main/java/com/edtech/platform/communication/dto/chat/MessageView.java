package com.edtech.platform.communication.dto.chat;

import com.edtech.platform.communication.domain.MessageType;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;
import com.edtech.platform.common.dto.response.AttachmentView;

@Value
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
    private AttachmentView attachment;
    private Instant sentAt;
    private Instant readAt;
}
