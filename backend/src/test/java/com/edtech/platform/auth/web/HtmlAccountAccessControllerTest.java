package com.edtech.platform.auth.web;

import com.edtech.platform.auth.dto.request.ForgotPasswordRequest;
import com.edtech.platform.auth.dto.request.ResetPasswordRequest;
import com.edtech.platform.auth.dto.request.VerifyEmailRequest;
import com.edtech.platform.auth.security.OAuth2AuthenticationFailureHandler;
import com.edtech.platform.auth.security.OAuth2AuthenticationSuccessHandler;
import com.edtech.platform.auth.service.AccountVerificationService;
import com.edtech.platform.auth.service.PasswordRecoveryService;
import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.common.config.SecurityConfig;
import com.edtech.platform.common.config.properties.CorsProperties;
import com.edtech.platform.common.security.HtmlSessionAuthenticationFilter;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(HtmlAccountAccessController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, HtmlSessionAuthenticationFilter.class})
class HtmlAccountAccessControllerTest {
    private static final String EMAIL_TOKEN_KEY = HtmlAccountAccessController.class.getName() + ".EMAIL_VERIFY_TOKEN";
    private static final String RESET_TOKEN_KEY = HtmlAccountAccessController.class.getName() + ".PASSWORD_RESET_TOKEN";

    @Autowired private MockMvc mockMvc;
    @MockBean private AccountVerificationService verification;
    @MockBean private PasswordRecoveryService recovery;
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
    void verificationLinkIsStagedInSessionAndRedirectedWithoutToken() throws Exception {
        mockMvc.perform(get("/auth/verify-email").param("token", "one-time-email-token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/verify-email"))
                .andExpect(result -> {
                    org.assertj.core.api.Assertions.assertThat(result.getResponse().getHeader("Location"))
                            .doesNotContain("one-time-email-token");
                    org.assertj.core.api.Assertions.assertThat(result.getRequest().getSession(false).getAttribute(EMAIL_TOKEN_KEY))
                            .isEqualTo("one-time-email-token");
                });
    }

    @Test
    void oversizedVerificationTokenIsRemovedFromUrl() throws Exception {
        mockMvc.perform(get("/auth/verify-email").param("token", "x".repeat(513)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/verify-email"))
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getFlashMap().get("verificationError"))
                        .isEqualTo("Liên kết xác minh không hợp lệ hoặc đã hết hạn."));
    }

    @Test
    void verificationRequiresCsrfAndConsumesStagedTokenOnPost() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(EMAIL_TOKEN_KEY, "one-time-email-token");

        mockMvc.perform(post("/auth/verify-email").session(session).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/verify-email"));

        verify(verification).verify(new VerifyEmailRequest("one-time-email-token"));
        org.assertj.core.api.Assertions.assertThat(session.getAttribute(EMAIL_TOKEN_KEY)).isNull();
    }

    @Test
    void passwordResetLinkIsStagedAndNeverRenderedBack() throws Exception {
        mockMvc.perform(get("/auth/reset-password").param("token", "one-time-reset-token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/reset-password"))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("one-time-reset-token"))))
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getRequest().getSession(false).getAttribute(RESET_TOKEN_KEY))
                        .isEqualTo("one-time-reset-token"));
    }

    @Test
    void oversizedResetTokenIsRemovedFromUrl() throws Exception {
        mockMvc.perform(get("/auth/reset-password").param("token", "x".repeat(513)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/reset-password"))
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getFlashMap().get("resetError"))
                        .isEqualTo("Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn."));
    }

    @Test
    void resetPageWithoutTokenDoesNotRenderPasswordFormOrScripts() throws Exception {
        mockMvc.perform(get("/auth/reset-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/reset-password"))
                .andExpect(content().string(containsString("href=\"/auth/forgot-password\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("id=\"new-password\""))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("<script"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("KHÔI PHỤC TÀI KHOẢN"))));
    }

    @Test
    void resetValidationRendersFieldErrorAndMovesNativeFocus() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(RESET_TOKEN_KEY, "one-time-reset-token");

        mockMvc.perform(post("/auth/reset-password").session(session).with(csrf()).param("newPassword", "weak"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/reset-password"))
                .andExpect(content().string(containsString("Mật khẩu phải dài ít nhất 8 ký tự")))
                .andExpect(content().string(matchesPattern("(?s).*<input[^>]*id=\"new-password\"[^>]*autofocus=\"autofocus\".*")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("value=\"weak\""))));

        verify(recovery, never()).reset(any());
    }

    @Test
    void resetWithoutStagedTokenShowsFocusableError() throws Exception {
        mockMvc.perform(post("/auth/reset-password").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hãy mở liên kết đặt lại mật khẩu trong email trước khi tiếp tục.")))
                .andExpect(content().string(containsString("role=\"alert\" tabindex=\"-1\" autofocus>")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("id=\"new-password\""))));

        verify(recovery, never()).reset(any());
    }

    @Test
    void successfulPasswordResetRevokesSessionsAndRedirectsToLogin() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(RESET_TOKEN_KEY, "one-time-reset-token");

        mockMvc.perform(post("/auth/reset-password").session(session).with(csrf())
                        .param("newPassword", "StrongPass1!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));

        verify(recovery).reset(new ResetPasswordRequest("one-time-reset-token", "StrongPass1!"));
        org.assertj.core.api.Assertions.assertThat(session.getAttribute(RESET_TOKEN_KEY)).isNull();
    }

    @Test
    void forgotPasswordResponseDoesNotRevealWhetherEmailExists() throws Exception {
        mockMvc.perform(post("/auth/forgot-password").with(csrf()).param("email", "learner@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/forgot-password?sent"));

        verify(recovery).requestReset(new ForgotPasswordRequest("learner@example.com"), "127.0.0.1");
    }

    @Test
    void invalidForgotEmailRendersFieldErrorAndMovesNativeFocus() throws Exception {
        mockMvc.perform(post("/auth/forgot-password").with(csrf()).param("email", "not-an-email"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/forgot-password"))
                .andExpect(content().string(containsString("Email không đúng định dạng")))
                .andExpect(content().string(matchesPattern("(?s).*<input[^>]*id=\"forgot-email\"[^>]*autofocus=\"autofocus\".*")));

        verify(recovery, never()).requestReset(any(), any());
    }

    @Test
    void invalidVerificationEmailRendersFieldErrorAndMovesNativeFocus() throws Exception {
        mockMvc.perform(post("/auth/verify-email/resend").with(csrf()).param("email", "not-an-email"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/verify-email"))
                .andExpect(content().string(containsString("Email không đúng định dạng")))
                .andExpect(content().string(matchesPattern("(?s).*<input[^>]*id=\"verify-email\"[^>]*autofocus=\"autofocus\".*")));

        verify(verification, never()).resend(any(), any());
    }

    @Test
    void verificationErrorIsFocusableAfterPostRedirectGet() throws Exception {
        mockMvc.perform(get("/auth/verify-email")
                        .flashAttr("verificationError", "Liên kết xác minh không hợp lệ hoặc đã hết hạn."))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("role=\"alert\" tabindex=\"-1\" autofocus>")))
                .andExpect(content().string(containsString("Liên kết xác minh không hợp lệ hoặc đã hết hạn.")));
    }

    @Test
    void forgotAndVerifyPagesRenderWithoutJavascript() throws Exception {
        mockMvc.perform(get("/auth/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/forgot-password"))
                .andExpect(content().string(containsString("action=\"/auth/forgot-password\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("<script"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("KHÔI PHỤC TÀI KHOẢN"))));
        mockMvc.perform(get("/auth/verify-email"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/verify-email"))
                .andExpect(content().string(containsString("action=\"/auth/verify-email/resend\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("<script"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("BẢO MẬT TÀI KHOẢN"))));
    }

    @Test
    void verifiedEmailCompleteRendersPrimaryLoginCtaButton() throws Exception {
        mockMvc.perform(get("/auth/verify-email").flashAttr("verificationComplete", true))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"auth-submit\" href=\"/auth/login\">Đăng nhập ngay")));
    }
}
