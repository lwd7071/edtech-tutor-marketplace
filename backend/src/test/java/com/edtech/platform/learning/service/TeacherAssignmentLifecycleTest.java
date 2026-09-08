package com.edtech.platform.learning.service;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.learning.domain.*;
import com.edtech.platform.learning.dto.request.GradeSubmissionRequest;
import com.edtech.platform.learning.repository.AssignmentRepository;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.teacher.facade.dto.TeacherSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TeacherAssignmentLifecycleTest {
    @Test void rejectsScoresOutsideTenPointScaleBeforeAccessingData() {
        var service = new TeacherAssignmentService(null,null,null,null,null,null,null,null,null,null);
        for (String score : new String[]{"-0.1", "10.1"}) {
            var request = new GradeSubmissionRequest(); request.setScore(new BigDecimal(score));
            assertThrows(BusinessException.class, () -> service.gradeSubmission(UUID.randomUUID(), UUID.randomUUID(), request));
        }
    }
    @Test void draftCanPublishThenCloseButCannotReopen() {
        var repository = mock(AssignmentRepository.class);
        var teachers = mock(TeacherFacade.class);
        UUID user = UUID.randomUUID(), profile = UUID.randomUUID(), id = UUID.randomUUID();
        var teacher = mock(TeacherSnapshot.class);
        when(teacher.id()).thenReturn(profile);
        when(teachers.getTeacherByUserId(user)).thenReturn(teacher);
        var assignment = Assignment.builder().teacherId(profile).assignmentType(AssignmentType.FREEFORM)
                .status(AssignmentStatus.DRAFT).dueAt(Instant.now().plusSeconds(3600)).build();
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(assignment));
        when(repository.save(assignment)).thenReturn(assignment);
        var objectMapper = new ObjectMapper();
        var service = new TeacherAssignmentService(repository,null,teachers,null,null,null,objectMapper,null,
                new AssignmentViewMapper(objectMapper), Clock.systemUTC());
        service.transition(user,id,AssignmentStatus.PUBLISHED);
        assertEquals(AssignmentStatus.PUBLISHED, assignment.getStatus());
        service.transition(user,id,AssignmentStatus.CLOSED);
        assertEquals(AssignmentStatus.CLOSED, assignment.getStatus());
        assertThrows(BusinessException.class, () -> service.transition(user,id,AssignmentStatus.PUBLISHED));
    }
    @Test void anotherTeacherCannotPublishDraft() {
        var repository = mock(AssignmentRepository.class);
        var teachers = mock(TeacherFacade.class);
        UUID user = UUID.randomUUID(), id = UUID.randomUUID();
        var teacher = mock(TeacherSnapshot.class);
        when(teacher.id()).thenReturn(UUID.randomUUID());
        when(teachers.getTeacherByUserId(user)).thenReturn(teacher);
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(Assignment.builder().teacherId(UUID.randomUUID()).status(AssignmentStatus.DRAFT).build()));
        var objectMapper = new ObjectMapper();
        var service = new TeacherAssignmentService(repository,null,teachers,null,null,null,objectMapper,null,
                new AssignmentViewMapper(objectMapper), Clock.systemUTC());
        assertThrows(BusinessException.class, () -> service.transition(user,id,AssignmentStatus.PUBLISHED));
        verify(repository,never()).save(any());
    }
}
