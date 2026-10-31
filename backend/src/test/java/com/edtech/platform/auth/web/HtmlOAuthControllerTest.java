package com.edtech.platform.auth.web;

import com.edtech.platform.auth.domain.Role;
import com.edtech.platform.auth.domain.UserStatus;
import com.edtech.platform.auth.dto.request.CompleteOAuthRegistrationRequest;
import com.edtech.platform.auth.dto.response.AuthResult;
import com.edtech.platform.auth.security.OAuth2AuthenticationFailureHandler;
import com.edtech.platform.auth.security.OAuth2AuthenticationSuccessHandler;
import com.edtech.platform.auth.service.OAuthAccountService;
import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.common.config.SecurityConfig;
import com.edtech.platform.common.config.properties.CorsProperties;
import com.edtech.platform.common.security.HtmlSessionAuthenticationFilter;
import com.edtech.platform.common.security.HtmlSessionLoginSupport;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(HtmlOAuthController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, HtmlSessionAuthenticationFilter.class, HtmlSessionLoginSupport.class})
class HtmlOAuthControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private OAuthAccountService oauthAccounts;
    @MockBean private SessionService sessions;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private java.time.Clock clock;
    @MockBean private UserStatusCacheService userStatusCacheService;
    @MockBean private RestAuthenticationEntryPoint authenticationEntryPoint;
    @MockBean private RestAccessDeniedHandler accessDeniedHandler;
    @MockBean private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    @MockBean private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    @MockBean private CorsProperties corsProperties;

    @Test
    void startsGoogleFlowWithAnHtmlServerSessionMarker() throws Exception {
        mockMvc.perform(get("/auth/oauth/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/oauth2/authorization/google"))
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getRequest().getSession(false)
                        .getAttribute(HtmlOAuthController.HTML_FLOW_ATTRIBUTE)).isEqualTo(true));
    }

    @Test
    void roleSelectionIsServerRenderedAndRequiresPendingOauthSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HtmlOAuthController.PENDING_REGISTRATION_ATTRIBUTE, "pending-registration");
        mockMvc.perform(get("/auth/oauth/role").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/oauth-role"))
                .andExpect(content().string(containsString("<title>Chọn vai trò | Tutor Match</title>")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("Chọn một vai trò")))
                .andExpect(content().string(containsString("value=\"STUDENT\"")))
                .andExpect(content().string(containsString("value=\"TEACHER\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("pending-registration"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("<script"))));
    }

    @Test
    void guestRolePageExplainsMissingOauthSessionAndFocusesAlert() throws Exception {
        mockMvc.perform(get("/auth/oauth/role"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/oauth-role"))
                .andExpect(content().string(containsString("<title>Phiên đăng ký đã kết thúc | Tutor Match</title>")))
                .andExpect(content().string(containsString("Phiên đăng ký đã kết thúc")))
                .andExpect(content().string(containsString("Phiên đăng ký Google không còn hiệu lực. Quay lại đăng nhập để thử lại.")))
                .andExpect(content().string(containsString("role=\"alert\" tabindex=\"-1\" autofocus>")))
                .andExpect(content().string(containsString("href=\"/auth/login\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("action=\"/auth/oauth/role\""))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("<script"))));
    }

    @Test
    void missingRoleReturnsAccessibleFieldErrorAndDoesNotCompleteRegistration() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HtmlOAuthController.PENDING_REGISTRATION_ATTRIBUTE, "pending-registration");

        mockMvc.perform(post("/auth/oauth/role").session(session).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/oauth-role"))
                .andExpect(content().string(containsString("Hãy chọn vai trò")))
                .andExpect(content().string(containsString("aria-describedby=\"oauth-role-error\"")))
                .andExpect(content().string(containsString("id=\"oauth-role-error\"")))
                .andExpect(content().string(containsString("autofocus=\"autofocus\"")));

        verify(oauthAccounts, never()).completeRegistration(any(), any());
    }

    @Test
    void choosingRoleCompletesRegistrationIntoHtmlSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HtmlOAuthController.PENDING_REGISTRATION_ATTRIBUTE, "registration-token");
        AuthResult auth = new AuthResult("access-token", "refresh-token", "Bearer", 900,
                new AuthResult.UserSummary(UUID.randomUUID(), "new@example.test", "Nguyễn An", Role.STUDENT, UserStatus.ACTIVE, null));
        when(oauthAccounts.completeRegistration(any(CompleteOAuthRegistrationRequest.class), eq("127.0.0.1")))
                .thenReturn(auth);
        when(clock.instant()).thenReturn(java.time.Instant.parse("2026-10-28T12:00:00Z"));

        mockMvc.perform(post("/auth/oauth/role").session(session).with(csrf()).param("role", "STUDENT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student"));

        verify(oauthAccounts).completeRegistration(new CompleteOAuthRegistrationRequest("registration-token", Role.STUDENT), "127.0.0.1");
        org.assertj.core.api.Assertions.assertThat(session.getAttribute(HtmlOAuthController.PENDING_REGISTRATION_ATTRIBUTE)).isNull();
        org.assertj.core.api.Assertions.assertThat(session.getAttribute(HtmlSessionAuthenticationFilter.SESSION_STATE_ATTRIBUTE))
                .isNotNull();
    }
}
