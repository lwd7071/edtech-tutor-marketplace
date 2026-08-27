package com.edtech.platform.teacher.facade.impl;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;

import com.edtech.platform.teacher.domain.ProfileStatus;
import com.edtech.platform.teacher.domain.TeacherProfile;
import com.edtech.platform.teacher.domain.TeacherSubject;
import com.edtech.platform.teacher.facade.dto.TeacherSnapshot;
import com.edtech.platform.teacher.repository.TeacherProfileRepository;
import com.edtech.platform.teacher.repository.TeacherSubjectRepository;
import com.edtech.platform.teacher.repository.TeacherAvailabilityRepository;
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
public class TeacherFacadeImplTest {

    @Mock
    private TeacherProfileRepository teacherProfileRepository;

    @Mock
    private TeacherSubjectRepository teacherSubjectRepository;

    @Mock
    private TeacherAvailabilityRepository teacherAvailabilityRepository;

    @InjectMocks
    private TeacherFacadeImpl teacherFacade;

    private UUID testTeacherId;
    private UUID testUserId;
    private UUID testSubjectId;
    private TeacherProfile testProfile;

    @Mock
    private com.edtech.platform.auth.facade.IdentityFacade identityFacade;

    @BeforeEach
    void setUp() {
        testTeacherId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testSubjectId = UUID.randomUUID();

        testProfile = TeacherProfile.builder()
                .userId(testUserId)
                .build();
        ReflectionTestUtils.setField(testProfile, "id", testTeacherId);
        ReflectionTestUtils.setField(testProfile, "profileStatus", ProfileStatus.APPROVED);
        ReflectionTestUtils.setField(testProfile, "isVisible", true);
        
        lenient().when(identityFacade.getIdentity(any())).thenReturn(
                java.util.Optional.of(new com.edtech.platform.auth.facade.dto.IdentitySnapshot(testUserId, "test@test.com", "Teacher Name", "TEACHER", "ACTIVE", null, false, null))
        );
    }

    @Test
    void getTeacher_Success() {
        when(teacherProfileRepository.findById(testTeacherId)).thenReturn(Optional.of(testProfile));

        TeacherSnapshot snapshot = teacherFacade.getTeacher(testTeacherId);

        assertNotNull(snapshot);
        assertEquals(testTeacherId, snapshot.id());
        assertEquals(testUserId, snapshot.userId());
        assertEquals("APPROVED", snapshot.status());
        assertTrue(snapshot.isVerified());
        assertTrue(snapshot.isVisible());
    }

    @Test
    void getTeacher_ThrowsNotFound() {
        when(teacherProfileRepository.findById(testTeacherId)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            teacherFacade.getTeacher(testTeacherId);
        });

        assertEquals(ErrorCode.TEACHER_PROFILE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void getTeacherByUserId_Success() {
        when(teacherProfileRepository.findByUserId(testUserId)).thenReturn(Optional.of(testProfile));

        TeacherSnapshot snapshot = teacherFacade.getTeacherByUserId(testUserId);

        assertNotNull(snapshot);
        assertEquals(testTeacherId, snapshot.id());
    }

    @Test
    void getTeacherByUserId_ThrowsNotFound() {
        when(teacherProfileRepository.findByUserId(testUserId)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> {
            teacherFacade.getTeacherByUserId(testUserId);
        });
    }

    @Test
    void hasAssignedSubject_True() {
        TeacherSubject ts = mock(TeacherSubject.class);
        when(ts.isActive()).thenReturn(true);
        when(ts.getSubjectId()).thenReturn(testSubjectId);

        when(teacherSubjectRepository.findByTeacherId(testTeacherId)).thenReturn(List.of(ts));

        assertTrue(teacherFacade.hasAssignedSubject(testTeacherId, testSubjectId));
    }
    
    @Test
    void hasAssignedSubject_FalseWhenInactive() {
        TeacherSubject ts = mock(TeacherSubject.class);
        when(ts.isActive()).thenReturn(false);
        when(ts.getSubjectId()).thenReturn(testSubjectId);

        when(teacherSubjectRepository.findByTeacherId(testTeacherId)).thenReturn(List.of(ts));

        assertFalse(teacherFacade.hasAssignedSubject(testTeacherId, testSubjectId));
    }
}
