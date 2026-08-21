package com.edtech.platform.auth.service;

import com.edtech.platform.auth.domain.RefreshToken;
import com.edtech.platform.auth.domain.Role;
import com.edtech.platform.auth.domain.User;
import com.edtech.platform.auth.domain.UserStatus;
import com.edtech.platform.auth.dto.request.*;
import com.edtech.platform.auth.dto.response.AuthResult;
import com.edtech.platform.auth.repository.RefreshTokenRepository;
import com.edtech.platform.auth.repository.UserRepository;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.RateLimiterService;
import com.edtech.platform.auth.event.UserRegisteredEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final ApplicationEventPublisher eventPublisher;
    private final RedisTokenService redisTokenService;
    private final RateLimiterService rateLimiterService;
    private final EmailService emailService;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       ApplicationEventPublisher eventPublisher,
                       RedisTokenService redisTokenService,
                       RateLimiterService rateLimiterService,
                       EmailService emailService,
                       @Value("${app.jwt.access-expiration-ms:900000}") long accessTokenExpirationMs,
                       @Value("${app.jwt.refresh-expiration-ms:604800000}") long refreshTokenExpirationMs) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.eventPublisher = eventPublisher;
        this.redisTokenService = redisTokenService;
        this.rateLimiterService = rateLimiterService;
        this.emailService = emailService;
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Transactional
    public AuthResult register(RegisterRequest request, String ipAddress) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessException(ErrorCode.AUTH_EMAIL_ALREADY_EXISTS);
        }

        if (request.role() == Role.ADMIN) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Cannot register as ADMIN");
        }

        boolean isTeacher = request.role() == Role.TEACHER;
        if (isTeacher && (StringUtils.hasText(request.parentEmail()) ||
                StringUtils.hasText(request.parentPhone()) ||
                StringUtils.hasText(request.parentFullName()))) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Teacher cannot have parent contact info");
        }

        boolean notifyParent = false;
        if (!isTeacher && (StringUtils.hasText(request.parentEmail()) || StringUtils.hasText(request.parentPhone()))) {
            notifyParent = true;
        }

        User user = User.builder()
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(request.role())
                .status(UserStatus.PENDING_VERIFICATION)
                .emailVerified(false)
                .parentFullName(isTeacher ? null : request.parentFullName())
                .parentPhone(isTeacher ? null : request.parentPhone())
                .parentEmail(isTeacher ? null : request.parentEmail())
                .notifyParent(notifyParent)
                .build();

        user = userRepository.save(user);

        eventPublisher.publishEvent(new UserRegisteredEvent(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getFullName()
        ));

        // Generate email verification token and send email
        String token = redisTokenService.issue(RedisTokenService.Purpose.EMAIL_VERIFY, user.getId().toString());
        emailService.sendVerificationEmail(user.getEmail(), token);

        return createAuthResult(user, null, ipAddress);
    }

    @Transactional
    public AuthResult login(LoginRequest request, String ipAddress) {
        rateLimiterService.checkRateLimit("login", request.email(), 5, 900); // Max 5 login attempts per 15 min per email

        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        checkUserStatus(user);

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        return createAuthResult(user, request.deviceInfo(), ipAddress);
    }

    @Transactional
    public AuthResult refresh(String rawRefreshToken, String ipAddress) {
        String tokenHash = hashToken(rawRefreshToken);
        
        RefreshToken refreshTokenEntity = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID));

        if (refreshTokenEntity.getRevokedAt() != null) {
            throw new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_REVOKED);
        }

        if (refreshTokenEntity.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED);
        }

        User user = refreshTokenEntity.getUser();
        checkUserStatus(user);

        // Rotate
        refreshTokenEntity.revoke();
        refreshTokenRepository.save(refreshTokenEntity);

        return createAuthResult(user, refreshTokenEntity.getDeviceInfo(), ipAddress);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (!StringUtils.hasText(rawRefreshToken)) {
            return;
        }
        String tokenHash = hashToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.revoke();
            refreshTokenRepository.save(token);
        });
    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        String userIdStr = redisTokenService.consume(RedisTokenService.Purpose.EMAIL_VERIFY, request.token(), String.class);
        User user = userRepository.findById(UUID.fromString(userIdStr))
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_VERIFY_TOKEN_INVALID));

        user.setEmailVerified(true);
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            user.setStatus(UserStatus.ACTIVE);
        }
        userRepository.save(user);
    }

    public void resendVerification(ForgotPasswordRequest request, String ipAddress) {
        rateLimiterService.checkRateLimit("resend_verify", ipAddress + "_" + request.email(), 3, 900);
        
        userRepository.findByEmailIgnoreCase(request.email()).ifPresent(user -> {
            if (!Boolean.TRUE.equals(user.getEmailVerified())) {
                String token = redisTokenService.issue(RedisTokenService.Purpose.EMAIL_VERIFY, user.getId().toString());
                emailService.sendVerificationEmail(user.getEmail(), token);
            }
        });
    }

    public void forgotPassword(ForgotPasswordRequest request, String ipAddress) {
        rateLimiterService.checkRateLimit("forgot_password", ipAddress + "_" + request.email(), 3, 900);

        userRepository.findByEmailIgnoreCase(request.email()).ifPresent(user -> {
            if (user.getPasswordHash() != null) {
                String token = redisTokenService.issue(RedisTokenService.Purpose.PASSWORD_RESET, user.getId().toString());
                emailService.sendPasswordResetEmail(user.getEmail(), token);
            }
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String userIdStr = redisTokenService.consume(RedisTokenService.Purpose.PASSWORD_RESET, request.token(), String.class);
        User user = userRepository.findById(UUID.fromString(userIdStr))
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_PASSWORD_RESET_TOKEN_INVALID));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        refreshTokenRepository.updateRevokedAtByUserId(user.getId(), Instant.now());
    }

    @Transactional
    public AuthResult exchangeOAuthToken(OAuthExchangeRequest request, String ipAddress) {
        String userIdStr = redisTokenService.consume(RedisTokenService.Purpose.OAUTH_LOGIN_EXCHANGE, request.exchangeCode(), String.class);
        User user = userRepository.findById(UUID.fromString(userIdStr))
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_VERIFY_TOKEN_INVALID));

        checkUserStatus(user);

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        return createAuthResult(user, null, ipAddress);
    }

    @Transactional
    public AuthResult completeOAuthRegistration(CompleteOAuthRegistrationRequest request, String ipAddress) {
        if (request.role() == Role.ADMIN) {
            throw new BusinessException(ErrorCode.AUTH_OAUTH_ROLE_REQUIRED);
        }

        Map<String, String> payload = redisTokenService.consume(RedisTokenService.Purpose.OAUTH_REGISTRATION, request.registrationToken(), Map.class);
        String email = payload.get("email");
        String oauthProvider = payload.get("oauthProvider");
        String oauthSubject = payload.get("oauthSubject");
        String fullName = payload.get("fullName");

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException(ErrorCode.AUTH_OAUTH_LINK_NOT_ALLOWED);
        }

        User user = User.builder()
                .email(email.toLowerCase())
                .passwordHash(null)
                .fullName(fullName)
                .role(request.role())
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .oauthProvider(oauthProvider)
                .oauthSubject(oauthSubject)
                .build();

        user = userRepository.save(user);

        eventPublisher.publishEvent(new UserRegisteredEvent(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getFullName()
        ));

        return createAuthResult(user, null, ipAddress);
    }

    private void checkUserStatus(User user) {
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
    }

    private AuthResult createAuthResult(User user, String deviceInfo, String ipAddress) {
        AuthenticatedUser authUser = new AuthenticatedUser(user.getId(), user.getEmail(), user.getRole().name());
        String accessToken = jwtTokenProvider.generateAccessToken(authUser);

        String rawRefreshToken = generateSecureToken();
        String refreshTokenHash = hashToken(rawRefreshToken);

        Instant expiresAt = Instant.now().plus(refreshTokenExpirationMs, ChronoUnit.MILLIS);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(refreshTokenHash)
                .expiresAt(expiresAt)
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .build();

        refreshTokenRepository.save(refreshToken);

        AuthResult.UserSummary userSummary = new AuthResult.UserSummary(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getStatus(),
                user.getAvatarUrl()
        );

        return new AuthResult(
                accessToken,
                rawRefreshToken,
                "Bearer",
                accessTokenExpirationMs / 1000,
                userSummary
        );
    }

    private String generateSecureToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }
}
