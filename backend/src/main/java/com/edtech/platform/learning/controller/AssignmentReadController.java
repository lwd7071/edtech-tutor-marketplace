package com.edtech.platform.learning.controller;

import com.edtech.platform.learning.service.AssignmentReadService;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.RequireRole;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.response.PageMeta;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AssignmentReadController {
    private final AssignmentReadService service;

    @GetMapping("/api/teacher/assignments")
    @RequireRole("TEACHER")
    public ApiResponse<?> list(@AuthenticationPrincipal AuthenticatedUser user, Pageable pageable) {
        var page = service.teacherList(user.id(),pageable);
        return ApiResponse.page(page.getContent(),PageMeta.from(page));
    }
    @GetMapping("/api/teacher/assignments/{id}")
    @RequireRole("TEACHER")
    public ApiResponse<?> teacherDetail(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable UUID id) {
        return ApiResponse.ok(service.detail(user.id(),id,true));
    }
    @GetMapping("/api/teacher/submissions/{id}")
    @RequireRole("TEACHER")
    public ApiResponse<?> submission(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable UUID id) {
        return ApiResponse.ok(service.submission(user.id(),id));
    }
}

