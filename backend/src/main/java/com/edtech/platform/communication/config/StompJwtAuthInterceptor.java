package com.edtech.platform.communication.config;

import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class StompJwtAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider tokenProvider;
    private final UserStatusCacheService statuses;
    private final Clock clock;
    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    private record Session(UUID userId, Instant expiresAt) {}

    public StompJwtAuthInterceptor(JwtTokenProvider tokenProvider, UserStatusCacheService statuses, Clock clock) {
        this.tokenProvider = tokenProvider;
        this.statuses = statuses;
        this.clock = clock;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && (StompCommand.CONNECT.equals(accessor.getCommand()) || StompCommand.STOMP.equals(accessor.getCommand()))) {
            boolean authenticated = false;
            List<String> authorization = accessor.getNativeHeader("Authorization");
            if (authorization != null && !authorization.isEmpty()) {
                String bearerToken = authorization.get(0);
                if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
                    String jwt = bearerToken.substring(7);
                    var verified = tokenProvider.parseAccessToken(jwt);
                    AuthenticatedUser user = verified.user();
                    String sessionId = accessor.getSessionId();
                    if (sessionId == null || !active(user.id()) || !verified.expiresAt().isAfter(clock.instant())) {
                        throw new org.springframework.security.access.AccessDeniedException("Authentication required");
                    }
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()) {
                        @Override public String getName() { return user.id().toString(); }
                    };
                    accessor.setUser(auth);
                    accessor.removeNativeHeader("Authorization");
                    sessions.put(sessionId, new Session(user.id(), verified.expiresAt()));
                    authenticated = true;
                }
            }
            if (!authenticated) throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        }
        if (accessor != null && (StompCommand.SEND.equals(accessor.getCommand()) || StompCommand.SUBSCRIBE.equals(accessor.getCommand()))) {
            Session session = accessor.getSessionId() == null ? null : sessions.get(accessor.getSessionId());
            if (accessor.getUser() == null || session == null
                    || !session.userId().toString().equals(accessor.getUser().getName()) || !valid(session)) {
                throw new org.springframework.security.access.AccessDeniedException("Authentication required");
            }
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

    ChannelInterceptor outboundInterceptor() {
        return new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                if (SimpMessageHeaderAccessor.getMessageType(message.getHeaders()) != SimpMessageType.MESSAGE) return message;
                String id = SimpMessageHeaderAccessor.getSessionId(message.getHeaders());
                Session session = id == null ? null : sessions.get(id);
                try {
                    return session != null && valid(session) ? message : null;
                } catch (RuntimeException ex) {
                    return null;
                }
            }
        };
    }

    @EventListener
    public void disconnected(SessionDisconnectEvent event) {
        sessions.remove(event.getSessionId());
    }

    private boolean valid(Session session) {
        return session.expiresAt().isAfter(clock.instant()) && active(session.userId());
    }

    private boolean active(UUID userId) {
        return "ACTIVE".equals(statuses.resolve(userId).orElse(null));
    }
}
