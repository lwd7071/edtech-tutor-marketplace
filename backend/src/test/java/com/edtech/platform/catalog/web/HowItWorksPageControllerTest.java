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

@WebMvcTest(HowItWorksPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class HowItWorksPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private UserStatusCacheService userStatusCacheService;
    @MockBean private HtmlSessionAuthenticationFilter htmlSessionAuthenticationFilter;

    @Test
    void rendersTheThreeStepLearningJourneyWithoutJavascript() throws Exception {
        mockMvc.perform(get("/how-it-works"))
                .andExpect(status().isOk())
                .andExpect(view().name("public/how-it-works"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tìm người phù hợp")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Yêu cầu học thử chưa phải buổi học đã xác nhận.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/teachers\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/become-a-tutor\"")));
    }
}
