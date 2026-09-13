package com.edtech.platform.learning.controller;

import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.RequireRole;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.learning.dto.request.CreateAssignmentRequest;
import com.edtech.platform.learning.dto.request.GradeSubmissionRequest;
import com.edtech.platform.learning.dto.request.VersionedAssignmentActionRequest;
import jakarta.validation.constraints.Min;
import com.edtech.platform.learning.dto.response.AssignmentDetail;
import com.edtech.platform.learning.dto.response.SubmissionDetail;
import com.edtech.platform.learning.service.TeacherAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherAssignmentController {

    private final TeacherAssignmentService teacherAssignmentService;

    @org.springframework.web.bind.annotation.PutMapping("/assignments/{id}")
    @RequireRole("TEACHER")
    public ApiResponse<AssignmentDetail> update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
            @Valid @RequestBody CreateAssignmentRequest request) {
        return ApiResponse.ok(teacherAssignmentService.updateDraft(user.id(), id, request));
    }

    @PostMapping("/assignments/{id}/publish")
    @RequireRole("TEACHER")
    public ApiResponse<AssignmentDetail> publish(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id, @Valid @RequestBody VersionedAssignmentActionRequest request) {
        return ApiResponse.ok(teacherAssignmentService.transition(user.id(), id, com.edtech.platform.learning.domain.AssignmentStatus.PUBLISHED, request.version()));
    }

    @PostMapping("/assignments/{id}/close")
    @RequireRole("TEACHER")
    public ApiResponse<AssignmentDetail> close(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id, @Valid @RequestBody VersionedAssignmentActionRequest request) {
        return ApiResponse.ok(teacherAssignmentService.transition(user.id(), id, com.edtech.platform.learning.domain.AssignmentStatus.CLOSED, request.version()));
    }

    @PostMapping("/assignments")
    @RequireRole("TEACHER")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AssignmentDetail> createAssignment(
            @AuthenticationPrincipal AuthenticatedUser userDetails,
            @Valid @RequestBody CreateAssignmentRequest request) {
        
        return ApiResponse.created(teacherAssignmentService.createAssignment(userDetails.id(), request));
    }

    @PostMapping("/submissions/{id}/grade")
    @RequireRole("TEACHER")
    public ApiResponse<SubmissionDetail> gradeSubmission(
            @AuthenticationPrincipal AuthenticatedUser userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody GradeSubmissionRequest request) {
        
        return ApiResponse.ok(teacherAssignmentService.gradeSubmission(userDetails.id(), id, request));
    }
}
