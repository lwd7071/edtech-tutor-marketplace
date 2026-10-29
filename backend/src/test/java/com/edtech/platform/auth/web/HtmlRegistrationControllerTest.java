package com.edtech.platform.auth.web;

import com.edtech.platform.auth.domain.Role;
import com.edtech.platform.auth.dto.response.RegistrationResult;
import com.edtech.platform.auth.security.OAuth2AuthenticationFailureHandler;
import com.edtech.platform.auth.security.OAuth2AuthenticationSuccessHandler;
import com.edtech.platform.auth.service.RegistrationService;
import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.common.config.SecurityConfig;
import com.edtech.platform.common.config.properties.CorsProperties;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
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
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(HtmlRegistrationController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, HtmlSessionAuthenticationFilter.class})
class HtmlRegistrationControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private RegistrationService registrations;
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
    void rendersNoScriptRegistrationFormWithRequiredRoleOptionsAndCsrf() throws Exception {
        mockMvc.perform(get("/auth/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("value=\"STUDENT\"")))
                .andExpect(content().string(containsString("value=\"TEACHER\"")))
                .andExpect(content().string(matchesPattern("(?s).*<input type=\"radio\" value=\"STUDENT\"[^>]*checked=\"checked\".*")))
                .andExpect(content().string(containsString("Thông tin phụ huynh")))
                .andExpect(content().string(containsString("chỉ dành cho học viên")))
                .andExpect(content().string(containsString("href=\"/privacy\"")))
                .andExpect(content().string(containsString("Sau khi gửi, bạn cần xác minh email")));
    }

    @Test
    void teacherRoleQueryPreselectsTeacherAndRejectsUnsupportedRoles() throws Exception {
        mockMvc.perform(get("/auth/register").param("role", "TEACHER"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*<input type=\"radio\" value=\"TEACHER\"[^>]*checked=\"checked\".*")))
                .andExpect(content().string(org.hamcrest.Matchers.not(matchesPattern("(?s).*<input type=\"radio\" value=\"STUDENT\"[^>]*checked=\"checked\".*"))));

        mockMvc.perform(get("/auth/register").param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*<input type=\"radio\" value=\"STUDENT\"[^>]*checked=\"checked\".*")))
                .andExpect(content().string(org.hamcrest.Matchers.not(matchesPattern("(?s).*<input type=\"radio\" value=\"TEACHER\"[^>]*checked=\"checked\".*"))));
    }

    @Test
    void registrationUsesPostRedirectGetAndDoesNotReturnTokens() throws Exception {
        when(registrations.register(any(), eq("127.0.0.1")))
                .thenReturn(new RegistrationResult("learner@example.com", true));

        mockMvc.perform(post("/auth/register").with(csrf())
                        .param("email", "learner@example.com")
                        .param("password", "SecretPass1!")
                        .param("fullName", "Nguyễn Minh An")
                        .param("role", "STUDENT")
                        .param("parentFullName", "")
                        .param("parentPhone", "")
                        .param("parentEmail", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/verify-email"))
                .andExpect(flash().attribute("registered", true));
    }

    @Test
    void invalidPasswordRendersFieldErrorWithoutEchoingPassword() throws Exception {
        mockMvc.perform(post("/auth/register").with(csrf())
                        .param("email", "learner@example.com")
                        .param("password", "weak")
                        .param("fullName", "Nguyễn Minh An")
                        .param("role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(content().string(containsString("Mật khẩu phải dài ít nhất 8 ký tự")))
                .andExpect(content().string(containsString("id=\"register-error-summary\"")))
                .andExpect(content().string(containsString("Có thông tin chưa hợp lệ")))
                .andExpect(content().string(matchesPattern("(?s).*<input[^>]*id=\"register-password\"[^>]*autofocus=\"autofocus\".*")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("id=\"register-error-summary\" class=\"auth-notice auth-notice--error\" role=\"alert\" tabindex=\"-1\" autofocus"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("value=\"weak\""))));
    }

    @Test
    void duplicateEmailShowsDomainError() throws Exception {
        when(registrations.register(any(), any())).thenThrow(new BusinessException(ErrorCode.AUTH_EMAIL_ALREADY_EXISTS));

        mockMvc.perform(post("/auth/register").with(csrf())
                        .param("email", "learner@example.com")
                        .param("password", "StrongPass1!")
                        .param("fullName", "Nguyễn Minh An")
                        .param("role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Email này đã được đăng ký")))
                .andExpect(content().string(matchesPattern("(?s).*<input[^>]*id=\"register-email\"[^>]*autofocus=\"autofocus\".*")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("StrongPass1!"))));
    }
}
