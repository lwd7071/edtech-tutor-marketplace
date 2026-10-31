package com.edtech.platform.dashboard.web;

import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.dashboard.dto.response.StudentDashboardView;
import com.edtech.platform.dashboard.service.StudentDashboardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(StudentDashboardPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class StudentDashboardPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private StudentDashboardService dashboardService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private UserStatusCacheService userStatusCacheService;
    @MockBean private SessionService sessionService;
    @MockBean private Clock clock;

    private final UUID studentId = UUID.randomUUID();
    private final UsernamePasswordAuthenticationToken student = new UsernamePasswordAuthenticationToken(
            new AuthenticatedUser(studentId, "student@example.com", "STUDENT"), null, List.of());

    @BeforeEach
    void authenticateStudent() {
        SecurityContextHolder.getContext().setAuthentication(student);
    }

    @Test
    void rendersSummaryAndUpcomingLessonInVietnameseTime() throws Exception {
        when(dashboardService.getDashboard(studentId)).thenReturn(
                new StudentDashboardView(Instant.parse("2026-09-14T03:00:00Z"), 12, 3, 4, 2));

        mockMvc.perform(get("/student"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/dashboard"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("10:00 · Thứ Hai, 14/09/2026")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("12")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Việc cần theo dõi")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Thông báo chưa đọc")));
    }

    @Test
    void rendersExplicitEmptyNextLessonAndZeroCounters() throws Exception {
        when(dashboardService.getDashboard(studentId)).thenReturn(new StudentDashboardView(null, 0, 0, 0, 0));

        mockMvc.perform(get("/student"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa có lịch học")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Thông báo chưa đọc")));
    }

    @Test
    void displaysRetryStateWhenSummaryCannotBeLoaded() throws Exception {
        when(dashboardService.getDashboard(studentId)).thenThrow(new IllegalStateException("unavailable"));

        mockMvc.perform(get("/student"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tải được tổng quan.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/student\">Thử lại")));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Các card Buổi học tiếp theo và Đang tìm gia sư không có viền vàng dạ quang, đồng bộ var(--border)")
    void cardsShouldNotHaveBrandYellowBorderAndUseConsistentBorder() throws Exception {
        java.nio.file.Path cssPath = java.nio.file.Path.of("src/main/resources/static/css/student-dashboard.css");
        org.junit.jupiter.api.Assertions.assertTrue(java.nio.file.Files.exists(cssPath), "File CSS student-dashboard.css phải tồn tại");
        String css = java.nio.file.Files.readString(cssPath);

        org.junit.jupiter.api.Assertions.assertFalse(
                css.contains("overview-card--next { background: var(--surface); border-color: var(--brand-yellow); }") ||
                css.contains("overview-card--next { border-color: var(--brand-yellow)"),
                "Thẻ Buổi học tiếp theo không được có viền vàng dạ quang brand-yellow"
        );

        org.junit.jupiter.api.Assertions.assertFalse(
                css.contains(".dashboard-next-step { display: flex; flex-direction: column; align-items: flex-start; justify-content: center; padding: 24px; border: 1px solid var(--brand-yellow);"),
                "Thẻ Đang tìm gia sư không được có viền vàng dạ quang brand-yellow"
        );

        org.junit.jupiter.api.Assertions.assertTrue(
                css.contains(".dashboard-next-step { display: flex; flex-direction: column; align-items: flex-start; justify-content: center; padding: 24px; border: 1px solid var(--border);"),
                "Thẻ Đang tìm gia sư phải dùng border chuẩn 1px solid var(--border) đồng bộ toàn trang"
        );
    }
}
