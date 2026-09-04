package com.edtech.platform.admin.controller;

import com.edtech.platform.admin.dto.request.UpdatePlatformSettingsRequest;
import com.edtech.platform.admin.dto.response.PlatformSettingsView;
import com.edtech.platform.admin.service.AdminSettingsService;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.security.RequireRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class AdminSettingsController {

    private final AdminSettingsService adminSettingsService;

    @GetMapping
    public ApiResponse<PlatformSettingsView> getSettings() {
        return ApiResponse.ok(adminSettingsService.getSettings());
    }

    @PutMapping
    public ApiResponse<PlatformSettingsView> updateSettings(@Valid @RequestBody UpdatePlatformSettingsRequest request) {
        return ApiResponse.ok(adminSettingsService.updateSettings(request));
    }
}