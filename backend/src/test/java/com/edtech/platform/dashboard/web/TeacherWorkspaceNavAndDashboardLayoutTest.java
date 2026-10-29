package com.edtech.platform.dashboard.web;

import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.booking.service.BookingReadService;
import com.edtech.platform.booking.service.BookingRequestService;
import com.edtech.platform.booking.service.TrialRequestService;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.finance.dto.response.WalletView;
import com.edtech.platform.finance.service.WalletQueryService;
import com.edtech.platform.learning.service.AssignmentReadService;
import com.edtech.platform.teacher.domain.ProfileStatus;
import com.edtech.platform.teacher.dto.TeacherProfileDetail;
import com.edtech.platform.teacher.service.TeacherProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TDD: Kiểm tra hành vi giao diện mới của thanh Header, Grouped Nav (5 nhóm) và KPI Stat Cards.
 */
@WebMvcTest(TeacherDashboardPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeacherWorkspaceNavAndDashboardLayoutTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private TeacherProfileService profiles;
    @MockBean private BookingRequestService bookingRequests;
    @MockBean private BookingReadService bookings;
    @MockBean private TrialRequestService trials;
    @MockBean private AssignmentReadService assignments;
    @MockBean private WalletQueryService wallets;
    @MockBean private JwtTokenProvider tokens;
    @MockBean private UserStatusCacheService statuses;
    @MockBean private SessionService sessions;
    @MockBean private Clock clock;

    private final UUID teacherId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-10-29T10:00:00Z");

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new AuthenticatedUser(teacherId, "teacher@example.test", "TEACHER"), null, List.of()
                )
        );
        when(clock.instant()).thenReturn(now);

        // Mock dữ liệu cơ bản
        TeacherProfileDetail profile = new TeacherProfileDetail(
                teacherId, "Bio", 5, List.of("vi"), true, false, "Hà Nội", "HN", "W1", null, ProfileStatus.APPROVED, null, true, true
        );
        when(profiles.getProfile(teacherId)).thenReturn(profile);
        when(bookingRequests.findTeacherWorkspace(eq(teacherId), any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(bookings.findUpcomingTeacherWorkspace(eq(teacherId), any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(trials.findPendingTeacherWorkspace(eq(teacherId), any())).thenReturn(new PageImpl<>(List.of()));
        when(assignments.teacherList(eq(teacherId), any())).thenReturn(new PageImpl<>(List.of()));
        when(wallets.getOrCreateWallet(teacherId)).thenReturn(new WalletView(UUID.randomUUID(), teacherId, 0, 0, 0, 0, 0));
    }

    @Test
    @DisplayName("Header phải có biểu tượng Tin nhắn, Thông báo và Form đăng xuất")
    void headerShouldContainDirectActionLinks() throws Exception {
        mockMvc.perform(get("/teacher"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/teacher/messages")))
                .andExpect(content().string(containsString("/teacher/notifications")))
                .andExpect(content().string(containsString("/auth/logout")));
    }

    @Test
    @DisplayName("Top navigation phải gom gọn vào 5 nhóm chính")
    void navigationShouldContainOnlyFiveMainGroups() throws Exception {
        mockMvc.perform(get("/teacher"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tổng quan")))
                .andExpect(content().string(containsString("Lịch & Yêu cầu")))
                .andExpect(content().string(containsString("Lớp & Học tập")))
                .andExpect(content().string(containsString("Thu nhập & Ví")))
                .andExpect(content().string(containsString("Hồ sơ & Môn dạy")));
    }

    @Test
    @DisplayName("Dashboard phải hiển thị 4 KPI Stat Cards")
    void dashboardShouldRenderKpiStatCards() throws Exception {
        mockMvc.perform(get("/teacher"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("teacher-stat-cards")))
                .andExpect(content().string(containsString("Lịch hôm nay")))
                .andExpect(content().string(containsString("Yêu cầu chờ")))
                .andExpect(content().string(containsString("Bài tập cần chấm")))
                .andExpect(content().string(containsString("Số dư khả dụng")));
    }

    @Test
    @DisplayName("Các section ở phần thân dashboard phải được đóng gói vào các Card có cấu trúc hoàn chỉnh")
    void dashboardSectionsShouldBeOrganizedInCards() throws Exception {
        mockMvc.perform(get("/teacher"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("teacher-dashboard-main-grid")))
                .andExpect(content().string(containsString("teacher-card-section")))
                .andExpect(content().string(containsString("Buổi học sắp tới")))
                .andExpect(content().string(containsString("Lịch dạy sắp tới")))
                .andExpect(content().string(containsString("Yêu cầu học thử đang chờ")))
                .andExpect(content().string(containsString("Bài tập có bài nộp")))
                .andExpect(content().string(containsString("Thu nhập")));
    }
}
