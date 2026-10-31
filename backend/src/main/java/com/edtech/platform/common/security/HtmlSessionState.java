package com.edtech.platform.common.security;

import com.edtech.platform.auth.dto.response.AuthResult;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

public record HtmlSessionState(String accessToken, String refreshToken, Instant createdAt) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public static HtmlSessionState from(AuthResult auth, Instant createdAt) {
        return new HtmlSessionState(auth.accessToken(), auth.refreshToken(), createdAt);
    }

    public HtmlSessionState refreshed(AuthResult auth) {
        return from(auth, createdAt);
    }
}
