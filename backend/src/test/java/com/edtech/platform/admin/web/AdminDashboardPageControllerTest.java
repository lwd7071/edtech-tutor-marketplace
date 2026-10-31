package com.edtech.platform.admin.web;

import com.edtech.platform.admin.dto.response.AdminDashboardView;
import com.edtech.platform.admin.service.AdminDashboardService;
import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.time.Clock;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminDashboardPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminDashboardPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private AdminDashboardService dashboardService;
    @MockBean private JwtTokenProvider tokens;
    @MockBean private UserStatusCacheService statuses;
    @MockBean private SessionService sessions;
    @MockBean private Clock clock;

    @BeforeEach
    void authenticateAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(java.util.UUID.randomUUID(), "admin@example.test", "ADMIN"), null, List.of()));
    }

    @Test
    void rendersRealDashboardFieldsOnBothWorkspacePathsWithoutJavaScript() throws Exception {
        when(dashboardService.getDashboard()).thenReturn(new AdminDashboardView(
                1_250_000L, 125_000L, 12, 240, 380, 300, 60, 20, 3, 2_400_000L, 2));

        for (String path : List.of("/admin", "/admin/dashboard")) {
            mockMvc.perform(get(path))
                    .andExpect(status().isOk())
                    .andExpect(view().name("workspace/admin-dashboard"))
                    .andExpect(header().string("Cache-Control", "no-store, max-age=0"))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Yêu cầu tài chính đang chờ")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("2.400.000 ₫")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("1.250.000 ₫")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Gia sư đã duyệt")))
                    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script"))));
        }
    }

    @Test
    void showsZeroQueueStateFromRealDashboardValues() throws Exception {
        when(dashboardService.getDashboard()).thenReturn(new AdminDashboardView(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Không có yêu cầu đang chờ.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tổng số buổi")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("GMV đã thanh toán")));
    }

    @Test
    void showsRetryInsteadOfExposingDashboardFailureDetails() throws Exception {
        when(dashboardService.getDashboard()).thenThrow(new IllegalStateException("database details must stay private"));

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tải được số liệu quản trị.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Thử tải lại")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("database details"))));
    }
}
