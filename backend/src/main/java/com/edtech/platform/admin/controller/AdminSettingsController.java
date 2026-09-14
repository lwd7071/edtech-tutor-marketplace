package com.edtech.platform.admin.controller;

import com.edtech.platform.admin.dto.request.UpdatePlatformSettingsRequest;
import com.edtech.platform.admin.dto.response.PlatformSettingsView;
import com.edtech.platform.admin.service.AdminSettingsService;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.security.RequireRole;
import com.edtech.platform.common.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

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
    public ApiResponse<PlatformSettingsView> updateSettings(@Valid @RequestBody UpdatePlatformSettingsRequest request,
                                                            @AuthenticationPrincipal AuthenticatedUser actor) {
        return ApiResponse.ok(actor == null || actor.id() == null
                ? adminSettingsService.updateSettings(request)
                : adminSettingsService.updateSettings(actor.id(), request));
    }
}
