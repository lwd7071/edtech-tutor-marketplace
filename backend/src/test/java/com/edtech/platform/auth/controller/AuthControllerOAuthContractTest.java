package com.edtech.platform.auth.controller;

import com.edtech.platform.auth.domain.Role;
import com.edtech.platform.auth.domain.UserStatus;
import com.edtech.platform.auth.dto.request.CompleteOAuthRegistrationRequest;
import com.edtech.platform.auth.dto.response.AuthResult;
import com.edtech.platform.auth.service.AccountVerificationService;
import com.edtech.platform.auth.service.OAuthAccountService;
import com.edtech.platform.auth.service.PasswordRecoveryService;
import com.edtech.platform.auth.service.RegistrationService;
import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerOAuthContractTest {

    private final RegistrationService registration = mock(RegistrationService.class);
    private final SessionService sessions = mock(SessionService.class);
    private final AccountVerificationService verification = mock(AccountVerificationService.class);
    private final PasswordRecoveryService passwordRecovery = mock(PasswordRecoveryService.class);
    private final OAuthAccountService oauthAccounts = mock(OAuthAccountService.class);

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
                    new AuthController(registration, sessions, verification, passwordRecovery, oauthAccounts))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    private final ObjectMapper json = new ObjectMapper();

    @Test
    @DisplayName("Cycle 4: POST /api/auth/oauth2/complete-registration returns 201 with AuthResult")
    void completeOAuthRegistration_success_returns201() throws Exception {
        CompleteOAuthRegistrationRequest request = new CompleteOAuthRegistrationRequest("valid-token", Role.STUDENT);
        UUID userId = UUID.randomUUID();
        AuthResult result = new AuthResult(
                "jwt-access-token", "raw-refresh-token", "Bearer", 900L,
                new AuthResult.UserSummary(userId, "student@example.com", "Nguyen Van A", Role.STUDENT, UserStatus.ACTIVE, null)
        );

        when(oauthAccounts.completeRegistration(any(), anyString())).thenReturn(result);

        mvc.perform(post("/api/auth/oauth2/complete-registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("jwt-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("raw-refresh-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.id").value(userId.toString()))
                .andExpect(jsonPath("$.data.user.email").value("student@example.com"))
                .andExpect(jsonPath("$.data.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.data.user.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Cycle 4: POST /api/auth/oauth2/complete-registration with expired token returns 400 and error envelope")
    void completeOAuthRegistration_invalidToken_returns400() throws Exception {
        CompleteOAuthRegistrationRequest request = new CompleteOAuthRegistrationRequest("expired-token", Role.STUDENT);

        when(oauthAccounts.completeRegistration(any(), anyString()))
                .thenThrow(new BusinessException(ErrorCode.AUTH_OAUTH_REGISTRATION_TOKEN_INVALID));

        mvc.perform(post("/api/auth/oauth2/complete-registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value("AUTH_OAUTH_REGISTRATION_TOKEN_INVALID"));
    }
}
