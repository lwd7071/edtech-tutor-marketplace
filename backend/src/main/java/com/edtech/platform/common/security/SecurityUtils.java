package com.edtech.platform.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityUtils {
    
    private SecurityUtils() {}

    public static AuthenticatedUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        return null;
    }

    public static UUID getCurrentUserId() {
        AuthenticatedUser user = getCurrentUser();
        return user != null ? user.id() : null;
    }

    public static String getCurrentUserRole() {
        AuthenticatedUser user = getCurrentUser();
        return user != null ? user.role() : null;
    }
}
