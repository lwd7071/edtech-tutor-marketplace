package com.edtech.platform.learning.controller;

import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.RequireRole;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.response.PageMeta;
import com.edtech.platform.learning.domain.AssignmentStatus;
import com.edtech.platform.learning.dto.request.CreateSubmissionRequest;
import com.edtech.platform.learning.dto.response.AssignmentDetail;
import com.edtech.platform.learning.dto.response.SubmissionDetail;
import com.edtech.platform.learning.service.StudentAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.util.UUID;

@RestController
@RequestMapping("/api/student")
@Validated
@RequiredArgsConstructor
public class StudentAssignmentController {

    private final StudentAssignmentService studentAssignmentService;

    @GetMapping("/assignments")
    @RequireRole("STUDENT")
    public ApiResponse<java.util.List<AssignmentDetail>> getAssignments(
            @AuthenticationPrincipal AuthenticatedUser userDetails,
            @RequestParam(required = false) AssignmentStatus status,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Trang phải lớn hơn hoặc bằng 0") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "Kích thước trang phải lớn hơn 0") @Max(value = 100, message = "Kích thước tối đa là 100") int size) {
        
        Page<AssignmentDetail> result = studentAssignmentService.getAssignments(userDetails.id(), status, PageRequest.of(page, size));
        return ApiResponse.page(result.getContent(), PageMeta.from(result));
    }

    @PostMapping("/assignments/{id}/submissions")
    @RequireRole("STUDENT")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SubmissionDetail> createSubmission(
            @AuthenticationPrincipal AuthenticatedUser userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody CreateSubmissionRequest request) {
        
        return ApiResponse.created(studentAssignmentService.createOrUpdateSubmission(userDetails.id(), id, request));
    }
}
