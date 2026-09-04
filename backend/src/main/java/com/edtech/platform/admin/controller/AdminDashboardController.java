package com.edtech.platform.admin.controller;

import com.edtech.platform.admin.dto.response.AdminDashboardView;
import com.edtech.platform.admin.service.AdminDashboardService;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.security.RequireRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @GetMapping
    public ApiResponse<AdminDashboardView> getDashboard() {
        return ApiResponse.ok(dashboardService.getDashboard());
    }
}