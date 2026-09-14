package com.edtech.platform.dashboard.controller;

import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.RequireRole;
import com.edtech.platform.dashboard.dto.response.StudentDashboardView;
import com.edtech.platform.dashboard.service.StudentDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/dashboard")
@RequiredArgsConstructor
@RequireRole("STUDENT")
public class StudentDashboardController {

    private final StudentDashboardService service;

    @GetMapping
    public ApiResponse<StudentDashboardView> getDashboard(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(service.getDashboard(user.id()));
    }
}
