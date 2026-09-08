package com.edtech.platform.auth.service;

import com.edtech.platform.auth.domain.User;
import com.edtech.platform.auth.dto.request.ForgotPasswordRequest;
import com.edtech.platform.auth.dto.request.ResetPasswordRequest;
import com.edtech.platform.auth.repository.RefreshTokenRepository;
import com.edtech.platform.auth.repository.UserRepository;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.RateLimiterService;
import com.edtech.platform.mail.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final RedisTokenService oneTimeTokens;
    private final MailService mail;
    private final RateLimiterService rateLimiter;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public void requestReset(ForgotPasswordRequest request, String ipAddress) {
        rateLimiter.checkRateLimit("forgot_password", ipAddress + "_" + request.email(), 3, 900);
        users.findByEmailIgnoreCase(request.email()).ifPresent(user -> {
            if (user.getPasswordHash() != null) {
                String token = oneTimeTokens.issue(RedisTokenService.Purpose.PASSWORD_RESET, user.getId().toString());
                mail.sendPasswordResetEmail(user.getEmail(), token);
            }
        });
    }

    @Transactional
    public void reset(ResetPasswordRequest request) {
        String userId = oneTimeTokens.consume(RedisTokenService.Purpose.PASSWORD_RESET, request.token(), String.class);
        User user = users.findById(UUID.fromString(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_PASSWORD_RESET_TOKEN_INVALID));
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        users.save(user);
        refreshTokens.updateRevokedAtByUserId(user.getId(), clock.instant());
    }
}
