package com.edtech.platform.learning.service;

import com.edtech.platform.auth.facade.IdentityFacade;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.facade.AttachmentFacade;
import com.edtech.platform.learning.domain.Assignment;
import com.edtech.platform.learning.domain.AssignmentStatus;
import com.edtech.platform.learning.domain.Submission;
import com.edtech.platform.learning.domain.SubmissionStatus;
import com.edtech.platform.learning.dto.request.CreateSubmissionRequest;
import com.edtech.platform.learning.dto.response.AssignmentDetail;
import com.edtech.platform.learning.dto.response.ContentBlock;
import com.edtech.platform.learning.dto.response.SubmissionDetail;
import com.edtech.platform.learning.repository.AssignmentRepository;
import com.edtech.platform.learning.repository.SubmissionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
    private final AttachmentFacade attachmentFacade;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<AssignmentDetail> getAssignments(UUID studentId, AssignmentStatus status, Pageable pageable) {
        Page<Assignment> assignments;
        if (status != null) {
            assignments = assignmentRepository.findByStudentIdAndStatus(studentId, status, pageable);
        } else {
            assignments = assignmentRepository.findByStudentId(studentId, pageable);
        }
        return assignments.map(this::toAssignmentDetail);
    }

    @Transactional
    public SubmissionDetail createOrUpdateSubmission(UUID studentId, UUID assignmentId, CreateSubmissionRequest request) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ASSIGNMENT_NOT_FOUND));

        if (!assignment.getStudentId().equals(studentId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_RESOURCE);
        }

        if (assignment.getStatus() != AssignmentStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.ASSIGNMENT_INVALID_STATE);
        }

        if (assignment.getDueAt() != null && Instant.now().isAfter(assignment.getDueAt())) {
            throw new BusinessException(ErrorCode.ASSIGNMENT_DUE_DATE_PASSED);
        }

        if (!identityFacade.existsById(studentId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        Optional<Submission> existingSubmission = submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId);
        Submission submission;

        if (existingSubmission.isPresent()) {
            submission = existingSubmission.get();
            if (submission.getStatus() == SubmissionStatus.GRADED) {
                throw new BusinessException(ErrorCode.SUBMISSION_ALREADY_GRADED);
            }
            submission.setContentBlocks(objectMapper.valueToTree(request.getContentBlocks()));
            submission.setStatus(request.getStatus());
            if (request.getStatus() == SubmissionStatus.SUBMITTED) {
                submission.setSubmittedAt(Instant.now());
            }
        } else {
            submission = Submission.builder()
                    .assignment(assignment)
                    .studentId(studentId)
                    .contentBlocks(objectMapper.valueToTree(request.getContentBlocks()))
                    .status(request.getStatus())
                    .submittedAt(request.getStatus() == SubmissionStatus.SUBMITTED ? Instant.now() : null)
                    .build();
        }

        submission = submissionRepository.save(submission);

        if (request.getContentBlocks() != null) {
            validateAndLinkAttachments(studentId, "SUBMISSION", request.getContentBlocks(), submission.getId());
        }

        return toSubmissionDetail(submission);
    }

    private void validateAndLinkAttachments(UUID currentUserId, String attachableType, List<ContentBlock> contentBlocks, UUID attachableId) {
        for (ContentBlock block : contentBlocks) {
            if ("IMAGE".equals(block.getType()) || "FILE".equals(block.getType())) {
                if (block.getAttachmentId() == null) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Missing attachmentId for IMAGE/FILE block");
                }

                attachmentFacade.validateAndBind(block.getAttachmentId(), currentUserId, attachableType, attachableId);
            }
        }
    }

    private AssignmentDetail toAssignmentDetail(Assignment assignment) {
        List<ContentBlock> blocks = null;
        if (assignment.getContentBlocks() != null) {
            blocks = objectMapper.convertValue(assignment.getContentBlocks(), new TypeReference<>() {});
        }
        return new AssignmentDetail(
                assignment.getId(),
                assignment.getTeacherId(),
                assignment.getStudentId(),
                assignment.getSubjectId(),
                assignment.getTitle(),
                assignment.getAssignmentType(),
                blocks,
                assignment.getQuizSchema(),
                assignment.getDueAt(),
                assignment.getStatus()
        );
    }

    private SubmissionDetail toSubmissionDetail(Submission submission) {
        List<ContentBlock> blocks = null;
        if (submission.getContentBlocks() != null) {
            blocks = objectMapper.convertValue(submission.getContentBlocks(), new TypeReference<>() {});
        }
        return new SubmissionDetail(
                submission.getId(),
                submission.getAssignment().getId(),
                submission.getStudentId(),
                blocks,
                submission.getSubmittedAt(),
                submission.getStatus(),
                submission.getScore(),
                submission.getFeedbackText(),
                submission.getGradedAt()
        );
    }
}
