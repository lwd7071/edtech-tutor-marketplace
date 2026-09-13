package com.edtech.platform.learning.service;

import com.edtech.platform.auth.facade.IdentityFacade;
import com.edtech.platform.enrollment.facade.EnrollmentFacade;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.learning.domain.Assignment;
import com.edtech.platform.learning.domain.Submission;
import com.edtech.platform.learning.domain.SubmissionStatus;
import com.edtech.platform.learning.dto.request.CreateAssignmentRequest;
import com.edtech.platform.learning.dto.request.GradeSubmissionRequest;
import com.edtech.platform.learning.dto.response.AssignmentDetail;
import com.edtech.platform.learning.dto.response.SubmissionDetail;
import com.edtech.platform.learning.repository.AssignmentRepository;
import com.edtech.platform.learning.repository.SubmissionRepository;
import com.edtech.platform.subject.facade.SubjectFacade;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import com.edtech.platform.common.event.StudentLifecycleEvent;

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
    private final ObjectMapper objectMapper;
    private final AssignmentAttachmentBinder attachmentBinder;
    private final AssignmentViewMapper views;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    @Transactional
    public AssignmentDetail createAssignment(UUID teacherUserId, CreateAssignmentRequest request) {
        validateRequest(request);
        if (request.getVersion() == null || request.getVersion() != 0L) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Create version must be 0");
        var teacher = teacherFacade.getTeacherByUserId(teacherUserId);

        if (identityFacade.getIdentity(request.getStudentId()).filter(i -> "STUDENT".equals(i.roleName())).isEmpty()) {
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
            attachmentBinder.bind(teacherUserId, "ASSIGNMENT", request.getContentBlocks(), assignment.getId());
        }
        if (assignment.getStatus() == com.edtech.platform.learning.domain.AssignmentStatus.PUBLISHED) publishAssignment(assignment);

        return views.assignment(assignment);
    }

    @Transactional
    public SubmissionDetail gradeSubmission(UUID teacherUserId, UUID submissionId, GradeSubmissionRequest request) {
        if (request.getScore() == null || request.getScore().signum() < 0 || request.getScore().compareTo(java.math.BigDecimal.TEN) > 0)
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Điểm phải nằm trong khoảng 0 đến 10");
        UUID assignmentId = submissionRepository.findAssignmentId(submissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBMISSION_NOT_FOUND));

        var teacher = teacherFacade.getTeacherByUserId(teacherUserId);
        assignmentRepository.findByIdForUpdate(assignmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ASSIGNMENT_NOT_FOUND));
        Submission submission = submissionRepository.findById(submissionId).orElseThrow();
        if (!submission.getAssignment().getTeacherId().equals(teacher.id())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_RESOURCE);
        }
        requireVersion(submission.getVersion(), request.getVersion());

        if (submission.getStatus() == SubmissionStatus.DRAFT) {
            throw new BusinessException(ErrorCode.SUBMISSION_NOT_SUBMITTED);
        }

        if (submission.getStatus() == SubmissionStatus.GRADED) {
            throw new BusinessException(ErrorCode.SUBMISSION_ALREADY_GRADED);
        }

        submission.setScore(request.getScore());
        submission.setFeedbackText(request.getFeedbackText());
        submission.setStatus(SubmissionStatus.GRADED);
        submission.setGradedAt(clock.instant());

        Submission saved = submissionRepository.save(submission);
        events.publishEvent(new StudentLifecycleEvent(saved.getAssignment().getStudentId(), "SUBMISSION_GRADED",
                "Bài tập đã được chấm", saved.getAssignment().getTitle(), "ASSIGNMENT", saved.getAssignment().getId()));
        return views.submission(saved);
    }

    @Transactional
    public AssignmentDetail updateDraft(UUID userId, UUID id, CreateAssignmentRequest request) {
        Assignment assignment = ownedForUpdate(userId, id);
        if (assignment.getStatus() != com.edtech.platform.learning.domain.AssignmentStatus.DRAFT
                || request.getStatus() != com.edtech.platform.learning.domain.AssignmentStatus.DRAFT)
            throw new BusinessException(ErrorCode.ASSIGNMENT_INVALID_STATE);
        validateRequest(request);
        if (!assignment.getStudentId().equals(request.getStudentId()) || !assignment.getSubjectId().equals(request.getSubjectId()))
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Không thể đổi học viên hoặc môn học của bài tập");
        requireVersion(assignment.getVersion(), request.getVersion());
        assignment.setTitle(request.getTitle());
        assignment.setAssignmentType(request.getAssignmentType());
        assignment.setContentBlocks(objectMapper.valueToTree(request.getContentBlocks()));
        assignment.setQuizSchema(null);
        assignment.setDueAt(request.getDueAt());
        if (request.getContentBlocks() != null) attachmentBinder.bind(userId, "ASSIGNMENT", request.getContentBlocks(), id);
        return views.assignment(assignmentRepository.save(assignment));
    }

    @Transactional
    public AssignmentDetail transition(UUID userId, UUID id, com.edtech.platform.learning.domain.AssignmentStatus target, long requestedVersion) {
        Assignment assignment = ownedForUpdate(userId, id);
        requireVersion(assignment.getVersion(), requestedVersion);
        var source = assignment.getStatus();
        if (!(source == com.edtech.platform.learning.domain.AssignmentStatus.DRAFT && target == com.edtech.platform.learning.domain.AssignmentStatus.PUBLISHED)
                && !(source == com.edtech.platform.learning.domain.AssignmentStatus.PUBLISHED && target == com.edtech.platform.learning.domain.AssignmentStatus.CLOSED))
            throw new BusinessException(ErrorCode.ASSIGNMENT_INVALID_STATE);
        if (target == com.edtech.platform.learning.domain.AssignmentStatus.PUBLISHED) {
            if (assignment.getAssignmentType() != com.edtech.platform.learning.domain.AssignmentType.FREEFORM)
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "MVP chỉ hỗ trợ bài tập tự luận và file");
            if (assignment.getDueAt() != null && !assignment.getDueAt().isAfter(clock.instant()))
                throw new BusinessException(ErrorCode.ASSIGNMENT_DUE_DATE_PASSED);
        }
        assignment.setStatus(target);
        Assignment saved = assignmentRepository.save(assignment);
        if (target == com.edtech.platform.learning.domain.AssignmentStatus.PUBLISHED) publishAssignment(saved);
        return views.assignment(saved);
    }

    private void publishAssignment(Assignment assignment) {
        events.publishEvent(new StudentLifecycleEvent(assignment.getStudentId(), "ASSIGNMENT_PUBLISHED",
                "Bạn có bài tập mới", assignment.getTitle(), "ASSIGNMENT", assignment.getId()));
    }

    private Assignment ownedForUpdate(UUID userId, UUID id) {
        var teacher = teacherFacade.getTeacherByUserId(userId);
        Assignment assignment = assignmentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ASSIGNMENT_NOT_FOUND));
        if (!assignment.getTeacherId().equals(teacher.id())) throw new BusinessException(ErrorCode.ASSIGNMENT_NOT_FOUND);
        return assignment;
    }

    private void validateRequest(CreateAssignmentRequest request) {
        if (request.getAssignmentType() != com.edtech.platform.learning.domain.AssignmentType.FREEFORM || request.getQuizSchema() != null)
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "MVP chỉ hỗ trợ bài tập tự luận và file");
        if (request.getStatus() != com.edtech.platform.learning.domain.AssignmentStatus.DRAFT && request.getStatus() != com.edtech.platform.learning.domain.AssignmentStatus.PUBLISHED)
            throw new BusinessException(ErrorCode.ASSIGNMENT_INVALID_STATE);
        if (request.getDueAt() != null && !request.getDueAt().isAfter(clock.instant()))
            throw new BusinessException(ErrorCode.ASSIGNMENT_DUE_DATE_PASSED);
    }

    /** Compatibility overload for internal callers; HTTP controllers must provide version. */
    public AssignmentDetail transition(UUID userId, UUID id, com.edtech.platform.learning.domain.AssignmentStatus target) {
        return transition(userId, id, target, 0L);
    }

    private void requireVersion(long current, long requested) {
        if (current != requested) throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
    }

}
