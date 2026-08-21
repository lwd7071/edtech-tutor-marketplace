package com.edtech.platform.learning.service;

import com.edtech.platform.auth.domain.User;
import com.edtech.platform.auth.repository.UserRepository;
import com.edtech.platform.catalog.service.LearningRelationshipChecker;
import com.edtech.platform.common.domain.AttachableType;
import com.edtech.platform.common.domain.Attachment;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.repository.AttachmentRepository;
import com.edtech.platform.learning.domain.Assignment;
import com.edtech.platform.learning.domain.Submission;
import com.edtech.platform.learning.domain.SubmissionStatus;
import com.edtech.platform.learning.dto.request.CreateAssignmentRequest;
import com.edtech.platform.learning.dto.request.GradeSubmissionRequest;
import com.edtech.platform.learning.dto.response.AssignmentDetail;
import com.edtech.platform.learning.dto.response.ContentBlock;
import com.edtech.platform.learning.dto.response.SubmissionDetail;
import com.edtech.platform.learning.repository.AssignmentRepository;
import com.edtech.platform.learning.repository.SubmissionRepository;
import com.edtech.platform.subject.domain.Subject;
import com.edtech.platform.subject.repository.SubjectRepository;
import com.edtech.platform.teacher.domain.TeacherProfile;
import com.edtech.platform.teacher.repository.TeacherProfileRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherAssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final LearningRelationshipChecker learningRelationshipChecker;
    private final AttachmentRepository attachmentRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public AssignmentDetail createAssignment(UUID teacherUserId, CreateAssignmentRequest request) {
        TeacherProfile teacher = teacherProfileRepository.findByUserId(teacherUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEACHER_PROFILE_NOT_FOUND));

        User student = userRepository.findById(request.getStudentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBJECT_NOT_FOUND));

        if (!learningRelationshipChecker.hasValidRelationship(teacher.getId(), student.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_RESOURCE, "No active learning relationship found.");
        }

        Assignment assignment = Assignment.builder()
                .teacher(teacher)
                .student(student)
                .subject(subject)
                .title(request.getTitle())
                .assignmentType(request.getAssignmentType())
                .contentBlocks(objectMapper.valueToTree(request.getContentBlocks()))
                .quizSchema(request.getQuizSchema())
                .dueAt(request.getDueAt())
                .status(request.getStatus())
                .build();

        assignment = assignmentRepository.save(assignment);

        if (request.getContentBlocks() != null) {
            validateAndLinkAttachments(teacherUserId, AttachableType.ASSIGNMENT, request.getContentBlocks(), assignment.getId());
        }

        return toAssignmentDetail(assignment);
    }

    @Transactional
    public SubmissionDetail gradeSubmission(UUID teacherUserId, UUID submissionId, GradeSubmissionRequest request) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBMISSION_NOT_FOUND));

        if (!submission.getAssignment().getTeacher().getUser().getId().equals(teacherUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_RESOURCE);
        }

        if (submission.getStatus() == SubmissionStatus.DRAFT) {
            throw new BusinessException(ErrorCode.SUBMISSION_NOT_SUBMITTED);
        }

        if (submission.getStatus() == SubmissionStatus.GRADED) {
            throw new BusinessException(ErrorCode.SUBMISSION_ALREADY_GRADED);
        }

        submission.setScore(request.getScore());
        submission.setFeedbackText(request.getFeedbackText());
        submission.setStatus(SubmissionStatus.GRADED);
        submission.setGradedAt(Instant.now());

        return toSubmissionDetail(submissionRepository.save(submission));
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

                // self reference check
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
