package com.edtech.platform.communication.service;

import com.edtech.platform.common.facade.AttachmentFacade;
import com.edtech.platform.communication.domain.Message;
import com.edtech.platform.communication.dto.chat.MessageView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageViewMapper {
    private final AttachmentFacade attachments;

    public MessageView toView(Message message) {
        var attachment = message.getAttachmentId() == null ? null : attachments.getAttachmentView(message.getAttachmentId());
        String attachmentUrl = attachment == null ? null : attachment.getSecureUrl();
        return MessageView.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .clientMessageId(message.getClientMessageId())
                .messageType(message.getMessageType())
                .content(message.getContent())
                .attachmentId(message.getAttachmentId())
                .attachmentUrl(attachmentUrl)
                .attachment(attachment)
                .sentAt(message.getSentAt())
                .readAt(message.getReadAt())
                .build();
    }
}
