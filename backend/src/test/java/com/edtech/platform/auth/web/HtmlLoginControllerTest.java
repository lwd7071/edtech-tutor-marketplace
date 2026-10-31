package com.edtech.platform.auth.web;

import com.edtech.platform.auth.domain.Role;
import com.edtech.platform.auth.domain.UserStatus;
import com.edtech.platform.auth.dto.response.AuthResult;
import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.auth.security.OAuth2AuthenticationFailureHandler;
import com.edtech.platform.auth.security.OAuth2AuthenticationSuccessHandler;
import com.edtech.platform.common.config.SecurityConfig;
import com.edtech.platform.common.config.properties.CorsProperties;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.HtmlSessionAuthenticationFilter;
import com.edtech.platform.common.security.HtmlSessionLoginSupport;
import com.edtech.platform.common.security.HtmlSessionState;
import com.edtech.platform.common.security.JwtAuthenticationFilter;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.RestAccessDeniedHandler;
import com.edtech.platform.common.security.RestAuthenticationEntryPoint;
import com.edtech.platform.common.security.UserStatusCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(HtmlLoginController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, HtmlSessionAuthenticationFilter.class, HtmlSessionLoginSupport.class})
class HtmlLoginControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private SessionService sessions;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private Clock clock;
    @MockBean private UserStatusCacheService userStatusCacheService;
    @MockBean private RestAuthenticationEntryPoint authenticationEntryPoint;
    @MockBean private RestAccessDeniedHandler accessDeniedHandler;
    @MockBean private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    @MockBean private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    @MockBean private CorsProperties corsProperties;

    @Test
    void rendersLoginFormWithCsrfAndNoJavascriptDependency() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(content().string(containsString("action=\"/auth/login\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("autocomplete=\"current-password\"")));
    }

    @Test
    void oauthFailureIsRenderedAsFocusableAlert() throws Exception {
        mockMvc.perform(get("/auth/login").param("oauthError", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"login-error\" class=\"auth-notice auth-notice--error\" role=\"alert\"")))
                .andExpect(content().string(containsString("aria-describedby=\"login-error\" autofocus=\"autofocus\"")))
                .andExpect(content().string(containsString("Không thể tiếp tục bằng Google")));
    }

    @Test
    void successfulLoginStoresTokensOnlyInServerSessionAndUsesRoleSafeTarget() throws Exception {
        UUID id = UUID.randomUUID();
        Instant now = Instant.parse("2026-10-28T12:00:00Z");
        AuthResult authResult = new AuthResult("access-secret", "refresh-secret", "Bearer", 900,
                new AuthResult.UserSummary(id, "learner@example.com", "Nguyễn An", Role.STUDENT, UserStatus.ACTIVE, null));
        when(sessions.login(any(), eq("127.0.0.1"))).thenReturn(authResult);
        when(clock.instant()).thenReturn(now);
        MockHttpSession preAuthSession = new MockHttpSession();
        String oldSessionId = preAuthSession.getId();

        mockMvc.perform(post("/auth/login").session(preAuthSession).with(csrf())
                        .param("email", "learner@example.com")
                        .param("password", "SecretPass1!")
                        .param("continueTo", "/admin/users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student"))
                .andExpect(result -> {
                    Object state = result.getRequest().getSession(false)
                            .getAttribute(HtmlSessionAuthenticationFilter.SESSION_STATE_ATTRIBUTE);
                    org.assertj.core.api.Assertions.assertThat(state).isInstanceOf(HtmlSessionState.class);
                    org.assertj.core.api.Assertions.assertThat(result.getRequest().getSession(false).getId())
                            .isNotEqualTo(oldSessionId);
                    org.assertj.core.api.Assertions.assertThat(result.getResponse().getContentAsString())
                            .doesNotContain("access-secret", "refresh-secret");
                });
    }

    @Test
    void successfulStudentLoginReturnsToWhitelistedTeacherProfileAndSelectedPackage() throws Exception {
        UUID teacherId = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        AuthResult authResult = new AuthResult("access-secret", "refresh-secret", "Bearer", 900,
                new AuthResult.UserSummary(UUID.randomUUID(), "learner@example.com", "Nguyễn An", Role.STUDENT, UserStatus.ACTIVE, null));
        when(sessions.login(any(), eq("127.0.0.1"))).thenReturn(authResult);
        when(clock.instant()).thenReturn(Instant.parse("2026-10-28T12:00:00Z"));

        mockMvc.perform(post("/auth/login").with(csrf())
                        .param("email", "learner@example.com")
                        .param("password", "SecretPass1!")
                        .param("continueTo", "/teachers/" + teacherId + "?packageId=" + packageId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teachers/" + teacherId + "?packageId=" + packageId));
    }

    @Test
    void invalidCredentialsRenderGenericInlineMessage() throws Exception {
        when(sessions.login(any(), any())).thenThrow(new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));

        mockMvc.perform(post("/auth/login").with(csrf())
                        .param("email", "learner@example.com")
                        .param("password", "wrong-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(content().string(containsString("Email hoặc mật khẩu không chính xác")))
                .andExpect(content().string(containsString("value=\"learner@example.com\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("wrong-password"))));
    }

    @Test
    void rejectsExternalContinueTarget() throws Exception {
        mockMvc.perform(get("/auth/login").param("continueTo", "https://example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("https://example.com"))));
    }
}
