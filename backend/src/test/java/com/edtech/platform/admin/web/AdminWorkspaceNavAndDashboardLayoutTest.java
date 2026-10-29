package com.edtech.platform.admin.web;

import com.edtech.platform.admin.dto.response.AdminDashboardView;
import com.edtech.platform.admin.dto.response.PlatformSettingsView;
import com.edtech.platform.admin.service.AdminDashboardService;
import com.edtech.platform.admin.service.AdminSettingsService;
import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import jakarta.validation.Validator;
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

import java.math.BigDecimal;
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
 * TDD: Kiểm tra hành vi giao diện mới của Admin Workspace:
 * Header định danh vai trò, Top Navigation 5 nhóm chính, Contextual Sub-pills và Bento Dashboard.
 */
@WebMvcTest({AdminDashboardPageController.class, AdminSettingsPageController.class})
@AutoConfigureMockMvc(addFilters = false)
class AdminWorkspaceNavAndDashboardLayoutTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AdminDashboardService dashboardService;
    @MockBean private AdminSettingsService settingsService;
    @MockBean private Validator validator;
    @MockBean private JwtTokenProvider tokens;
    @MockBean private UserStatusCacheService statuses;
    @MockBean private SessionService sessions;
    @MockBean private Clock clock;

    private final UUID adminId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new AuthenticatedUser(adminId, "admin@example.test", "ADMIN"), null, List.of()
                )
        );
        when(dashboardService.getDashboard()).thenReturn(new AdminDashboardView(
                1_250_000L, 125_000L, 12, 240, 380, 300, 60, 20, 3, 2_400_000L, 2
        ));
        when(settingsService.getSettings()).thenReturn(new PlatformSettingsView(
                UUID.randomUUID(), new BigDecimal("15.0"), 24, 48, 72, Instant.now()
        ));
    }

    @Test
    @DisplayName("Slice 1: Header phải có Badge định danh Quản trị viên và form đăng xuất")
    void headerShouldContainAdminRoleBadgeAndLogoutForm() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("admin-role-badge")))
                .andExpect(content().string(containsString("Quản trị viên")))
                .andExpect(content().string(containsString("/auth/logout")));
    }

    @Test
    @DisplayName("Slice 1: Top Navigation phải gom gọn vào 5 nhóm chính không còn dàn trải 11 tab")
    void navigationShouldRenderFiveMainGroups() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("admin-workspace-nav__main")))
                .andExpect(content().string(containsString("aria-current=\"page\">Tổng quan</a>")))
                .andExpect(content().string(containsString("Xét duyệt")))
                .andExpect(content().string(containsString("Tài chính")))
                .andExpect(content().string(containsString("Người dùng")))
                .andExpect(content().string(containsString("Hệ thống")));
    }

    @Test
    @DisplayName("Slice 2: Trang Dashboard Tổng quan không hiển thị Sub-pills thừa")
    void dashboardShouldNotRenderSubPills() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("admin-workspace-nav__sub"))));
    }

    @Test
    @DisplayName("Slice 2: Trang Hệ thống (Cài đặt) phải hiển thị Sub-pills ngữ cảnh (Cài đặt, Nhật ký)")
    void settingsPageShouldRenderSystemSubPills() throws Exception {
        mockMvc.perform(get("/admin/settings"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("admin-workspace-nav__sub")))
                .andExpect(content().string(containsString("admin-sub-pills")))
                .andExpect(content().string(containsString("aria-current=\"page\">Hệ thống</a>")))
                .andExpect(content().string(containsString("is-active\">Cài đặt</a>")))
                .andExpect(content().string(containsString("Nhật ký")));
    }

    @Test
    @DisplayName("Slice 3: Dashboard phải hiển thị 4 KPI Stat Cards trên cùng")
    void dashboardShouldRenderFourKpiStatCards() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("admin-stat-cards")))
                .andExpect(content().string(containsString("GMV đã thanh toán")))
                .andExpect(content().string(containsString("Hoa hồng ghi nhận")))
                .andExpect(content().string(containsString("Yêu cầu rút tiền")))
                .andExpect(content().string(containsString("Yêu cầu hoàn tiền")));
    }

    @Test
    @DisplayName("Slice 3: Dashboard phải tổ chức nội dung theo Bento Grid và Card Sections kèm link hành động")
    void dashboardShouldOrganizeContentInBentoGridAndCards() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("admin-dashboard-bento-grid")))
                .andExpect(content().string(containsString("admin-card-section")))
                .andExpect(content().string(containsString("/admin/payouts")))
                .andExpect(content().string(containsString("/admin/refunds")));
    }

    @Test
    @DisplayName("Logo Tutor Match ở Header admin phải dẫn về trang chủ /")
    void brandLogoShouldLinkToHomepage() throws Exception {
        String html = mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        int brandStart = html.indexOf("admin-brand-wrapper");
        org.junit.jupiter.api.Assertions.assertTrue(brandStart >= 0, "Phải tìm thấy thẻ admin-brand-wrapper");
        int brandEnd = html.indexOf("</a>", brandStart);
        String brandContent = html.substring(brandStart, brandEnd);

        org.junit.jupiter.api.Assertions.assertTrue(brandContent.contains("href=\"/\""),
                "Logo admin phải có href=\"/\"");
    }
}
