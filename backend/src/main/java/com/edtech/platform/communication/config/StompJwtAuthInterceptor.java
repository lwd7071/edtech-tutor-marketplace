package com.edtech.platform.communication.config;

import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Configuration
public class StompJwtAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider tokenProvider;

    public StompJwtAuthInterceptor(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            List<String> authorization = accessor.getNativeHeader("Authorization");
            if (authorization != null && !authorization.isEmpty()) {
                String bearerToken = authorization.get(0);
                if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
                    String jwt = bearerToken.substring(7);
                    AuthenticatedUser user = tokenProvider.getAuthenticatedUserFromToken(jwt);
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()) {
                        @Override public String getName() { return user.id().toString(); }
                    };
                    accessor.setUser(auth);
                }
            }
            if (accessor.getUser() == null) throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        }
        if (accessor != null && (StompCommand.SEND.equals(accessor.getCommand()) || StompCommand.SUBSCRIBE.equals(accessor.getCommand()))) {
            if (accessor.getUser() == null) throw new org.springframework.security.access.AccessDeniedException("Authentication required");
            String destination = accessor.getDestination();
            boolean allowed;
            if (StompCommand.SEND.equals(accessor.getCommand())) {
                allowed = "/app/chat.send".equals(destination) || "/app/chat.read".equals(destination);
            } else {
                String accountId = accessor.getUser().getName();
                allowed = java.util.Set.of("/user/queue/messages", "/user/queue/messages/seen", "/user/queue/notifications", "/user/queue/errors").contains(destination == null ? "" : destination)
                        || ("/topic/bookings." + accountId).equals(destination)
                        || ("/topic/invoices." + accountId).equals(destination)
                        || ("/topic/wallet." + accountId).equals(destination);
            }
            if (!allowed) throw new org.springframework.security.access.AccessDeniedException("Destination not allowed");
        }
        return message;
    }
}
