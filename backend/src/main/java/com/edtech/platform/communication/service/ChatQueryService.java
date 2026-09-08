package com.edtech.platform.communication.service;

import com.edtech.platform.auth.facade.IdentityFacade;
import com.edtech.platform.auth.facade.dto.IdentitySnapshot;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.communication.domain.Conversation;
import com.edtech.platform.communication.dto.chat.ConversationView;
import com.edtech.platform.communication.dto.chat.MessageView;
import com.edtech.platform.communication.repository.ConversationRepository;
import com.edtech.platform.communication.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatQueryService {
    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final IdentityFacade identities;
    private final ChatMembership membership;
    private final MessageViewMapper views;

    @Transactional(readOnly = true)
    public Page<ConversationView> getConversations(UUID userId, Pageable pageable) {
        Page<Conversation> page = conversations.findByUserId(userId, pageable);
        if (page.isEmpty()) return Page.empty(pageable);

        Map<UUID, UUID> teacherAccounts = membership.teacherAccountIds(page.getContent());
        Map<UUID, UUID> participants = page.getContent().stream().collect(Collectors.toMap(
                Conversation::getId,
                conversation -> teacherAccounts.get(conversation.getId()).equals(userId)
                        ? conversation.getStudentId() : teacherAccounts.get(conversation.getId())));
        Map<UUID, IdentitySnapshot> participantDetails = identities.getIdentities(participants.values());
        Map<UUID, MessageRepository.ConversationSummary> summaries = messages.summarizeConversations(
                        page.getContent().stream().map(Conversation::getId).toList(), userId).stream()
                .collect(Collectors.toMap(MessageRepository.ConversationSummary::getConversationId, Function.identity()));

        return page.map(conversation -> toView(
                conversation,
                participants.get(conversation.getId()),
                participantDetails,
                summaries.get(conversation.getId())));
    }

    @Transactional(readOnly = true)
    public Page<MessageView> getMessages(UUID conversationId, UUID userId, Pageable pageable) {
        Conversation conversation = conversations.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
        membership.requireMember(conversation, userId);
        return messages.findByConversationId(conversationId, pageable).map(views::toView);
    }

    private ConversationView toView(
            Conversation conversation,
            UUID participantId,
            Map<UUID, IdentitySnapshot> participantDetails,
            MessageRepository.ConversationSummary summary) {
        IdentitySnapshot participant = participantDetails.get(participantId);
        return ConversationView.builder()
                .id(conversation.getId())
                .teacherId(conversation.getTeacherId())
                .studentId(conversation.getStudentId())
                .participantId(participantId)
                .participantName(participant == null ? "Người dùng" : participant.fullName())
                .participantAvatar(participant == null ? null : participant.avatarUrl())
                .lastMessagePreview(summary == null ? null : summary.getLastMessagePreview())
                .unreadCount(summary == null ? 0 : summary.getUnreadCount())
                .lastMessageAt(conversation.getLastMessageAt())
                .createdAt(conversation.getCreatedAt())
                .build();
    }
}
