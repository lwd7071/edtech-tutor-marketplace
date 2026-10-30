package com.edtech.platform.assistant.controller;

import com.edtech.platform.assistant.dto.AssistantChatRequest;
import com.edtech.platform.assistant.dto.AssistantChatResponse;
import com.edtech.platform.assistant.service.AssistantChatService;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/assistant")
public class AssistantChatController {
    private final AssistantChatService assistant;
    private final RateLimiterService rateLimiter;

    public AssistantChatController(AssistantChatService assistant, RateLimiterService rateLimiter) {
        this.assistant = assistant;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<AssistantChatResponse>> chat(
            @Valid @RequestBody AssistantChatRequest request,
            @AuthenticationPrincipal AuthenticatedUser user,
            HttpServletRequest servletRequest) {
        String identifier = user != null && user.id() != null
                ? user.id().toString()
                : servletRequest.getRemoteAddr();
        rateLimiter.checkRateLimit("assistant_chat", identifier == null ? "unknown" : identifier, 10, 300);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(assistant.reply(request)));
    }
}
