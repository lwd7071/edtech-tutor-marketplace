package com.edtech.platform.subject.controller;

import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.subject.dto.CreateSubjectProposalRequest;
import com.edtech.platform.subject.dto.SubjectProposalView;
import com.edtech.platform.subject.service.SubjectProposalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teacher/subject-proposals")
@RequiredArgsConstructor
public class TeacherSubjectProposalController {

    private final SubjectProposalService subjectProposalService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SubjectProposalView> createProposal(
            @Valid @RequestBody CreateSubjectProposalRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.created(subjectProposalService.createProposal(user.id(), request));
    }

    @GetMapping
    public ApiResponse<List<SubjectProposalView>> getProposals(
            @RequestParam(required = false) String status,
            Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser user) {
        Page<SubjectProposalView> page = subjectProposalService.getProposals(user.id(), status, pageable);
        return ApiResponse.page(page.getContent(), com.edtech.platform.common.response.PageMeta.from(page));
    }
}
