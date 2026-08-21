package com.edtech.platform.learning.controller;

import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.RequireRole;
import com.edtech.platform.learning.dto.request.CreateAssignmentRequest;
import com.edtech.platform.learning.dto.request.GradeSubmissionRequest;
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

    @PostMapping("/assignments")
    @RequireRole("TEACHER")
    public AssignmentDetail createAssignment(
            @AuthenticationPrincipal AuthenticatedUser userDetails,
            @Valid @RequestBody CreateAssignmentRequest request) {
        
        return teacherAssignmentService.createAssignment(userDetails.id(), request);
    }

    @PostMapping("/submissions/{id}/grade")
    @RequireRole("TEACHER")
    public SubmissionDetail gradeSubmission(
            @AuthenticationPrincipal AuthenticatedUser userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody GradeSubmissionRequest request) {
        
        return teacherAssignmentService.gradeSubmission(userDetails.id(), id, request);
    }
}
