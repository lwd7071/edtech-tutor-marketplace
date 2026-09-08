package com.edtech.platform.auth.service;

import com.edtech.platform.auth.domain.RefreshToken;
import com.edtech.platform.auth.domain.User;
import com.edtech.platform.auth.dto.response.AuthResult;
import com.edtech.platform.auth.repository.RefreshTokenRepository;
import com.edtech.platform.common.config.properties.JwtProperties;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;

@Component
@RequiredArgsConstructor
class SessionIssuer {
    private final RefreshTokenRepository refreshTokens;
    private final JwtTokenProvider jwtTokens;
    private final JwtProperties properties;
    private final Clock clock;
    private final SecureRandom secureRandom;

    AuthResult issue(User user, String deviceInfo, String ipAddress) {
        var authenticatedUser = new AuthenticatedUser(user.getId(), user.getEmail(), user.getRole().name());
        String accessToken = jwtTokens.generateAccessToken(authenticatedUser);
        String rawRefreshToken = generateSecureToken();

        refreshTokens.save(RefreshToken.builder()
                .user(user)
                .tokenHash(hash(rawRefreshToken))
                .expiresAt(clock.instant().plus(Duration.ofMillis(properties.refreshExpirationMs())))
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .build());

        var summary = new AuthResult.UserSummary(
                user.getId(), user.getEmail(), user.getFullName(), user.getRole(), user.getStatus(), user.getAvatarUrl());
        return new AuthResult(accessToken, rawRefreshToken, "Bearer",
                properties.accessExpirationMs() / 1000, summary);
    }

    String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
