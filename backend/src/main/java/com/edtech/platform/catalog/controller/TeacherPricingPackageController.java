package com.edtech.platform.catalog.controller;

import com.edtech.platform.catalog.dto.ChangePackageStatusRequest;
import com.edtech.platform.catalog.dto.PricingPackageView;
import com.edtech.platform.catalog.dto.UpsertPricingPackageRequest;
import com.edtech.platform.catalog.service.PricingPackageService;
import com.edtech.platform.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/teacher/packages")
@RequiredArgsConstructor
public class TeacherPricingPackageController {

    private final PricingPackageService pricingPackageService;

    @PostMapping
    public ApiResponse<PricingPackageView> createPackage(
            @Valid @RequestBody UpsertPricingPackageRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.edtech.platform.common.security.AuthenticatedUser userDetails) {
        UUID userId = userDetails.id();
        return ApiResponse.ok(pricingPackageService.createPackage(userId, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<PricingPackageView> updatePackage(
            @PathVariable UUID id,
            @Valid @RequestBody UpsertPricingPackageRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.edtech.platform.common.security.AuthenticatedUser userDetails) {
        UUID userId = userDetails.id();
        return ApiResponse.ok(pricingPackageService.updatePackage(userId, id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<PricingPackageView> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ChangePackageStatusRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.edtech.platform.common.security.AuthenticatedUser userDetails) {
        UUID userId = userDetails.id();
        return ApiResponse.ok(pricingPackageService.changeStatus(userId, id, request));
    }
}
