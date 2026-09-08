package com.edtech.platform.auth.service;

import com.edtech.platform.auth.domain.User;
import com.edtech.platform.auth.domain.UserStatus;
import com.edtech.platform.auth.dto.request.ForgotPasswordRequest;
import com.edtech.platform.auth.dto.request.VerifyEmailRequest;
import com.edtech.platform.auth.repository.UserRepository;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.RateLimiterService;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.mail.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountVerificationService {
    private final UserRepository users;
    private final RedisTokenService oneTimeTokens;
    private final MailService mail;
    private final RateLimiterService rateLimiter;
    private final UserStatusCacheService statusCache;

    @Transactional
    public void verify(VerifyEmailRequest request) {
        String userId = oneTimeTokens.consume(RedisTokenService.Purpose.EMAIL_VERIFY, request.token(), String.class);
        User user = users.findById(UUID.fromString(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_VERIFY_TOKEN_INVALID));
        user.setEmailVerified(true);
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            statusCache.beforeStatusChange(user.getId());
            user.setStatus(UserStatus.ACTIVE);
            statusCache.afterStatusChange(user.getId(), UserStatus.ACTIVE.name());
        }
        users.save(user);
    }

    public void resend(ForgotPasswordRequest request, String ipAddress) {
        rateLimiter.checkRateLimit("resend_verify", ipAddress + "_" + request.email(), 3, 900);
        users.findByEmailIgnoreCase(request.email()).ifPresent(user -> {
            if (!Boolean.TRUE.equals(user.getEmailVerified())) {
                String token = oneTimeTokens.issue(RedisTokenService.Purpose.EMAIL_VERIFY, user.getId().toString());
                mail.sendVerificationEmail(user.getEmail(), token);
            }
        });
    }
}
