package com.edtech.platform.admin.controller;

import com.edtech.platform.admin.dto.request.ApproveExtensionRequest;
import com.edtech.platform.admin.dto.request.RejectRequest;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.response.PageMeta;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.RequireRole;
import com.edtech.platform.finance.dto.response.ExtensionRequestView;
import com.edtech.platform.finance.service.ExtensionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/extension-requests")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class AdminExtensionController {

    private final ExtensionService extensionService;

    @GetMapping
    public ApiResponse<List<ExtensionRequestView>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        size = Math.min(size, 100);
        Page<ExtensionRequestView> result = extensionService.findAdminExtensions(status, PageRequest.of(page, size));
        return ApiResponse.page(result.getContent(), PageMeta.from(result));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<ExtensionRequestView> approve(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @Valid @RequestBody ApproveExtensionRequest request
    ) {
        return ApiResponse.ok(extensionService.approveExtension(user.id(), id, request));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<ExtensionRequestView> reject(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @Valid @RequestBody RejectRequest request
    ) {
        return ApiResponse.ok(extensionService.rejectExtension(user.id(), id, request));
    }
}