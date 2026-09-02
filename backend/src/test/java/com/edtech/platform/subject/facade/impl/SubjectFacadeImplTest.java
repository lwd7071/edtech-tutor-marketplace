package com.edtech.platform.subject.facade.impl;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.subject.domain.EducationLevel;
import com.edtech.platform.subject.domain.Subject;
import com.edtech.platform.subject.facade.dto.SubjectSnapshot;
import com.edtech.platform.subject.repository.SubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SubjectFacadeImplTest {

    @Mock
    private SubjectRepository subjectRepository;

    @InjectMocks
    private SubjectFacadeImpl subjectFacade;

    private UUID testSubjectId;
    private Subject testSubject;

    @BeforeEach
    void setUp() {
        testSubjectId = UUID.randomUUID();
        testSubject = Subject.builder()
                .code("MATH101")
                .name("Toán cơ bản")
                .educationLevel(EducationLevel.HIGH_SCHOOL)
                .build();
        ReflectionTestUtils.setField(testSubject, "id", testSubjectId);
        testSubject.setActive(true);
    }

    @Test
    void getSubject_Success() {
        when(subjectRepository.findById(testSubjectId)).thenReturn(Optional.of(testSubject));

        SubjectSnapshot snapshot = subjectFacade.getSubject(testSubjectId);

        assertNotNull(snapshot);
        assertEquals(testSubjectId, snapshot.id());
        assertEquals("MATH101", snapshot.code());
        assertEquals("Toán cơ bản", snapshot.name());
        assertEquals("HIGH_SCHOOL", snapshot.educationLevel());
        assertTrue(snapshot.isActive());
    }

    @Test
    void getSubject_ThrowsNotFound() {
        when(subjectRepository.findById(testSubjectId)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            subjectFacade.getSubject(testSubjectId);
        });
        assertEquals(ErrorCode.SUBJECT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void isSubjectActive_True() {
        when(subjectRepository.findById(testSubjectId)).thenReturn(Optional.of(testSubject));

        assertTrue(subjectFacade.isSubjectActive(testSubjectId));
    }

    @Test
    void isSubjectActive_False_WhenInactive() {
        testSubject.setActive(false);
        when(subjectRepository.findById(testSubjectId)).thenReturn(Optional.of(testSubject));

        assertFalse(subjectFacade.isSubjectActive(testSubjectId));
    }

    @Test
    void isSubjectActive_False_WhenNotFound() {
        when(subjectRepository.findById(testSubjectId)).thenReturn(Optional.empty());

        assertFalse(subjectFacade.isSubjectActive(testSubjectId));
    }
}
