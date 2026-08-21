package com.edtech.platform.communication.controller;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.communication.domain.Conversation;
import com.edtech.platform.communication.dto.chat.ConversationView;
import com.edtech.platform.communication.dto.chat.MessageView;
import com.edtech.platform.communication.repository.ConversationRepository;
import com.edtech.platform.communication.repository.MessageRepository;
import com.edtech.platform.communication.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ChatService chatService;

    @GetMapping
    public Page<ConversationView> getConversations(
            @AuthenticationPrincipal AuthenticatedUser user,
            Pageable pageable) {
        return conversationRepository.findByUserId(user.getId(), pageable)
                .map(c -> ConversationView.builder()
                        .id(c.getId())
                        .teacherId(c.getTeacherId())
                        .studentId(c.getStudentId())
                        .lastMessageAt(c.getLastMessageAt())
                        .createdAt(c.getCreatedAt())
                        .build());
    }

    @GetMapping("/{id}/messages")
    public Page<MessageView> getMessages(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            Pageable pageable) {
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));

        if (!conversation.getTeacherId().equals(user.getId()) && !conversation.getStudentId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND);
        }

        return messageRepository.findByConversationId(id, pageable)
                .map(chatService::mapToView);
    }
}
