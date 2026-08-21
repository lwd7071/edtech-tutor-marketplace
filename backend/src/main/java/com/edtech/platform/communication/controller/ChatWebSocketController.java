package com.edtech.platform.communication.controller;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.communication.domain.Conversation;
import com.edtech.platform.communication.dto.chat.ChatMessageRequest;
import com.edtech.platform.communication.dto.chat.ChatReadRequest;
import com.edtech.platform.communication.repository.ConversationRepository;
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
    private final ConversationRepository conversationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageRequest request, Principal principal) {
        AuthenticatedUser user = getAuthenticatedUser(principal);
        if (user == null) return;

        Conversation conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));

        if (!conversation.getTeacherId().equals(user.getId()) && !conversation.getStudentId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND);
        }

        chatService.sendMessage(user.getId(), request);
    }

    @MessageMapping("/chat.read")
    public void readMessages(@Payload ChatReadRequest request, Principal principal) {
        AuthenticatedUser user = getAuthenticatedUser(principal);
        if (user == null) return;

        Conversation conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));

        if (!conversation.getTeacherId().equals(user.getId()) && !conversation.getStudentId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND);
        }

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
