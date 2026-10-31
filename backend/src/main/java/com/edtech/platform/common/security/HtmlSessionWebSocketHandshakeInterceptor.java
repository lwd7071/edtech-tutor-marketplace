package com.edtech.platform.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class HtmlSessionWebSocketHandshakeInterceptor implements HandshakeInterceptor {
    public static final String ACCESS_EXPIRES_AT_ATTRIBUTE =
            HtmlSessionWebSocketHandshakeInterceptor.class.getName() + ".ACCESS_EXPIRES_AT";

    private final JwtTokenProvider tokens;

    public HtmlSessionWebSocketHandshakeInterceptor(JwtTokenProvider tokens) {
        this.tokens = tokens;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) return true;
        HttpServletRequest httpRequest = servletRequest.getServletRequest();
        HttpSession session = httpRequest.getSession(false);
        if (session == null) return true;
        Object state = session.getAttribute(HtmlSessionAuthenticationFilter.SESSION_STATE_ATTRIBUTE);
        if (state instanceof HtmlSessionState htmlSession) {
            try {
                attributes.put(ACCESS_EXPIRES_AT_ATTRIBUTE,
                        tokens.parseAccessToken(htmlSession.accessToken()).expiresAt());
            } catch (RuntimeException invalidSession) {
                return true;
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // No handshake resources to release.
    }
}
