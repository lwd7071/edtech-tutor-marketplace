package com.edtech.platform.dashboard.web;

import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.dashboard.dto.response.StudentDashboardView;
import com.edtech.platform.dashboard.service.StudentDashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TDD: Kiểm tra giao diện Bottom Navigation Bar trên Mobile cho Học viên (Student Workspace).
 */
@WebMvcTest(StudentDashboardPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class StudentMobileNavLayoutTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private StudentDashboardService dashboardService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private UserStatusCacheService userStatusCacheService;
    @MockBean private SessionService sessionService;
    @MockBean private Clock clock;

    private final UUID studentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new AuthenticatedUser(studentId, "student@example.test", "STUDENT"), null, List.of()
                )
        );
        when(dashboardService.getDashboard(studentId)).thenReturn(
                new StudentDashboardView(Instant.parse("2026-09-14T03:00:00Z"), 12, 3, 4, 2)
        );
    }

    @Test
    @DisplayName("Học viên phải có thanh Mobile Bottom Nav cố định với 4 tabs cốt lõi và icon SVG")
    void studentWorkspaceShouldRenderMobileBottomNavWithFourCoreTabs() throws Exception {
        mockMvc.perform(get("/student"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("mobile-bottom-nav")))
                .andExpect(content().string(containsString("aria-label=\"Điều hướng di động học viên\"")))
                .andExpect(content().string(containsString("href=\"/student\"")))
                .andExpect(content().string(containsString("href=\"/student/packages\"")))
                .andExpect(content().string(containsString("href=\"/student/bookings\"")))
                .andExpect(content().string(containsString("href=\"/student/messages\"")));
    }

    @Test
    @DisplayName("Tab hiện tại (Tổng quan) phải có class active và aria-current='page'")
    void activeTabShouldHaveActiveClassAndAriaCurrent() throws Exception {
        String html = mockMvc.perform(get("/student"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        int mobileNavStart = html.indexOf("class=\"mobile-bottom-nav\"");
        org.junit.jupiter.api.Assertions.assertTrue(mobileNavStart >= 0, "Phải tìm thấy thẻ mobile-bottom-nav");
        int mobileNavEnd = html.indexOf("</nav>", mobileNavStart);
        String mobileNavContent = html.substring(mobileNavStart, mobileNavEnd);

        org.junit.jupiter.api.Assertions.assertTrue(
                mobileNavContent.contains("mobile-bottom-nav__item--active"),
                "Tab đang chọn trên mobile nav phải có class mobile-bottom-nav__item--active"
        );
        org.junit.jupiter.api.Assertions.assertTrue(
                mobileNavContent.contains("aria-current=\"page\""),
                "Tab đang chọn trên mobile nav phải có thuộc tính aria-current=\"page\""
        );
    }
}
