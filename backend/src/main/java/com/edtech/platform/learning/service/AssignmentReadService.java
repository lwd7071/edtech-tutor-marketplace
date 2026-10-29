package com.edtech.platform.learning.service;

import com.edtech.platform.learning.repository.AssignmentRepository;
import com.edtech.platform.learning.repository.SubmissionRepository;
import com.edtech.platform.learning.domain.Assignment;
import com.edtech.platform.learning.domain.Submission;
import com.edtech.platform.auth.facade.IdentityFacade;
import com.edtech.platform.subject.facade.SubjectFacade;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.learning.dto.response.SubmissionDetail;
import com.edtech.platform.learning.dto.response.ContentBlock;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AssignmentReadService {
    private final AssignmentRepository assignments;
    private final SubmissionRepository submissions;
    private final TeacherFacade teachers;
    private final IdentityFacade identities;
    private final SubjectFacade subjects;
    private final ObjectMapper mapper;

    @Transactional(readOnly=true)
    public Page<Map<String,Object>> teacherList(UUID userId, Pageable pageable) {
        UUID teacherId = teacherId(userId);
        Page<Assignment> page = assignments.findByTeacherId(teacherId, pageable);
        var rows = page.getContent();
        if (rows.isEmpty()) return page.map(this::view);
        var studentNames = identities.getIdentities(rows.stream().map(Assignment::getStudentId).distinct().toList());
        var subjectNames = subjects.getSubjects(rows.stream().map(Assignment::getSubjectId).distinct().toList());
        var submissionsByAssignment = submissions.findByAssignmentIdIn(rows.stream().map(Assignment::getId).toList()).stream()
                .collect(java.util.stream.Collectors.toMap(s -> s.getAssignment().getId(), this::submissionView, (first, ignored) -> first));
        return page.map(assignment -> view(assignment, studentNames, subjectNames,
                submissionsByAssignment.get(assignment.getId())));
    }

    @Transactional(readOnly=true)
    public Map<String,Object> detail(UUID userId, UUID id, boolean teacherRole) {
        Assignment a = assignments.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.ASSIGNMENT_NOT_FOUND));
        if (teacherRole ? !a.getTeacherId().equals(teacherId(userId)) : !a.getStudentId().equals(userId)
                || a.getStatus() == com.edtech.platform.learning.domain.AssignmentStatus.DRAFT)
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        return view(a);
    }

    @Transactional(readOnly=true)
    public SubmissionDetail submission(UUID userId, UUID id) {
        Submission s = submissions.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.SUBMISSION_NOT_FOUND));
        if (!s.getAssignment().getTeacherId().equals(teacherId(userId)))
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        return submissionView(s);
    }

    private UUID teacherId(UUID userId) {
        var teacher = teachers.getTeacherByUserId(userId);
        if (teacher == null) throw new BusinessException(ErrorCode.TEACHER_PROFILE_NOT_FOUND);
        return teacher.id();
    }

    private Map<String,Object> view(Assignment a) {
        return view(a, identities.getIdentities(List.of(a.getStudentId())), subjects.getSubjects(List.of(a.getSubjectId())),
                submissions.findByAssignmentIdAndStudentId(a.getId(), a.getStudentId()).map(this::submissionView).orElse(null));
    }

    private Map<String,Object> view(Assignment a,
                                   Map<UUID, com.edtech.platform.auth.facade.dto.IdentitySnapshot> studentNames,
                                   Map<UUID, com.edtech.platform.subject.facade.dto.SubjectSnapshot> subjectNames,
                                   SubmissionDetail submitted) {
        var result = new LinkedHashMap<String,Object>();
        result.put("id",a.getId()); result.put("teacherId",a.getTeacherId()); result.put("studentId",a.getStudentId());
        result.put("subjectId",a.getSubjectId()); result.put("title",a.getTitle()); result.put("assignmentType",a.getAssignmentType());
        result.put("contentBlocks",a.getContentBlocks()); result.put("quizSchema",a.getQuizSchema());
        result.put("dueAt",a.getDueAt()); result.put("status",a.getStatus());
        result.put("version", a.getVersion());
        var identity = studentNames.get(a.getStudentId());
        var subject = subjectNames.get(a.getSubjectId());
        result.put("studentName", identity == null ? "Học viên" : identity.fullName());
        result.put("subjectName", subject == null ? "Môn học" : subject.name());
        result.put("submissions", submitted == null ? List.of() : List.of(submitted));
        result.put("submittedCount", submitted != null && submitted.getStatus()!=com.edtech.platform.learning.domain.SubmissionStatus.DRAFT ? 1 : 0);
        result.put("totalStudents",1);
        return result;
    }

    private SubmissionDetail submissionView(Submission s) {
        List<ContentBlock> blocks = s.getContentBlocks()==null?List.of():mapper.convertValue(s.getContentBlocks(),new TypeReference<List<ContentBlock>>(){});
        return new SubmissionDetail(s.getId(),s.getAssignment().getId(),s.getStudentId(),blocks,s.getSubmittedAt(),s.getStatus(),s.getScore(),s.getFeedbackText(),s.getGradedAt(),s.getVersion());
    }
}

