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
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TDD: Kiểm tra giao diện Header & Nav của Học viên (Student Workspace):
 * Nút Thông báo tích hợp lên Header (dạng icon chuông) và thanh Tab bên dưới chỉ thuần phục vụ học tập.
 */
@WebMvcTest(StudentDashboardPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class StudentHeaderAndNavLayoutTest {

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
    @DisplayName("Header của học viên phải có icon chuông dẫn tới /student/notifications")
    void headerShouldContainNotificationLinkWithIconAndLabel() throws Exception {
        mockMvc.perform(get("/student"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("student-header-notification-link")))
                .andExpect(content().string(containsString("/student/notifications")));
    }

    @Test
    @DisplayName("Tag vai trò Học viên phải nằm ở bên trái cạnh Brand logo và không còn ở cụm hành động bên phải")
    void headerShouldPlaceStudentRoleBadgeNextToBrandOnTheLeft() throws Exception {
        String html = mockMvc.perform(get("/student"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("student-brand-wrapper")))
                .andExpect(content().string(containsString("student-role-badge")))
                .andExpect(content().string(containsString("Học viên")))
                .andReturn().getResponse().getContentAsString();

        int actionsStart = html.indexOf("student-site-header__actions");
        org.junit.jupiter.api.Assertions.assertTrue(actionsStart >= 0, "Phải tìm thấy thẻ student-site-header__actions");
        int actionsEnd = html.indexOf("</header>", actionsStart);
        String actionsContent = html.substring(actionsStart, actionsEnd);

        org.junit.jupiter.api.Assertions.assertFalse(actionsContent.contains("student-role-label"),
                "Cụm action bên phải không được còn class student-role-label");
    }

    @Test
    @DisplayName("Header của học viên phải có liên kết Tin nhắn dẫn tới /student/messages kèm SVG icon và nhãn")
    void headerShouldContainMessagesLinkWithIconAndLabel() throws Exception {
        mockMvc.perform(get("/student"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/student/messages\"")))
                .andExpect(content().string(containsString("Tin nhắn")))
                .andExpect(content().string(containsString("student-header-action-item")))
                .andExpect(content().string(containsString("M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z")));
    }

    @Test
    @DisplayName("Logo Tutor Match ở Header học viên phải dẫn về trang chủ /")
    void brandLogoShouldLinkToHomepage() throws Exception {
        String html = mockMvc.perform(get("/student"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        int brandStart = html.indexOf("student-brand-wrapper");
        org.junit.jupiter.api.Assertions.assertTrue(brandStart >= 0, "Phải tìm thấy thẻ student-brand-wrapper");
        int brandEnd = html.indexOf("</a>", brandStart);
        String brandContent = html.substring(brandStart, brandEnd);

        org.junit.jupiter.api.Assertions.assertTrue(brandContent.contains("href=\"/\""),
                "Logo học viên phải có href=\"/\"");
    }

    @Test
    @DisplayName("Thanh workspace-nav bên dưới chỉ chứa 7 tab học tập thuần túy và không còn tab Thông báo")
    void workspaceNavShouldContainOnlySevenLearningTabs() throws Exception {
        String html = mockMvc.perform(get("/student"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tổng quan")))
                .andExpect(content().string(containsString("Gói học")))
                .andExpect(content().string(containsString("Lịch học")))
                .andExpect(content().string(containsString("Bài tập")))
                .andExpect(content().string(containsString("Báo cáo buổi học")))
                .andExpect(content().string(containsString("Yêu cầu")))
                .andExpect(content().string(containsString("Hồ sơ")))
                .andReturn().getResponse().getContentAsString();

        int navStart = html.indexOf("<nav class=\"workspace-nav\"");
        org.junit.jupiter.api.Assertions.assertTrue(navStart >= 0, "Phải tìm thấy thẻ workspace-nav");
        int navEnd = html.indexOf("</nav>", navStart);
        org.junit.jupiter.api.Assertions.assertTrue(navEnd > navStart, "Phải tìm thấy thẻ đóng </nav>");

        String navContent = html.substring(navStart, navEnd);
        org.junit.jupiter.api.Assertions.assertFalse(navContent.contains("/student/notifications"),
                "Thanh workspace-nav không được chứa liên kết /student/notifications");
    }
}
