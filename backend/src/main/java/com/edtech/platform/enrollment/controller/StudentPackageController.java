package com.edtech.platform.enrollment.controller;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.response.PageMeta;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.RequireRole;
import com.edtech.platform.enrollment.domain.StudentPackageStatus;
import com.edtech.platform.enrollment.dto.StudentOwnedPackageView;
import com.edtech.platform.enrollment.service.StudentPackageReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/student/packages")
@RequiredArgsConstructor
@RequireRole("STUDENT")
public class StudentPackageController {
    private final StudentPackageReadService packageReads;

    @GetMapping
    public ApiResponse<List<StudentOwnedPackageView>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        StudentPackageStatus parsed = null;
        try {
            if (status != null) parsed = StudentPackageStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException invalid) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid package status");
        }
        var result = packageReads.list(user.id(), parsed, PageRequest.of(page, Math.min(size, 100)));
        return ApiResponse.page(result.getContent(), PageMeta.from(result));
    }

    @GetMapping("/{id}")
    public ApiResponse<StudentOwnedPackageView> detail(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(packageReads.detail(id, user.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)));
    }
}
