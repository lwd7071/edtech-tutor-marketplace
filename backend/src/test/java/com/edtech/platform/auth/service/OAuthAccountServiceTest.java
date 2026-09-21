package com.edtech.platform.auth.service;

import com.edtech.platform.auth.domain.Role;
import com.edtech.platform.auth.domain.User;
import com.edtech.platform.auth.dto.request.CompleteOAuthRegistrationRequest;
import com.edtech.platform.auth.dto.response.AuthResult;
import com.edtech.platform.auth.event.UserRegisteredEvent;
import com.edtech.platform.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthAccountServiceTest {

    @Mock
    private UserRepository users;

    @Mock
    private RedisTokenService oneTimeTokens;

    @Mock
    private ApplicationEventPublisher events;

    @Mock
    private AccountAccessPolicy accessPolicy;

    @Mock
    private SessionIssuer sessions;

    private Clock clock;
    private OAuthAccountService service;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-09-14T10:00:00Z"), ZoneOffset.UTC);
        service = new OAuthAccountService(users, oneTimeTokens, events, accessPolicy, sessions, clock);
    }

    @Test
    @DisplayName("Cycle 1 (Tracer Bullet): completeRegistration consumes OAuthIdentity, saves user, publishes event and returns session")
    void completeRegistration_success_withOAuthIdentity() {
        String token = "valid-reg-token";
        OAuthIdentity identity = new OAuthIdentity("GOOGLE", "google-123", "student@example.com", "Nguyen Van A");
        CompleteOAuthRegistrationRequest request = new CompleteOAuthRegistrationRequest(token, Role.STUDENT);

        // Expect consuming OAuthIdentity.class from Redis
        when(oneTimeTokens.consume(eq(RedisTokenService.Purpose.OAUTH_REGISTRATION), eq(token), eq(OAuthIdentity.class)))
                .thenReturn(identity);
        when(users.existsByEmailIgnoreCase("student@example.com")).thenReturn(false);

        User savedUser = User.builder()
                .email("student@example.com")
                .fullName("Nguyen Van A")
                .role(Role.STUDENT)
                .oauthProvider("GOOGLE")
                .oauthSubject("google-123")
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(savedUser, "id", UUID.randomUUID());

        when(users.save(any(User.class))).thenReturn(savedUser);

        AuthResult expectedAuthResult = new AuthResult(
                "access-token", "refresh-token", "Bearer", 900L,
                new AuthResult.UserSummary(savedUser.getId(), savedUser.getEmail(), savedUser.getFullName(), savedUser.getRole(), savedUser.getStatus(), null)
        );
        when(sessions.issue(savedUser, null, "127.0.0.1")).thenReturn(expectedAuthResult);

        AuthResult actualResult = service.completeRegistration(request, "127.0.0.1");

        assertThat(actualResult).isEqualTo(expectedAuthResult);

        // Verify User was saved with expected attributes
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(users).save(userCaptor.capture());
        User userToSave = userCaptor.getValue();
        assertThat(userToSave.getEmail()).isEqualTo("student@example.com");
        assertThat(userToSave.getFullName()).isEqualTo("Nguyen Van A");
        assertThat(userToSave.getRole()).isEqualTo(Role.STUDENT);
        assertThat(userToSave.getOauthProvider()).isEqualTo("GOOGLE");
        assertThat(userToSave.getOauthSubject()).isEqualTo("google-123");
        assertThat(userToSave.getEmailVerified()).isTrue();
        assertThat(userToSave.getNotifyParent()).isFalse();

        // Verify domain event was published
        ArgumentCaptor<UserRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(events).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEmail()).isEqualTo("student@example.com");
        assertThat(eventCaptor.getValue().getRoleName()).isEqualTo("STUDENT");
    }

    @Test
    @DisplayName("Cycle 2: authorize for new email issues RegistrationRequired with OAuthIdentity payload")
    void authorize_newAccount_issuesRegistrationTokenWithOAuthIdentity() {
        OAuthIdentity identity = new OAuthIdentity("GOOGLE", "sub-456", "new@example.com", "New Student");
        when(users.findByEmailIgnoreCase("new@example.com")).thenReturn(java.util.Optional.empty());
        when(oneTimeTokens.issue(eq(RedisTokenService.Purpose.OAUTH_REGISTRATION), any(OAuthIdentity.class)))
                .thenReturn("new-reg-token");

        OAuthAuthorizationResult result = service.authorize(identity);

        assertThat(result).isInstanceOf(OAuthAuthorizationResult.RegistrationRequired.class);
        var reg = (OAuthAuthorizationResult.RegistrationRequired) result;
        assertThat(reg.registrationToken()).isEqualTo("new-reg-token");

        ArgumentCaptor<OAuthIdentity> captor = ArgumentCaptor.forClass(OAuthIdentity.class);
        verify(oneTimeTokens).issue(eq(RedisTokenService.Purpose.OAUTH_REGISTRATION), captor.capture());
        OAuthIdentity savedPayload = captor.getValue();
        assertThat(savedPayload.email()).isEqualTo("new@example.com");
        assertThat(savedPayload.provider()).isEqualTo("GOOGLE");
        assertThat(savedPayload.subject()).isEqualTo("sub-456");
        assertThat(savedPayload.fullName()).isEqualTo("New Student");
    }

    @Test
    @DisplayName("Cycle 2: completeRegistration rejects ADMIN role")
    void completeRegistration_rejectsAdminRole() {
        CompleteOAuthRegistrationRequest request = new CompleteOAuthRegistrationRequest("token", Role.ADMIN);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.completeRegistration(request, "127.0.0.1"))
                .isInstanceOf(com.edtech.platform.common.exception.BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", com.edtech.platform.common.exception.ErrorCode.AUTH_OAUTH_ROLE_REQUIRED);
    }

    @Test
    @DisplayName("Cycle 2: completeRegistration rejects already existing email")
    void completeRegistration_rejectsExistingEmail() {
        String token = "reg-token";
        OAuthIdentity identity = new OAuthIdentity("GOOGLE", "sub-123", "existing@example.com", "Existing");
        CompleteOAuthRegistrationRequest request = new CompleteOAuthRegistrationRequest(token, Role.STUDENT);

        when(oneTimeTokens.consume(eq(RedisTokenService.Purpose.OAUTH_REGISTRATION), eq(token), eq(OAuthIdentity.class)))
                .thenReturn(identity);
        when(users.existsByEmailIgnoreCase("existing@example.com")).thenReturn(true);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.completeRegistration(request, "127.0.0.1"))
                .isInstanceOf(com.edtech.platform.common.exception.BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", com.edtech.platform.common.exception.ErrorCode.AUTH_OAUTH_LINK_NOT_ALLOWED);
    }

    @Test
    @DisplayName("Cycle 2: completeRegistration throws when token is missing/expired")
    void completeRegistration_throwsWhenTokenInvalid() {
        String token = "expired-token";
        CompleteOAuthRegistrationRequest request = new CompleteOAuthRegistrationRequest(token, Role.STUDENT);

        when(oneTimeTokens.consume(eq(RedisTokenService.Purpose.OAUTH_REGISTRATION), eq(token), eq(OAuthIdentity.class)))
                .thenThrow(new com.edtech.platform.common.exception.BusinessException(com.edtech.platform.common.exception.ErrorCode.AUTH_OAUTH_REGISTRATION_TOKEN_INVALID));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.completeRegistration(request, "127.0.0.1"))
                .isInstanceOf(com.edtech.platform.common.exception.BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", com.edtech.platform.common.exception.ErrorCode.AUTH_OAUTH_REGISTRATION_TOKEN_INVALID);
    }
}
