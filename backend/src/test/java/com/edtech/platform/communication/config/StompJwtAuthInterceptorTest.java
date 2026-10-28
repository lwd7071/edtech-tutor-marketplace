package com.edtech.platform.communication.config;

import com.edtech.platform.common.config.properties.JwtProperties;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StompJwtAuthInterceptorTest {
    private final MutableClock clock = new MutableClock();
    private final JwtTokenProvider tokens = new JwtTokenProvider(new JwtProperties(
            "this-is-a-long-enough-secret-for-hmac-sha256", 60000, 60000), clock);
    private final UserStatusCacheService statuses = mock(UserStatusCacheService.class);
    private final StompJwtAuthInterceptor interceptor = new StompJwtAuthInterceptor(tokens, statuses, clock);
    private final UUID id = UUID.randomUUID();

    @Test void lockedAccountCannotConnect() {
        when(statuses.resolve(id)).thenReturn(Optional.of("LOCKED"));
        assertThrows(AccessDeniedException.class, () -> connect("s1"));
    }

    @Test void lockedAccountCannotSendOrReceiveThroughExistingSubscription() {
        when(statuses.resolve(id)).thenReturn(Optional.of("ACTIVE"));
        connect("s1");
        when(statuses.resolve(id)).thenReturn(Optional.of("LOCKED"));
        assertThrows(AccessDeniedException.class, () -> inbound(StompCommand.SEND, "s1", "/app/chat.send"));
        assertNull(interceptor.outboundInterceptor().preSend(outbound("s1"), null));
    }

    @Test void activeAccountCanSendAndSubscribe() {
        when(statuses.resolve(id)).thenReturn(Optional.of("ACTIVE"));
        connect("s1");
        assertDoesNotThrow(() -> inbound(StompCommand.SEND, "s1", "/app/chat.send"));
        assertDoesNotThrow(() -> inbound(StompCommand.SUBSCRIBE, "s1", "/user/queue/messages"));
    }

    @Test void expirationStopsExistingSessionEvenWhenAnotherSessionIsValid() {
        when(statuses.resolve(id)).thenReturn(Optional.of("ACTIVE"));
        connect("s1");
        clock.now = clock.now.plusSeconds(30);
        connect("s2");
        clock.now = clock.now.plusSeconds(31);
        assertNull(interceptor.outboundInterceptor().preSend(outbound("s1"), null));
        assertNotNull(interceptor.outboundInterceptor().preSend(outbound("s2"), null));
        assertThrows(AccessDeniedException.class, () -> inbound(StompCommand.SUBSCRIBE, "s1", "/user/queue/messages"));
    }

    @Test void unknownSessionCannotReceiveMessage() {
        assertNull(interceptor.outboundInterceptor().preSend(outbound("missing"), null));
    }

    @Test void anonymousConnectIsRejected() {
        var headers = StompHeaderAccessor.create(StompCommand.CONNECT);
        headers.setSessionId("s1");
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message(headers), null));
    }

    private void connect(String session) {
        var headers = StompHeaderAccessor.create(StompCommand.CONNECT);
        headers.setSessionId(session);
        headers.addNativeHeader("Authorization", "Bearer " + tokens.generateAccessToken(new AuthenticatedUser(id, "a@example.test", "STUDENT")));
        headers.setLeaveMutable(true);
        interceptor.preSend(message(headers), null);
    }

    private void inbound(StompCommand command, String session, String destination) {
        var headers = StompHeaderAccessor.create(command);
        headers.setSessionId(session);
        headers.setDestination(destination);
        headers.setUser(new UsernamePasswordAuthenticationToken(id.toString(), null));
        interceptor.preSend(message(headers), null);
    }

    private Message<byte[]> outbound(String session) {
        var headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headers.setSessionId(session);
        return MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
    }

    private Message<byte[]> message(StompHeaderAccessor headers) {
        return MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
    }

    private static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-10-28T00:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
