package com.edtech.platform.communication.controller;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.response.PageMeta;
import com.edtech.platform.communication.domain.Conversation;
import com.edtech.platform.communication.dto.chat.ConversationView;
import com.edtech.platform.communication.dto.chat.MessageView;
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

    private final ChatService chatService;

    @GetMapping
    public ApiResponse<java.util.List<ConversationView>> getConversations(
            @AuthenticationPrincipal AuthenticatedUser user,
            Pageable pageable) {
        Page<ConversationView> page = chatService.getConversations(user.getId(), pageable);
        return ApiResponse.page(page.getContent(), PageMeta.from(page));
    }

    @GetMapping("/{id}/messages")
    public ApiResponse<java.util.List<MessageView>> getMessages(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            Pageable pageable) {
        Page<MessageView> page = chatService.getMessages(id, user.getId(), pageable);
        return ApiResponse.page(page.getContent(), PageMeta.from(page));
    }
}
