package com.edtech.platform.communication.service;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.facade.AttachmentFacade;
import com.edtech.platform.communication.domain.Conversation;
import com.edtech.platform.communication.domain.Message;
import com.edtech.platform.communication.domain.MessageType;
import com.edtech.platform.communication.dto.chat.ChatMessageRequest;
import com.edtech.platform.communication.dto.chat.MessageView;
import com.edtech.platform.communication.event.ChatMessageCommitted;
import com.edtech.platform.communication.event.ChatMessagesRead;
import com.edtech.platform.communication.repository.ConversationRepository;
import com.edtech.platform.communication.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatCommandService {
    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final AttachmentFacade attachments;
    private final ChatMembership membership;
    private final MessageViewMapper views;
    private final ApplicationEventPublisher events;

    @Transactional
    public MessageView sendMessage(UUID senderId, ChatMessageRequest request) {
        validateRequest(request);
        Conversation conversation = conversations.findByIdForUpdate(request.getConversationId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CONCURRENT_MODIFICATION));
        membership.requireMember(conversation, senderId);
        validateContent(request);

        var existing = messages.findBySenderIdAndClientMessageId(senderId, request.getClientMessageId());
        if (existing.isPresent()) {
            Message previous = existing.get();
            if (sameMessage(previous, request)) return views.toView(previous);
            throw new BusinessException(ErrorCode.MESSAGE_DUPLICATE);
        }

        Message message = messages.save(Message.builder()
                .conversationId(conversation.getId())
                .senderId(senderId)
                .clientMessageId(request.getClientMessageId())
                .messageType(request.getMessageType())
                .content(request.getContent())
                .attachmentId(request.getAttachmentId())
                .build());
        if (message.getAttachmentId() != null) {
            attachments.validateAndBind(message.getAttachmentId(), senderId, "MESSAGE", message.getId());
        }
        conversation.markLastMessageAt(message.getSentAt());
        conversations.save(conversation);

        MessageView view = views.toView(message);
        events.publishEvent(new ChatMessageCommitted(
                senderId, membership.otherParticipant(conversation, senderId), view));
        return view;
    }

    @Transactional
    public void readMessages(UUID conversationId, UUID readerId) {
        Conversation conversation = conversations.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
        membership.requireMember(conversation, readerId);
        if (messages.markMessagesAsRead(conversationId, readerId) > 0) {
            events.publishEvent(new ChatMessagesRead(
                    membership.otherParticipant(conversation, readerId), conversationId));
        }
    }

    private void validateRequest(ChatMessageRequest request) {
        if (request.getClientMessageId() == null || request.getConversationId() == null
                || request.getMessageType() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private void validateContent(ChatMessageRequest request) {
        if (request.getMessageType() == MessageType.TEXT
                && (request.getContent() == null || request.getContent().trim().isEmpty())) {
            throw new BusinessException(ErrorCode.MESSAGE_TYPE_INVALID);
        }
        if (request.getMessageType() != MessageType.TEXT && request.getAttachmentId() == null) {
            throw new BusinessException(ErrorCode.MESSAGE_TYPE_INVALID);
        }
    }

    private boolean sameMessage(Message message, ChatMessageRequest request) {
        return message.getMessageType() == request.getMessageType()
                && message.getConversationId().equals(request.getConversationId())
                && Objects.equals(message.getContent(), request.getContent())
                && Objects.equals(message.getAttachmentId(), request.getAttachmentId());
    }
}
