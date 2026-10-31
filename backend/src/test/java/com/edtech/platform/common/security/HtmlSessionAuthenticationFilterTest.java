package com.edtech.platform.common.security;

import com.edtech.platform.auth.dto.response.AuthResult;
import com.edtech.platform.auth.service.SessionService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.transaction.CannotCreateTransactionException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HtmlSessionAuthenticationFilterTest {
    private final Instant now = Instant.parse("2026-10-28T12:00:00Z");
    private final SessionService sessions = mock(SessionService.class);
    private final JwtTokenProvider tokens = mock(JwtTokenProvider.class);
    private final UserStatusCacheService statuses = mock(UserStatusCacheService.class);
    private final HtmlSessionAuthenticationFilter filter = new HtmlSessionAuthenticationFilter(
            sessions, tokens, statuses, Clock.fixed(now, ZoneOffset.UTC));

    @Test
    void concurrentExpiredRequestsRotateRefreshTokenOnlyOncePerSession() throws Exception {
        UUID userId = UUID.randomUUID();
        var user = new AuthenticatedUser(userId, "student@example.test", "STUDENT");
        AuthResult refreshed = new AuthResult("fresh-access", "rotated-refresh", "Bearer", 900,
                new AuthResult.UserSummary(userId, user.email(), "Student", null, null, null));
        when(tokens.parseAccessToken("expired-access"))
                .thenThrow(new ExpiredJwtException(null, null, "expired"));
        when(tokens.parseAccessToken("fresh-access"))
                .thenReturn(new JwtTokenProvider.VerifiedAccessToken(user, now.plusSeconds(900)));
        when(sessions.refresh("old-refresh", "127.0.0.1")).thenReturn(refreshed);
        when(statuses.resolve(userId)).thenReturn(java.util.Optional.of("ACTIVE"));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HtmlSessionAuthenticationFilter.SESSION_STATE_ATTRIBUTE,
                new HtmlSessionState("expired-access", "old-refresh", now.minusSeconds(60)));
        CountDownLatch completed = new CountDownLatch(2);
        FilterChain chain = (request, response) -> completed.countDown();

        try (var executor = Executors.newFixedThreadPool(2)) {
            for (int i = 0; i < 2; i++) {
                executor.submit(() -> {
                    try {
                        MockHttpServletRequest request = new MockHttpServletRequest();
                        request.setSession(session);
                        request.setRemoteAddr("127.0.0.1");
                        filter.doFilter(request, new MockHttpServletResponse(), chain);
                    } catch (Exception exception) {
                        throw new RuntimeException(exception);
                    }
                });
            }
            if (!completed.await(5, TimeUnit.SECONDS)) throw new AssertionError("Concurrent requests did not finish");
        }

        HtmlSessionState state = (HtmlSessionState) session.getAttribute(HtmlSessionAuthenticationFilter.SESSION_STATE_ATTRIBUTE);
        assertEquals("rotated-refresh", state.refreshToken());
        assertNotNull(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
        assertEquals(HtmlSessionAuthenticationFilter.IDLE_TIMEOUT_SECONDS, session.getMaxInactiveInterval());
        verify(sessions, times(1)).refresh("old-refresh", "127.0.0.1");
    }

    @Test
    void absoluteLifetimeExpiresSessionAndRevokesRefreshToken() throws Exception {
        String refreshToken = "private-refresh";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HtmlSessionAuthenticationFilter.SESSION_STATE_ATTRIBUTE,
                new HtmlSessionState("unused-access", refreshToken,
                        now.minus(HtmlSessionAuthenticationFilter.ABSOLUTE_SESSION_LIFETIME).minusSeconds(1)));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> session.getAttribute(HtmlSessionAuthenticationFilter.SESSION_STATE_ATTRIBUTE));
        verify(sessions).logout(refreshToken);
    }

    @Test
    void unavailableAccountStatusStoreReturnsServiceUnavailableWithoutAuthenticatingOrExpiringSession() throws Exception {
        UUID userId = UUID.randomUUID();
        var user = new AuthenticatedUser(userId, "admin@example.test", "ADMIN");
        when(tokens.parseAccessToken("valid-access"))
                .thenReturn(new JwtTokenProvider.VerifiedAccessToken(user, now.plusSeconds(900)));
        when(statuses.resolve(userId)).thenThrow(new CannotCreateTransactionException("database unavailable"));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HtmlSessionAuthenticationFilter.SESSION_STATE_ATTRIBUTE,
                new HtmlSessionState("valid-access", "refresh-token", now.minusSeconds(60)));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/admin/dashboard");
        request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
                org.junit.jupiter.api.Assertions.fail("Protected route must not run"));

        assertEquals(HttpServletResponse.SC_SERVICE_UNAVAILABLE, response.getStatus());
        assertEquals("/service-unavailable", response.getForwardedUrl());
        assertNotNull(session.getAttribute(HtmlSessionAuthenticationFilter.SESSION_STATE_ATTRIBUTE));
        org.junit.jupiter.api.Assertions.assertNull(
                session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
    }

    @Test
    void publicStaticAssetsDoNotRequireDatabaseBackedSessionRevalidation() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HtmlSessionAuthenticationFilter.SESSION_STATE_ATTRIBUTE,
                new HtmlSessionState("valid-access", "refresh-token", now.minusSeconds(60)));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/css/tokens.css");
        request.setServletPath("/css/tokens.css");
        request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();
        java.util.concurrent.atomic.AtomicBoolean reachedResource = new java.util.concurrent.atomic.AtomicBoolean();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> reachedResource.set(true));

        org.junit.jupiter.api.Assertions.assertTrue(reachedResource.get());
        assertEquals(200, response.getStatus());
        org.mockito.Mockito.verifyNoInteractions(statuses);
    }
}
