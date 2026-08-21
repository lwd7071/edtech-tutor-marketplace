package com.edtech.platform.learning.service;

import com.edtech.platform.auth.domain.User;
import com.edtech.platform.auth.repository.UserRepository;
import com.edtech.platform.common.domain.AttachableType;
import com.edtech.platform.common.domain.Attachment;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.repository.AttachmentRepository;
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
    private final UserRepository userRepository;
    private final AttachmentRepository attachmentRepository;
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

        if (!assignment.getStudent().getId().equals(studentId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_RESOURCE);
        }

        if (assignment.getStatus() != AssignmentStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.ASSIGNMENT_INVALID_STATE);
        }

        if (assignment.getDueAt() != null && Instant.now().isAfter(assignment.getDueAt())) {
            throw new BusinessException(ErrorCode.ASSIGNMENT_DUE_DATE_PASSED);
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

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
                    .student(student)
                    .contentBlocks(objectMapper.valueToTree(request.getContentBlocks()))
                    .status(request.getStatus())
                    .submittedAt(request.getStatus() == SubmissionStatus.SUBMITTED ? Instant.now() : null)
                    .build();
        }

        submission = submissionRepository.save(submission);

        if (request.getContentBlocks() != null) {
            validateAndLinkAttachments(studentId, AttachableType.SUBMISSION, request.getContentBlocks(), submission.getId());
        }

        return toSubmissionDetail(submission);
    }

    private void validateAndLinkAttachments(UUID currentUserId, AttachableType attachableType, List<ContentBlock> contentBlocks, UUID attachableId) {
        for (ContentBlock block : contentBlocks) {
            if ("IMAGE".equals(block.getType()) || "FILE".equals(block.getType())) {
                if (block.getAttachmentId() == null) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Missing attachmentId for IMAGE/FILE block");
                }

                Attachment attachment = attachmentRepository.findById(block.getAttachmentId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.ATTACHMENT_CONTEXT_INVALID, "Attachment not found"));

                if (!attachment.getOwner().getId().equals(currentUserId)) {
                    throw new BusinessException(ErrorCode.ATTACHMENT_CONTEXT_INVALID, "Attachment not owned by current user");
                }

                if (attachment.getAttachableType() != attachableType) {
                    throw new BusinessException(ErrorCode.ATTACHMENT_CONTEXT_INVALID, "Attachment type mismatch");
                }
                
                // If the attachment is already linked to this very entity, it's fine (overwrite update case)
                if (attachment.getAttachableId().equals(attachableId)) {
                    continue; 
                }

                if (!attachment.getAttachableId().equals(attachment.getId())) {
                    throw new BusinessException(ErrorCode.ATTACHMENT_CONTEXT_INVALID, "Attachment is already linked");
                }

                attachment.setAttachableId(attachableId);
                attachmentRepository.save(attachment);
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
                assignment.getTeacher().getId(),
                assignment.getStudent().getId(),
                assignment.getSubject().getId(),
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
                submission.getStudent().getId(),
                blocks,
                submission.getSubmittedAt(),
                submission.getStatus(),
                submission.getScore(),
                submission.getFeedbackText(),
                submission.getGradedAt()
        );
    }
}
