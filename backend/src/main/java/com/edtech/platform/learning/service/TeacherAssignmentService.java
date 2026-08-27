package com.edtech.platform.learning.service;

import com.edtech.platform.auth.facade.IdentityFacade;
import com.edtech.platform.enrollment.facade.EnrollmentFacade;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.facade.AttachmentFacade;
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
import com.edtech.platform.subject.facade.SubjectFacade;
import com.edtech.platform.teacher.facade.TeacherFacade;
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
    private final TeacherFacade teacherFacade;
    private final IdentityFacade identityFacade;
    private final SubjectFacade subjectFacade;
    private final EnrollmentFacade enrollmentFacade;
    private final AttachmentFacade attachmentFacade;
    private final ObjectMapper objectMapper;

    @Transactional
    public AssignmentDetail createAssignment(UUID teacherUserId, CreateAssignmentRequest request) {
        var teacher = teacherFacade.getTeacherByUserId(teacherUserId);

        if (!identityFacade.existsById(request.getStudentId())) {
            throw new BusinessException(ErrorCode.STUDENT_NOT_FOUND);
        }

        var subject = subjectFacade.getSubject(request.getSubjectId());

        if (!enrollmentFacade.hasValidRelationship(teacher.id(), request.getStudentId())) {
            throw new BusinessException(ErrorCode.LEARNING_RELATIONSHIP_NOT_FOUND);
        }

        Assignment assignment = Assignment.builder()
                .teacherId(teacher.id())
                .studentId(request.getStudentId())
                .subjectId(subject.id())
                .title(request.getTitle())
                .assignmentType(request.getAssignmentType())
                .contentBlocks(objectMapper.valueToTree(request.getContentBlocks()))
                .quizSchema(request.getQuizSchema())
                .dueAt(request.getDueAt())
                .status(request.getStatus())
                .build();

        assignment = assignmentRepository.save(assignment);

        if (request.getContentBlocks() != null) {
            validateAndLinkAttachments(teacherUserId, "ASSIGNMENT", request.getContentBlocks(), assignment.getId());
        }

        return toAssignmentDetail(assignment);
    }

    @Transactional
    public SubmissionDetail gradeSubmission(UUID teacherUserId, UUID submissionId, GradeSubmissionRequest request) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBMISSION_NOT_FOUND));

        var teacher = teacherFacade.getTeacherByUserId(teacherUserId);
        if (!submission.getAssignment().getTeacherId().equals(teacher.id())) {
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

    private void validateAndLinkAttachments(UUID currentUserId, String attachableType, List<ContentBlock> contentBlocks, UUID attachableId) {
        for (ContentBlock block : contentBlocks) {
            if ("IMAGE".equals(block.getType()) || "FILE".equals(block.getType())) {
                if (block.getAttachmentId() == null) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Thiếu attachmentId cho khối nội dung IMAGE/FILE");
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
