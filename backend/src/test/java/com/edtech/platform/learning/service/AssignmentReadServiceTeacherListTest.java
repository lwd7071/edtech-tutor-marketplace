package com.edtech.platform.learning.service;

import com.edtech.platform.auth.facade.IdentityFacade;
import com.edtech.platform.auth.facade.dto.IdentitySnapshot;
import com.edtech.platform.learning.domain.Assignment;
import com.edtech.platform.learning.domain.AssignmentStatus;
import com.edtech.platform.learning.domain.AssignmentType;
import com.edtech.platform.learning.domain.Submission;
import com.edtech.platform.learning.repository.AssignmentRepository;
import com.edtech.platform.learning.repository.SubmissionRepository;
import com.edtech.platform.subject.facade.SubjectFacade;
import com.edtech.platform.subject.facade.dto.SubjectSnapshot;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.teacher.facade.dto.TeacherSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssignmentReadServiceTeacherListTest {
    private final AssignmentRepository assignments = mock(AssignmentRepository.class);
    private final SubmissionRepository submissions = mock(SubmissionRepository.class);
    private final TeacherFacade teachers = mock(TeacherFacade.class);
    private final IdentityFacade identities = mock(IdentityFacade.class);
    private final SubjectFacade subjects = mock(SubjectFacade.class);
    private final AssignmentReadService service = new AssignmentReadService(assignments, submissions, teachers,
            identities, subjects, new ObjectMapper());

    @Test
    void teacherListBatchLoadsStudentSubjectAndSubmissionRows() {
        UUID userId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        when(teachers.getTeacherByUserId(userId)).thenReturn(new TeacherSnapshot(teacherId, userId, "APPROVED", true,
                true, "Teacher", null, null, 0, true, false, List.of(), null, null));
        Assignment first = assignment(studentId, subjectId, firstId, "Algebra");
        Assignment second = assignment(studentId, subjectId, secondId, "Geometry");
        var pageable = PageRequest.of(0, 20);
        when(assignments.findByTeacherId(teacherId, pageable)).thenReturn(new PageImpl<>(List.of(first, second), pageable, 2));
        when(identities.getIdentities(List.of(studentId))).thenReturn(Map.of(studentId,
                new IdentitySnapshot(studentId, "student@example.test", "Minh An", "STUDENT", "ACTIVE", null, false, null)));
        when(subjects.getSubjects(List.of(subjectId))).thenReturn(Map.of(subjectId,
                new SubjectSnapshot(subjectId, "ALG", "Đại số", "HIGH_SCHOOL", true)));
        when(submissions.findByAssignmentIdIn(List.of(firstId, secondId))).thenReturn(List.of());

        var result = service.teacherList(userId, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting(row -> row.get("studentName")).containsOnly("Minh An");
        assertThat(result.getContent()).extracting(row -> row.get("subjectName")).containsOnly("Đại số");
        assertThat(result.getContent()).extracting(row -> row.get("version")).containsOnly(0L);
        verify(identities).getIdentities(List.of(studentId));
        verify(subjects).getSubjects(List.of(subjectId));
        verify(submissions).findByAssignmentIdIn(List.of(firstId, secondId));
        verify(identities, never()).getIdentity(studentId);
        verify(subjects, never()).getSubject(subjectId);
    }

    private Assignment assignment(UUID studentId, UUID subjectId, UUID id, String title) {
        Assignment a = mock(Assignment.class);
        when(a.getId()).thenReturn(id);
        when(a.getStudentId()).thenReturn(studentId);
        when(a.getSubjectId()).thenReturn(subjectId);
        when(a.getTitle()).thenReturn(title);
        when(a.getAssignmentType()).thenReturn(AssignmentType.FREEFORM);
        when(a.getStatus()).thenReturn(AssignmentStatus.PUBLISHED);
        return a;
    }
}
