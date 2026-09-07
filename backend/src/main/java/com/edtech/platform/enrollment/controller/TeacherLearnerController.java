package com.edtech.platform.enrollment.controller;

import com.edtech.platform.enrollment.service.TeacherLearnerService;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.response.PageMeta;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.RequireRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

@RestController
@RequestMapping("/api/teacher/students")
@RequireRole("TEACHER")
@RequiredArgsConstructor
public class TeacherLearnerController {
    private final TeacherLearnerService service;

    @GetMapping
    public ApiResponse<java.util.List<TeacherLearnerService.LearnerPackage>> list(
            @AuthenticationPrincipal AuthenticatedUser user, @RequestParam(required=false) UUID studentId, Pageable pageable) {
        var page = service.list(user.id(), studentId, pageable);
        return ApiResponse.page(page.getContent(), PageMeta.from(page));
    }
}

