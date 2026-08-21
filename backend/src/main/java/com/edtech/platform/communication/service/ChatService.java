package com.edtech.platform.communication.service;

import com.edtech.platform.common.domain.AttachableType;
import com.edtech.platform.common.domain.Attachment;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.repository.AttachmentRepository;
import com.edtech.platform.communication.domain.Conversation;
import com.edtech.platform.communication.domain.Message;
import com.edtech.platform.communication.domain.MessageType;
import com.edtech.platform.communication.dto.chat.ChatMessageRequest;
import com.edtech.platform.communication.dto.chat.MessageView;
import com.edtech.platform.communication.repository.ConversationRepository;
import com.edtech.platform.communication.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final AttachmentRepository attachmentRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    @Transactional
    public MessageView sendMessage(UUID senderId, ChatMessageRequest request) {
        Conversation conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CONCURRENT_MODIFICATION)); // Should validate in controller first, but just in case
                
        // Validation type
        if (request.getMessageType() == MessageType.TEXT && (request.getContent() == null || request.getContent().trim().isEmpty())) {
            throw new BusinessException(ErrorCode.MESSAGE_TYPE_INVALID);
        }
        if (request.getMessageType() != MessageType.TEXT && request.getAttachmentId() == null) {
            throw new BusinessException(ErrorCode.MESSAGE_TYPE_INVALID);
        }

        // Idempotency
        Optional<Message> existing = messageRepository.findBySenderIdAndClientMessageId(senderId, request.getClientMessageId());
        if (existing.isPresent()) {
            Message old = existing.get();
            if (old.getMessageType() == request.getMessageType()) {
                return mapToView(old);
            }
            throw new BusinessException(ErrorCode.MESSAGE_DUPLICATE);
        }

        // Handle attachment
        Attachment attachment = null;
        if (request.getAttachmentId() != null) {
            attachment = attachmentRepository.findById(request.getAttachmentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ATTACHMENT_NOT_FOUND));
            if (!attachment.getOwner().getId().equals(senderId)) {
                throw new BusinessException(ErrorCode.ATTACHMENT_CONTEXT_INVALID);
            }
            if (!attachment.getAttachableType().equals(AttachableType.MESSAGE.name())) {
                throw new BusinessException(ErrorCode.ATTACHMENT_CONTEXT_INVALID);
            }
            if (!attachment.getAttachableId().equals(attachment.getId())) {
                throw new BusinessException(ErrorCode.ATTACHMENT_CONTEXT_INVALID);
            }
        }

        Message message = Message.builder()
                .conversationId(conversation.getId())
                .senderId(senderId)
                .clientMessageId(request.getClientMessageId())
                .messageType(request.getMessageType())
                .content(request.getContent())
                .attachmentId(attachment != null ? attachment.getId() : null)
                .build();

        message = messageRepository.save(message);

        if (attachment != null) {
            attachment.setAttachableId(message.getId());
            attachmentRepository.save(attachment);
        }

        conversation.markLastMessageAt(message.getSentAt());
        conversationRepository.save(conversation);

        MessageView view = mapToView(message);
        
        UUID receiverId = conversation.getTeacherId().equals(senderId) ? conversation.getStudentId() : conversation.getTeacherId();

        // Send STOMP
        messagingTemplate.convertAndSendToUser(receiverId.toString(), "/queue/messages", view);
        messagingTemplate.convertAndSendToUser(senderId.toString(), "/queue/messages", view);

        // Notification
        notificationService.createNotification(
                receiverId,
                "NEW_MESSAGE",
                "Bạn có tin nhắn mới",
                "Bạn vừa nhận được một tin nhắn mới",
                "MESSAGE",
                message.getId()
        );

        return view;
    }

    @Transactional
    public void readMessages(UUID conversationId, UUID readerId) {
        int updatedCount = messageRepository.markMessagesAsRead(conversationId, readerId);
        if (updatedCount > 0) {
            Conversation conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
            UUID senderId = conversation.getTeacherId().equals(readerId) ? conversation.getStudentId() : conversation.getTeacherId();
            
            // Notify the other person that their messages were read
            // Send a tiny payload just to trigger the seen status
            messagingTemplate.convertAndSendToUser(
                    senderId.toString(),
                    "/queue/messages/seen",
                    conversationId
            );
        }
    }

    public MessageView mapToView(Message m) {
        String attachmentUrl = null;
        if (m.getAttachmentId() != null) {
            attachmentUrl = attachmentRepository.findById(m.getAttachmentId())
                    .map(Attachment::getSecureUrl).orElse(null);
        }
        return MessageView.builder()
                .id(m.getId())
                .conversationId(m.getConversationId())
                .senderId(m.getSenderId())
                .clientMessageId(m.getClientMessageId())
                .messageType(m.getMessageType())
                .content(m.getContent())
                .attachmentId(m.getAttachmentId())
                .attachmentUrl(attachmentUrl)
                .sentAt(m.getSentAt())
                .readAt(m.getReadAt())
                .build();
    }
}
