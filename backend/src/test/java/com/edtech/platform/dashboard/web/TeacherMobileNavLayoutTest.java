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

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TDD: Kiểm tra giao diện Bottom Navigation Bar trên Mobile cho Gia sư (Teacher Workspace).
 */
@WebMvcTest(TeacherDashboardPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeacherMobileNavLayoutTest {

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
    @DisplayName("Gia sư phải có thanh Mobile Bottom Nav cố định với 4 tabs: Tổng quan, Lịch dạy, Học viên, Ví tiền")
    void teacherWorkspaceShouldRenderMobileBottomNavWithFourCoreTabs() throws Exception {
        mockMvc.perform(get("/teacher"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("mobile-bottom-nav")))
                .andExpect(content().string(containsString("aria-label=\"Điều hướng di động gia sư\"")))
                .andExpect(content().string(containsString("href=\"/teacher\"")))
                .andExpect(content().string(containsString("href=\"/teacher/bookings\"")))
                .andExpect(content().string(containsString("href=\"/teacher/students\"")))
                .andExpect(content().string(containsString("href=\"/teacher/wallet\"")));
    }

    @Test
    @DisplayName("Tab hiện tại (Tổng quan) của gia sư phải có class active và aria-current='page'")
    void activeTabShouldHaveActiveClassAndAriaCurrentForTeacher() throws Exception {
        String html = mockMvc.perform(get("/teacher"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        int mobileNavStart = html.indexOf("aria-label=\"Điều hướng di động gia sư\"");
        org.junit.jupiter.api.Assertions.assertTrue(mobileNavStart >= 0, "Phải tìm thấy thẻ mobile-bottom-nav của gia sư");
        int mobileNavEnd = html.indexOf("</nav>", mobileNavStart);
        String mobileNavContent = html.substring(mobileNavStart, mobileNavEnd);

        org.junit.jupiter.api.Assertions.assertTrue(
                mobileNavContent.contains("mobile-bottom-nav__item--active"),
                "Tab đang chọn trên mobile nav gia sư phải có class mobile-bottom-nav__item--active"
        );
        org.junit.jupiter.api.Assertions.assertTrue(
                mobileNavContent.contains("aria-current=\"page\""),
                "Tab đang chọn trên mobile nav gia sư phải có thuộc tính aria-current=\"page\""
        );
    }
}
