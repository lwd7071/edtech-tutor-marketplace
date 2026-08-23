package com.edtech.platform.teacher.service;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.subject.domain.EducationLevel;
import com.edtech.platform.subject.facade.SubjectFacade;
import com.edtech.platform.subject.facade.dto.SubjectSnapshot;
import com.edtech.platform.teacher.domain.TeacherProfile;
import com.edtech.platform.teacher.domain.TeacherSubject;
import com.edtech.platform.teacher.dto.AssignSubjectRequest;
import com.edtech.platform.teacher.dto.TeacherSubjectView;
import com.edtech.platform.teacher.repository.TeacherProfileRepository;
import com.edtech.platform.teacher.repository.TeacherSubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TeacherSubjectServiceTest {

    @Mock
    private TeacherSubjectRepository teacherSubjectRepository;

    @Mock
    private TeacherProfileRepository teacherProfileRepository;

    @Mock
    private SubjectFacade subjectFacade;

    @InjectMocks
    private TeacherSubjectService teacherSubjectService;

    private UUID userId;
    private UUID teacherId;
    private UUID subjectId;
    private TeacherProfile profile;
    private SubjectSnapshot subjectSnapshot;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        teacherId = UUID.randomUUID();
        subjectId = UUID.randomUUID();
        profile = mock(TeacherProfile.class);
        when(profile.getId()).thenReturn(teacherId);

        subjectSnapshot = new SubjectSnapshot(subjectId, "MATH", "Mathematics", "HIGH_SCHOOL", true);
    }

    @Test
    void getSubjects_Success() {
        when(teacherProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        
        TeacherSubject ts = mock(TeacherSubject.class);
        when(ts.getSubjectId()).thenReturn(subjectId);
        when(ts.getId()).thenReturn(UUID.randomUUID());
        when(ts.isActive()).thenReturn(true);
        when(ts.getLevelDescription()).thenReturn("Advanced");
        when(ts.getExperienceDescription()).thenReturn("5 years");
        
        when(teacherSubjectRepository.findByTeacherId(teacherId)).thenReturn(List.of(ts));
        when(subjectFacade.getSubject(subjectId)).thenReturn(subjectSnapshot);

        List<TeacherSubjectView> result = teacherSubjectService.getSubjects(userId);

        assertEquals(1, result.size());
        assertEquals("Mathematics", result.get(0).subject().name());
    }

    @Test
    void assignSubject_Success_New() {
        when(teacherProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(teacherSubjectRepository.findByTeacherIdAndSubjectIdIncludingDeleted(teacherId, subjectId)).thenReturn(Optional.empty());
        when(subjectFacade.isSubjectActive(subjectId)).thenReturn(true);
        
        AssignSubjectRequest request = new AssignSubjectRequest("Advanced", "5 years");
        
        TeacherSubject savedTs = mock(TeacherSubject.class);
        when(savedTs.getSubjectId()).thenReturn(subjectId);
        when(savedTs.getId()).thenReturn(UUID.randomUUID());
        when(savedTs.isActive()).thenReturn(true);
        when(savedTs.getLevelDescription()).thenReturn("Advanced");
        when(savedTs.getExperienceDescription()).thenReturn("5 years");

        when(teacherSubjectRepository.save(any(TeacherSubject.class))).thenReturn(savedTs);
        when(subjectFacade.getSubject(subjectId)).thenReturn(subjectSnapshot);

        TeacherSubjectView result = teacherSubjectService.assignSubject(userId, subjectId, request);
        assertNotNull(result);
        assertEquals("Mathematics", result.subject().name());
    }
}
