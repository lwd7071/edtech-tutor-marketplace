package com.edtech.platform.catalog.web;

import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.config.SecurityConfig;
import com.edtech.platform.common.config.properties.CorsProperties;
import com.edtech.platform.common.security.JwtAuthenticationFilter;
import com.edtech.platform.common.security.HtmlSessionAuthenticationFilter;
import com.edtech.platform.common.security.RestAccessDeniedHandler;
import com.edtech.platform.common.security.RestAuthenticationEntryPoint;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.auth.security.OAuth2AuthenticationFailureHandler;
import com.edtech.platform.auth.security.OAuth2AuthenticationSuccessHandler;
import com.edtech.platform.auth.service.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ForbiddenPageController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, HtmlSessionAuthenticationFilter.class})
class ForbiddenPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private SessionService sessions;
    @MockBean private Clock clock;
    @MockBean private UserStatusCacheService userStatusCacheService;
    @MockBean private RestAuthenticationEntryPoint authenticationEntryPoint;
    @MockBean private RestAccessDeniedHandler accessDeniedHandler;
    @MockBean private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    @MockBean private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    @MockBean private CorsProperties corsProperties;

    @Test
    void rendersAccessibleForbiddenPageWithSafePublicDestinations() throws Exception {
        mockMvc.perform(get("/forbidden"))
                .andExpect(status().isOk())
                .andExpect(view().name("public/forbidden"))
                .andExpect(content().string(containsString("<h1 id=\"forbidden-title\">Bạn chưa thể xem trang này</h1>")))
                .andExpect(content().string(containsString("href=\"/\">Về trang chủ</a>")))
                .andExpect(content().string(containsString("href=\"/teachers\">Tìm gia sư</a>")));
    }
}
