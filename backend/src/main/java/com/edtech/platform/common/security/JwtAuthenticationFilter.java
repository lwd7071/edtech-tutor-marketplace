package com.edtech.platform.common.security;

import com.edtech.platform.common.exception.ErrorCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

import org.springframework.lang.NonNull;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserStatusCacheService userStatusCache;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, UserStatusCacheService userStatusCache) {
        this.tokenProvider = tokenProvider;
        this.userStatusCache = userStatusCache;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);
            if (StringUtils.hasText(jwt)) {
                AuthenticatedUser user = tokenProvider.getAuthenticatedUserFromToken(jwt);
                String status = userStatusCache.resolve(user.id()).orElse(null);
                if (!"ACTIVE".equals(status)) {
                    request.setAttribute("jwt_error", statusError(status));
                    filterChain.doFilter(request, response);
                    return;
                }
                
                java.util.List<org.springframework.security.core.authority.SimpleGrantedAuthority> authorities = Collections.emptyList();
                if (user.role() != null) {
                    authorities = Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + user.role()));
                }
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        user, null, authorities
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (ExpiredJwtException ignored) {
            request.setAttribute("jwt_error", ErrorCode.AUTH_TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException ignored) {
            request.setAttribute("jwt_error", ErrorCode.AUTH_TOKEN_INVALID);
        }
        
        filterChain.doFilter(request, response);
    }

    private ErrorCode statusError(String status) {
        if ("LOCKED".equals(status)) return ErrorCode.ACCOUNT_LOCKED;
        if ("DISABLED".equals(status)) return ErrorCode.ACCOUNT_DISABLED;
        return ErrorCode.ACCOUNT_NOT_ACTIVE;
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
