package com.edtech.platform.learning.service;

import com.edtech.platform.auth.facade.IdentityFacade;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.learning.domain.Assignment;
import com.edtech.platform.learning.domain.AssignmentStatus;
import com.edtech.platform.learning.domain.Submission;
import com.edtech.platform.learning.domain.SubmissionStatus;
import com.edtech.platform.learning.domain.StudentAssignmentProgress;
import com.edtech.platform.learning.dto.request.CreateSubmissionRequest;
import com.edtech.platform.learning.dto.response.AssignmentDetail;
import com.edtech.platform.learning.dto.response.SubmissionDetail;
import com.edtech.platform.learning.dto.response.StudentAssignmentDetail;
import com.edtech.platform.learning.repository.AssignmentRepository;
import com.edtech.platform.learning.repository.SubmissionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentAssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final IdentityFacade identityFacade;
    private final ObjectMapper objectMapper;
    private final AssignmentAttachmentBinder attachmentBinder;
    private final AssignmentViewMapper views;
    private final Clock clock;
    private final com.edtech.platform.common.facade.AttachmentFacade attachments;

    @Transactional(readOnly = true)
    public Page<AssignmentDetail> getAssignments(UUID studentId, AssignmentStatus status, Pageable pageable) {
        Page<Assignment> assignments;
        if (status == AssignmentStatus.DRAFT) return Page.empty(pageable);
        if (status != null) {
            assignments = assignmentRepository.findByStudentIdAndStatus(studentId, status, pageable);
        } else {
            assignments = assignmentRepository.findByStudentIdAndStatusIn(studentId, List.of(AssignmentStatus.PUBLISHED, AssignmentStatus.CLOSED), pageable);
        }
        return assignments.map(views::assignment);
    }

    @Transactional(readOnly = true)
    public Page<AssignmentDetail> getAssignments(UUID studentId, AssignmentStatus status,
                                                  StudentAssignmentProgress progress, Pageable pageable) {
        if (progress == null) return getAssignments(studentId, status, pageable);
        Page<Assignment> result = switch (progress) {
            case TODO -> assignmentRepository.findStudentTodo(studentId, AssignmentStatus.PUBLISHED,
                    List.of(SubmissionStatus.SUBMITTED, SubmissionStatus.GRADED), pageable);
            case SUBMITTED -> assignmentRepository.findStudentBySubmissionStatus(studentId, SubmissionStatus.SUBMITTED, pageable);
            case GRADED -> assignmentRepository.findStudentBySubmissionStatus(studentId, SubmissionStatus.GRADED, pageable);
        };
        return result.map(views::assignment);
    }

    @Transactional(readOnly = true)
    public StudentAssignmentDetail getDetail(UUID studentId, UUID assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .filter(a -> a.getStudentId().equals(studentId) && a.getStatus() != AssignmentStatus.DRAFT)
                .orElseThrow(() -> new BusinessException(ErrorCode.ASSIGNMENT_NOT_FOUND));
        SubmissionDetail submission = submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId)
                .map(views::submission).orElse(null);
        List<com.edtech.platform.common.dto.response.AttachmentView> assignmentFiles = attachmentViews(views.assignment(assignment).getContentBlocks());
        List<com.edtech.platform.common.dto.response.AttachmentView> submissionFiles = submission == null ? List.of() : attachmentViews(submission.getContentBlocks());
        return new StudentAssignmentDetail(views.assignment(assignment), submission, assignmentFiles, submissionFiles);
    }

    private List<com.edtech.platform.common.dto.response.AttachmentView> attachmentViews(List<com.edtech.platform.learning.dto.response.ContentBlock> blocks) {
        if (blocks == null) return List.of();
        return blocks.stream().map(com.edtech.platform.learning.dto.response.ContentBlock::getAttachmentId)
                .filter(java.util.Objects::nonNull).distinct().map(attachments::getAttachmentView).toList();
    }

    @Transactional
    public SubmissionDetail createOrUpdateSubmission(UUID studentId, UUID assignmentId, CreateSubmissionRequest request) {
        if (request.getStatus() != SubmissionStatus.DRAFT && request.getStatus() != SubmissionStatus.SUBMITTED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Trạng thái bài nộp không hợp lệ");
        }
        Assignment assignment = assignmentRepository.findByIdForUpdate(assignmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ASSIGNMENT_NOT_FOUND));

        if (!assignment.getStudentId().equals(studentId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_RESOURCE);
        }

        if (assignment.getStatus() != AssignmentStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.ASSIGNMENT_INVALID_STATE);
        }

        if (assignment.getDueAt() != null && clock.instant().isAfter(assignment.getDueAt())) {
            throw new BusinessException(ErrorCode.ASSIGNMENT_DUE_DATE_PASSED);
        }

        if (!identityFacade.existsById(studentId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        Optional<Submission> existingSubmission = submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId);
        Submission submission;

        if (existingSubmission.isPresent()) {
            submission = existingSubmission.get();
            if (request.getVersion() == null || submission.getVersion() != request.getVersion())
                throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
            if (submission.getStatus() == SubmissionStatus.GRADED) {
                throw new BusinessException(ErrorCode.SUBMISSION_ALREADY_GRADED);
            }
            submission.setContentBlocks(objectMapper.valueToTree(request.getContentBlocks()));
            submission.setStatus(request.getStatus());
            if (request.getStatus() == SubmissionStatus.SUBMITTED) {
                submission.setSubmittedAt(clock.instant());
            } else {
                submission.setSubmittedAt(null);
            }
        } else {
            if (request.getVersion() == null || request.getVersion() != 0L)
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Create version must be 0");
            submission = Submission.builder()
                    .assignment(assignment)
                    .studentId(studentId)
                    .contentBlocks(objectMapper.valueToTree(request.getContentBlocks()))
                    .status(request.getStatus())
                    .submittedAt(request.getStatus() == SubmissionStatus.SUBMITTED ? clock.instant() : null)
                    .build();
        }

        submission = submissionRepository.save(submission);

        if (request.getContentBlocks() != null) {
            attachmentBinder.bind(studentId, "SUBMISSION", request.getContentBlocks(), submission.getId());
        }

        return views.submission(submission);
    }

}
