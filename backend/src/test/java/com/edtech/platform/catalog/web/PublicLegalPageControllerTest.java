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

@WebMvcTest(PublicLegalPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicLegalPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private UserStatusCacheService userStatusCacheService;
    @MockBean private HtmlSessionAuthenticationFilter htmlSessionAuthenticationFilter;

    @Test
    void termsRenderTheCurrentSourceCopyAndPrivacyLink() throws Exception {
        mockMvc.perform(get("/terms"))
                .andExpect(status().isOk())
                .andExpect(view().name("public/terms"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Điều khoản này là bản tối thiểu trong giai đoạn MVP")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/privacy\"")));
    }

    @Test
    void privacyRendersTheThreeSourceSectionsAndTermsLink() throws Exception {
        mockMvc.perform(get("/privacy"))
                .andExpect(status().isOk())
                .andExpect(view().name("public/privacy"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Thông tin tài khoản và học tập")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Thông tin liên hệ phụ huynh")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Dữ liệu xác thực và thanh toán")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/terms\"")));
    }
}
