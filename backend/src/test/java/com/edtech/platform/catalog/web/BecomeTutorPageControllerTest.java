package com.edtech.platform.catalog.web;

import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.common.security.HtmlSessionAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(BecomeTutorPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class BecomeTutorPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private UserStatusCacheService userStatusCacheService;
    @MockBean private HtmlSessionAuthenticationFilter htmlSessionAuthenticationFilter;

    @Test
    void rendersTutorOnboardingStepsAndRegistrationRoleWithoutJavascript() throws Exception {
        mockMvc.perform(get("/become-a-tutor"))
                .andExpect(status().isOk())
                .andExpect(view().name("public/become-a-tutor"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chuẩn bị hồ sơ")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sau khi được duyệt")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/auth/register?role=TEACHER\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/teacher/profile\"")));
    }
}
