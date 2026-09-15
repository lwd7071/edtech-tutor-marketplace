package com.edtech.platform.teacher.controller;

import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.teacher.dto.TeacherProfileDetail;
import com.edtech.platform.teacher.dto.UpdateTeacherProfileRequest;
import com.edtech.platform.teacher.dto.UpdateTeacherResidenceRequest;
import com.edtech.platform.teacher.service.TeacherProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/teacher/profile")
@RequiredArgsConstructor
public class TeacherProfileController {

    private final TeacherProfileService teacherProfileService;

    @GetMapping
    public ApiResponse<TeacherProfileDetail> getProfile(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(teacherProfileService.getProfile(user.id()));
    }

    @PutMapping
    public ApiResponse<TeacherProfileDetail> updateProfile(
            @Valid @RequestBody UpdateTeacherProfileRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(teacherProfileService.updateProfile(user.id(), request));
    }

    @PutMapping("/residence")
    public ApiResponse<TeacherProfileDetail> updateResidence(
            @Valid @RequestBody UpdateTeacherResidenceRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(teacherProfileService.updateResidence(user.id(), request));
    }

    @PostMapping("/submit")
    public ApiResponse<TeacherProfileDetail> submitProfile(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(teacherProfileService.submitProfile(user.id()));
    }
}
