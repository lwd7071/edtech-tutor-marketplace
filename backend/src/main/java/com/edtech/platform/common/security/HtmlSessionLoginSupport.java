package com.edtech.platform.common.security;

import com.edtech.platform.auth.domain.Role;
import com.edtech.platform.auth.dto.response.AuthResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Component
public class HtmlSessionLoginSupport {
    private static final int IDLE_TIMEOUT_SECONDS = 30 * 60;
    private final Clock clock;
    private final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
    private final HttpSessionSecurityContextRepository securityContexts = new HttpSessionSecurityContextRepository();

    public HtmlSessionLoginSupport(Clock clock) {
        this.clock = clock;
        securityContexts.setDisableUrlRewriting(true);
    }

    public String establish(AuthResult result, HttpServletRequest request, HttpServletResponse response,
                            String preferredDestination) {
        HttpSession session = request.getSession(true);
        request.changeSessionId();
        session.setMaxInactiveInterval(IDLE_TIMEOUT_SECONDS);
        session.setAttribute(HtmlSessionAuthenticationFilter.SESSION_STATE_ATTRIBUTE,
                HtmlSessionState.from(result, clock.instant()));

        var user = result.user();
        var authentication = new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(user.id(), user.email(), user.role().name()), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name())));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContexts.saveContext(context, request, response);

        String destination = roleSafeDestination(preferredDestination, user.role());
        SavedRequest saved = requestCache.getRequest(request, response);
        if (destination == null && saved != null) {
            destination = roleSafeDestination(pathFromSavedRequest(saved, request), user.role());
        }
        if (destination == null) destination = defaultDestination(user.role());
        requestCache.removeRequest(request, response);
        return destination;
    }

    public String roleSafeDestination(String candidate, Role role) {
        String safe = safeLocalPath(candidate);
        if (safe == null) return null;
        String path = safe.split("\\?", 2)[0];
        if (isAllowedPublicTeacherProfile(safe)) return safe;
        String prefix = switch (role) {
            case STUDENT -> "/student";
            case TEACHER -> "/teacher";
            case ADMIN -> "/admin";
        };
        return path.equals(prefix) || path.startsWith(prefix + "/") ? safe : null;
    }

    private boolean isAllowedPublicTeacherProfile(String candidate) {
        try {
            URI uri = URI.create(candidate);
            String path = uri.getRawPath();
            if (path == null || !path.startsWith("/teachers/")) return false;
            UUID.fromString(path.substring("/teachers/".length()));
            String query = uri.getRawQuery();
            if (query == null) return true;
            String[] parameter = query.split("=", -1);
            if (parameter.length != 2 || !"packageId".equals(parameter[0])) return false;
            UUID.fromString(parameter[1]);
            return true;
        } catch (RuntimeException invalid) {
            return false;
        }
    }

    private String pathFromSavedRequest(SavedRequest saved, HttpServletRequest request) {
        try {
            URI uri = URI.create(saved.getRedirectUrl());
            String host = request.getServerName();
            if (uri.getHost() != null && !uri.getHost().equalsIgnoreCase(host)) return null;
            return uri.getRawPath() + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
        } catch (RuntimeException invalid) {
            return null;
        }
    }

    private String safeLocalPath(String value) {
        if (value == null || value.isBlank() || !value.startsWith("/") || value.startsWith("//")
                || value.contains("\\") || value.contains("\r") || value.contains("\n")) return null;
        try {
            URI uri = URI.create(value);
            if (uri.isAbsolute() || uri.getRawAuthority() != null || uri.getRawPath() == null
                    || uri.getRawPath().startsWith("/auth")) return null;
            return uri.getRawPath() + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
        } catch (RuntimeException invalid) {
            return null;
        }
    }

    private String defaultDestination(Role role) {
        return switch (role) {
            case STUDENT -> "/student";
            case TEACHER -> "/teacher";
            case ADMIN -> "/admin";
        };
    }
}
