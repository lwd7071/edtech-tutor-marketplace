package com.edtech.platform.communication.service;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.facade.AttachmentFacade;
import com.edtech.platform.auth.facade.IdentityFacade;
import com.edtech.platform.auth.facade.dto.IdentitySnapshot;
import com.edtech.platform.communication.domain.Conversation;
import com.edtech.platform.communication.domain.Message;
import com.edtech.platform.communication.domain.MessageType;
import com.edtech.platform.communication.dto.chat.ChatMessageRequest;
import com.edtech.platform.communication.dto.chat.MessageView;
import com.edtech.platform.communication.dto.chat.ConversationView;
import com.edtech.platform.communication.repository.ConversationRepository;
import com.edtech.platform.communication.repository.MessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final AttachmentFacade attachmentFacade;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;
    private final IdentityFacade identityFacade;
    private final com.edtech.platform.teacher.facade.TeacherFacade teacherFacade;

    @Transactional
    public MessageView sendMessage(UUID senderId, ChatMessageRequest request) {
        if (request.getClientMessageId() == null || request.getConversationId() == null || request.getMessageType() == null)
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        Conversation conversation = conversationRepository.findByIdForUpdate(request.getConversationId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CONCURRENT_MODIFICATION)); // Should validate in controller first, but just in case
        if (!senderId.equals(teacherAccountId(conversation)) && !senderId.equals(conversation.getStudentId()))
            throw new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND);
                
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
            if (old.getMessageType() == request.getMessageType() && old.getConversationId().equals(request.getConversationId())
                    && java.util.Objects.equals(old.getContent(), request.getContent()) && java.util.Objects.equals(old.getAttachmentId(), request.getAttachmentId())) {
                return mapToView(old);
            }
            throw new BusinessException(ErrorCode.MESSAGE_DUPLICATE);
        }

        // Skip manual attachment validation here, will bind after saving message

        Message message = Message.builder()
                .conversationId(conversation.getId())
                .senderId(senderId)
                .clientMessageId(request.getClientMessageId())
                .messageType(request.getMessageType())
                .content(request.getContent())
                .attachmentId(request.getAttachmentId())
                .build();

        message = messageRepository.save(message);

        if (request.getAttachmentId() != null) {
            attachmentFacade.validateAndBind(request.getAttachmentId(), senderId, "MESSAGE", message.getId());
        }

        conversation.markLastMessageAt(message.getSentAt());
        conversationRepository.save(conversation);

        MessageView view = mapToView(message);
        
        UUID receiverId = teacherAccountId(conversation).equals(senderId) ? conversation.getStudentId() : teacherAccountId(conversation);

        // Send STOMP
        com.edtech.platform.common.transaction.AfterCommit.run(() -> {
            messagingTemplate.convertAndSendToUser(receiverId.toString(), "/queue/messages", view);
            messagingTemplate.convertAndSendToUser(senderId.toString(), "/queue/messages", view);
        });

        // Notification
        com.edtech.platform.common.transaction.AfterCommit.run(() -> notificationService.createNotification(
                receiverId,
                "NEW_MESSAGE",
                "Bạn có tin nhắn mới",
                "Bạn vừa nhận được một tin nhắn mới",
                "MESSAGE",
                view.getId()
        ));

        return view;
    }

    @Transactional
    public void readMessages(UUID conversationId, UUID readerId) {
        Conversation membership = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
        if (!readerId.equals(teacherAccountId(membership)) && !readerId.equals(membership.getStudentId()))
            throw new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND);
        int updatedCount = messageRepository.markMessagesAsRead(conversationId, readerId);
        if (updatedCount > 0) {
            Conversation conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
            UUID senderId = teacherAccountId(conversation).equals(readerId) ? conversation.getStudentId() : teacherAccountId(conversation);
            
            // Notify the other person that their messages were read
            // Send a tiny payload just to trigger the seen status
            com.edtech.platform.common.transaction.AfterCommit.run(() -> messagingTemplate.convertAndSendToUser(
                    senderId.toString(),
                    "/queue/messages/seen",
                    conversationId
            ));
        }
    }

    @Transactional(readOnly = true)
    public Page<ConversationView> getConversations(UUID userId, Pageable pageable) {
        return conversationRepository.findByUserId(userId, pageable)
                .map(c -> {
                    UUID participantId = teacherAccountId(c).equals(userId) ? c.getStudentId() : teacherAccountId(c);
                    IdentitySnapshot participant = identityFacade.getIdentity(participantId).orElse(null);
                    String preview = messageRepository
                            .findFirstByConversationIdAndDeletedFalseOrderBySentAtDesc(c.getId())
                            .map(Message::getContent).orElse(null);
                    return ConversationView.builder()
                            .id(c.getId())
                            .teacherId(c.getTeacherId())
                            .studentId(c.getStudentId())
                            .participantId(participantId)
                            .participantName(participant == null ? "Người dùng" : participant.fullName())
                            .participantAvatar(participant == null ? null : participant.avatarUrl())
                            .lastMessagePreview(preview)
                            .unreadCount(messageRepository.countByConversationIdAndSenderIdNotAndReadAtIsNullAndDeletedFalse(c.getId(), userId))
                            .lastMessageAt(c.getLastMessageAt())
                            .createdAt(c.getCreatedAt())
                            .build();
                });
    }

    @Transactional(readOnly = true)
    public Page<MessageView> getMessages(UUID conversationId, UUID userId, Pageable pageable) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));

        if (!teacherAccountId(conversation).equals(userId) && !conversation.getStudentId().equals(userId)) {
            throw new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND);
        }

        return messageRepository.findByConversationId(conversationId, pageable)
                .map(this::mapToView);
    }

    private UUID teacherAccountId(Conversation conversation) {
        return teacherFacade.getTeacher(conversation.getTeacherId()).userId();
    }

    public MessageView mapToView(Message m) {
        String attachmentUrl = null;
        if (m.getAttachmentId() != null) {
            attachmentUrl = attachmentFacade.getAttachmentView(m.getAttachmentId()).getSecureUrl();
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

