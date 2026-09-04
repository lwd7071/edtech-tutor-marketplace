package com.edtech.platform.admin.controller;

import com.edtech.platform.admin.dto.response.AdminDashboardView;
import com.edtech.platform.admin.service.AdminDashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminDashboardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminDashboardService dashboardService;

    @InjectMocks
    private AdminDashboardController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getDashboard_shouldReturn200() throws Exception {
        AdminDashboardView view = new AdminDashboardView(
                50_000_000L, 2_500_000L, 10L, 100L, 50L, 40L, 8L, 2L, 3L, 1_500_000L, 1L
        );
        when(dashboardService.getDashboard()).thenReturn(view);

        mockMvc.perform(get("/api/admin/dashboard")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalGmvVnd").value(50000000))
                .andExpect(jsonPath("$.data.totalCommissionVnd").value(2500000))
                .andExpect(jsonPath("$.data.totalTeachers").value(10))
                .andExpect(jsonPath("$.data.totalStudents").value(100));
    }
}