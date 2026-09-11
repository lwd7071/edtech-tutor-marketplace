package com.edtech.platform.communication.controller;

import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.response.PageMeta;
import com.edtech.platform.communication.dto.chat.ConversationView;
import com.edtech.platform.communication.dto.chat.MessageView;
import com.edtech.platform.communication.service.ChatQueryService;
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

    private final ChatQueryService chatQueries;
    @GetMapping
    @com.edtech.platform.common.security.RequireRole({"STUDENT", "TEACHER"})
    public ApiResponse<java.util.List<ConversationView>> getConversations(
            @AuthenticationPrincipal AuthenticatedUser user,
            Pageable pageable) {
        Page<ConversationView> page = chatQueries.getConversations(user.getId(), pageable);
        return ApiResponse.page(page.getContent(), PageMeta.from(page));
    }

    @GetMapping("/{id}/messages")
    @com.edtech.platform.common.security.RequireRole({"STUDENT", "TEACHER"})
    public ApiResponse<java.util.List<MessageView>> getMessages(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            Pageable pageable) {
        Page<MessageView> page = chatQueries.getMessages(id, user.getId(), pageable);
        return ApiResponse.page(page.getContent(), PageMeta.from(page));
    }
}
