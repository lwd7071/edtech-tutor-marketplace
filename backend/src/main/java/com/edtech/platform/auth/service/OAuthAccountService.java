package com.edtech.platform.auth.service;

import com.edtech.platform.auth.domain.Role;
import com.edtech.platform.auth.domain.User;
import com.edtech.platform.auth.domain.UserStatus;
import com.edtech.platform.auth.dto.request.CompleteOAuthRegistrationRequest;
import com.edtech.platform.auth.dto.request.OAuthExchangeRequest;
import com.edtech.platform.auth.dto.response.AuthResult;
import com.edtech.platform.auth.event.UserRegisteredEvent;
import com.edtech.platform.auth.repository.UserRepository;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuthAccountService implements OAuthAuthorizationPort {
    private final UserRepository users;
    private final RedisTokenService oneTimeTokens;
    private final ApplicationEventPublisher events;
    private final AccountAccessPolicy accessPolicy;
    private final SessionIssuer sessions;
    private final Clock clock;

    @Transactional(readOnly = true)
    @Override
    public OAuthAuthorizationResult authorize(OAuthIdentity identity) {
        if (identity.email() == null || identity.email().isBlank()) {
            return new OAuthAuthorizationResult.Rejected(ErrorCode.AUTH_OAUTH_LINK_NOT_ALLOWED.name());
        }
        return users.findByEmailIgnoreCase(identity.email())
                .<OAuthAuthorizationResult>map(user -> authorizeExisting(user, identity))
                .orElseGet(() -> authorizeRegistration(identity));
    }

    @Transactional
    public AuthResult exchange(OAuthExchangeRequest request, String ipAddress) {
        String userId = oneTimeTokens.consume(RedisTokenService.Purpose.OAUTH_LOGIN_EXCHANGE,
                request.exchangeCode(), String.class);
        User user = users.findById(UUID.fromString(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_OAUTH_EXCHANGE_TOKEN_INVALID));
        accessPolicy.requireActiveAccess(user);
        user.setLastLoginAt(clock.instant());
        users.save(user);
        return sessions.issue(user, null, ipAddress);
    }

    @Transactional
    public AuthResult completeRegistration(CompleteOAuthRegistrationRequest request, String ipAddress) {
        if (request.role() == Role.ADMIN) {
            throw new BusinessException(ErrorCode.AUTH_OAUTH_ROLE_REQUIRED);
        }
        OAuthIdentity identity = oneTimeTokens.consume(RedisTokenService.Purpose.OAUTH_REGISTRATION,
                request.registrationToken(), OAuthIdentity.class);
        String email = identity.email();
        if (email == null || users.existsByEmailIgnoreCase(email)) {
            throw new BusinessException(ErrorCode.AUTH_OAUTH_LINK_NOT_ALLOWED);
        }
        User user = users.save(User.builder()
                .email(email.toLowerCase())
                .passwordHash(null)
                .fullName(identity.fullName())
                .role(request.role())
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .oauthProvider(identity.provider())
                .oauthSubject(identity.subject())
                .build());
        events.publishEvent(new UserRegisteredEvent(user.getId(), user.getEmail(), user.getRole(), user.getFullName()));
        return sessions.issue(user, null, ipAddress);
    }

    private OAuthAuthorizationResult authorizeExisting(User user, OAuthIdentity identity) {
        if (identity.provider().equals(user.getOauthProvider()) && identity.subject().equals(user.getOauthSubject())) {
            String code = oneTimeTokens.issue(RedisTokenService.Purpose.OAUTH_LOGIN_EXCHANGE, user.getId().toString());
            return new OAuthAuthorizationResult.LoginExchange(code);
        }
        return new OAuthAuthorizationResult.Rejected(ErrorCode.AUTH_OAUTH_LINK_NOT_ALLOWED.name());
    }

    private OAuthAuthorizationResult authorizeRegistration(OAuthIdentity identity) {
        OAuthIdentity safeIdentity = new OAuthIdentity(
                identity.provider(),
                identity.subject(),
                identity.email(),
                identity.fullName() == null ? "" : identity.fullName());
        String token = oneTimeTokens.issue(RedisTokenService.Purpose.OAUTH_REGISTRATION, safeIdentity);
        return new OAuthAuthorizationResult.RegistrationRequired(token);
    }
}
