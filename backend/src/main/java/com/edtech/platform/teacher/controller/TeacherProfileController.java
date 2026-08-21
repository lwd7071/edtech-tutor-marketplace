package com.edtech.platform.teacher.controller;

import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.teacher.dto.TeacherProfileDetail;
import com.edtech.platform.teacher.dto.UpdateTeacherProfileRequest;
import com.edtech.platform.teacher.service.TeacherProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/teacher/profile")
@RequiredArgsConstructor
public class TeacherProfileController {

    private final TeacherProfileService teacherProfileService;

    @GetMapping
    public ApiResponse<TeacherProfileDetail> getProfile(Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        return ApiResponse.ok(teacherProfileService.getProfile(userId));
    }

    @PutMapping
    public ApiResponse<TeacherProfileDetail> updateProfile(
            @Valid @RequestBody UpdateTeacherProfileRequest request,
            Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        return ApiResponse.ok(teacherProfileService.updateProfile(userId, request));
    }

    @PostMapping("/submit")
    public ApiResponse<TeacherProfileDetail> submitProfile(Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        return ApiResponse.ok(teacherProfileService.submitProfile(userId));
    }
}
