package com.edtech.platform.communication.controller;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.communication.dto.chat.ChatMessageRequest;
import com.edtech.platform.communication.dto.chat.ChatReadRequest;
import com.edtech.platform.communication.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(@jakarta.validation.Valid @Payload ChatMessageRequest request, Principal principal) {
        AuthenticatedUser user = getAuthenticatedUser(principal);
        if (user == null) return;

        chatService.sendMessage(user.getId(), request);
    }

    @MessageMapping("/chat.read")
    public void readMessages(@jakarta.validation.Valid @Payload ChatReadRequest request, Principal principal) {
        AuthenticatedUser user = getAuthenticatedUser(principal);
        if (user == null) return;

        chatService.readMessages(request.getConversationId(), user.getId());
    }

    @MessageExceptionHandler
    public void handleException(BusinessException exception, Principal principal) {
        if (principal != null) {
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/errors",
                    Map.of("errorCode", exception.getErrorCode().name(), "message", exception.getMessage())
            );
        }
    }

    @MessageExceptionHandler(org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException.class)
    public void handleValidationException(Principal principal) {
        if (principal != null) messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors",
                Map.of("errorCode", ErrorCode.VALIDATION_ERROR.name(), "message", "Dữ liệu tin nhắn không hợp lệ"));
    }

    private AuthenticatedUser getAuthenticatedUser(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken token) {
            Object configUser = token.getPrincipal();
            if (configUser instanceof AuthenticatedUser) {
                return (AuthenticatedUser) configUser;
            }
        }
        return null;
    }
}

