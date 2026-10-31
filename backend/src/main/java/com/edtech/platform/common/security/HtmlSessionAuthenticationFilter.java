package com.edtech.platform.common.security;

import com.edtech.platform.auth.dto.response.AuthResult;
import com.edtech.platform.auth.service.SessionService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

@Component
public class HtmlSessionAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(HtmlSessionAuthenticationFilter.class);
    public static final String SESSION_STATE_ATTRIBUTE = HtmlSessionAuthenticationFilter.class.getName() + ".STATE";
    public static final Duration ABSOLUTE_SESSION_LIFETIME = Duration.ofDays(7);
    public static final int IDLE_TIMEOUT_SECONDS = 30 * 60;

    private final SessionService sessions;
    private final JwtTokenProvider tokens;
    private final UserStatusCacheService userStatuses;
    private final HttpSessionSecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final Clock clock;

    public HtmlSessionAuthenticationFilter(
            SessionService sessions,
            JwtTokenProvider tokens,
            UserStatusCacheService userStatuses,
            Clock clock) {
        this.sessions = sessions;
        this.tokens = tokens;
        this.userStatuses = userStatuses;
        securityContextRepository.setDisableUrlRewriting(true);
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/")
                || path.startsWith("/fonts/") || "/favicon.ico".equals(path) || "/favicon.svg".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            synchronized (session) {
                if (!authenticateSession(request, response, session)) return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean authenticateSession(HttpServletRequest request, HttpServletResponse response, HttpSession session)
            throws ServletException, IOException {
        Object value = session.getAttribute(SESSION_STATE_ATTRIBUTE);
        if (!(value instanceof HtmlSessionState state)) return true;

        if (!clock.instant().isBefore(state.createdAt().plus(ABSOLUTE_SESSION_LIFETIME))) {
            expire(session, state);
            return true;
        }

        AuthenticatedUser user;
        try {
            user = tokens.parseAccessToken(state.accessToken()).user();
        } catch (ExpiredJwtException expired) {
            try {
                AuthResult refreshed = sessions.refresh(state.refreshToken(), request.getRemoteAddr());
                state = state.refreshed(refreshed);
                session.setAttribute(SESSION_STATE_ATTRIBUTE, state);
                user = tokens.parseAccessToken(state.accessToken()).user();
            } catch (RuntimeException invalidSession) {
                expire(session, state);
                return true;
            }
        } catch (RuntimeException invalidSession) {
            expire(session, state);
            return true;
        }

        final String status;
        try {
            status = userStatuses.resolve(user.id()).orElse(null);
        } catch (RuntimeException statusLookupUnavailable) {
            log.warn("HTML session status could not be revalidated; denying access until identity storage is available");
            String query = request.getQueryString();
            request.setAttribute("retryUrl", request.getRequestURI() + (query == null ? "" : "?" + query));
            response.setHeader("Cache-Control", "no-store, max-age=0");
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            request.getRequestDispatcher("/service-unavailable").forward(request, response);
            return false;
        }

        if (!"ACTIVE".equals(status)) {
            expire(session, state);
            return true;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.role())));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        session.setMaxInactiveInterval(IDLE_TIMEOUT_SECONDS);
        securityContextRepository.saveContext(context, request, response);
        return true;
    }

    private void expire(HttpSession session, HtmlSessionState state) {
        try {
            sessions.logout(state.refreshToken());
        } catch (RuntimeException ignored) {
            // The HTTP session still has to expire when revocation storage is unavailable.
        }
        SecurityContextHolder.clearContext();
        session.invalidate();
    }
}
