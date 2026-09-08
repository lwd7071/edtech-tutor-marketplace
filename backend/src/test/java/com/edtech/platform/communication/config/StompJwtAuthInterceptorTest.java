package com.edtech.platform.communication.config;

import com.edtech.platform.common.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class StompJwtAuthInterceptorTest {
    @Test void anonymousConnectIsRejected() {
        var interceptor = new StompJwtAuthInterceptor(mock(JwtTokenProvider.class));
        var accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        var message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> interceptor.preSend(message, null));
    }
    @Test void anonymousSubscriptionCannotReadPrivateTopics() {
        var interceptor = new StompJwtAuthInterceptor(mock(JwtTokenProvider.class));
        var accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/wallet.someone-else");
        var message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> interceptor.preSend(message, null));
    }
}
