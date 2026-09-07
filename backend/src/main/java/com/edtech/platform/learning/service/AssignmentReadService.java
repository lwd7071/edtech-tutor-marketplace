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
        return assignments.findByTeacherId(teacherId,pageable).map(this::view);
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
        var result = new LinkedHashMap<String,Object>();
        result.put("id",a.getId()); result.put("teacherId",a.getTeacherId()); result.put("studentId",a.getStudentId());
        result.put("subjectId",a.getSubjectId()); result.put("title",a.getTitle()); result.put("assignmentType",a.getAssignmentType());
        result.put("contentBlocks",a.getContentBlocks()); result.put("quizSchema",a.getQuizSchema());
        result.put("dueAt",a.getDueAt()); result.put("status",a.getStatus());
        result.put("studentName",identities.getIdentity(a.getStudentId()).map(i -> i.fullName()).orElse("Học viên"));
        result.put("subjectName",subjects.getSubject(a.getSubjectId()).name());
        var submitted = submissions.findByAssignmentIdAndStudentId(a.getId(),a.getStudentId());
        result.put("submissions",submitted.map(s -> List.of(submissionView(s))).orElse(List.of()));
        result.put("submittedCount",submitted.filter(s -> s.getStatus()!=com.edtech.platform.learning.domain.SubmissionStatus.DRAFT).isPresent()?1:0);
        result.put("totalStudents",1);
        return result;
    }

    private SubmissionDetail submissionView(Submission s) {
        List<ContentBlock> blocks = s.getContentBlocks()==null?List.of():mapper.convertValue(s.getContentBlocks(),new TypeReference<List<ContentBlock>>(){});
        return new SubmissionDetail(s.getId(),s.getAssignment().getId(),s.getStudentId(),blocks,s.getSubmittedAt(),s.getStatus(),s.getScore(),s.getFeedbackText(),s.getGradedAt());
    }
}

