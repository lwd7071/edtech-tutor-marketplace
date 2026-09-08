package com.edtech.platform.auth.service;

import com.edtech.platform.auth.domain.RefreshToken;
import com.edtech.platform.auth.domain.User;
import com.edtech.platform.auth.dto.request.LoginRequest;
import com.edtech.platform.auth.dto.response.AuthResult;
import com.edtech.platform.auth.repository.RefreshTokenRepository;
import com.edtech.platform.auth.repository.UserRepository;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.RateLimiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class SessionService {
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final RateLimiterService rateLimiter;
    private final AccountAccessPolicy accessPolicy;
    private final SessionIssuer sessionIssuer;
    private final Clock clock;

    @Transactional
    public AuthResult login(LoginRequest request, String ipAddress) {
        rateLimiter.checkRateLimit("login", request.email(), 5, 900);
        User user = users.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
        accessPolicy.requireActiveAccess(user);
        user.setLastLoginAt(clock.instant());
        users.save(user);
        return sessionIssuer.issue(user, request.deviceInfo(), ipAddress);
    }

    @Transactional
    public AuthResult refresh(String rawRefreshToken, String ipAddress) {
        RefreshToken token = refreshTokens.findByTokenHash(sessionIssuer.hash(rawRefreshToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID));
        if (token.getRevokedAt() != null) {
            throw new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_REVOKED);
        }
        if (token.getExpiresAt().isBefore(clock.instant())) {
            throw new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED);
        }
        accessPolicy.requireActiveAccess(token.getUser());
        token.revoke(clock.instant());
        refreshTokens.save(token);
        return sessionIssuer.issue(token.getUser(), token.getDeviceInfo(), ipAddress);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (!StringUtils.hasText(rawRefreshToken)) {
            return;
        }
        refreshTokens.findByTokenHash(sessionIssuer.hash(rawRefreshToken)).ifPresent(token -> {
            token.revoke(clock.instant());
            refreshTokens.save(token);
        });
    }
}
