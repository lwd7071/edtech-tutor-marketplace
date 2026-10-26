package com.edtech.platform.subject.service;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.subject.domain.EducationLevel;
import com.edtech.platform.subject.domain.Subject;
import com.edtech.platform.subject.repository.SubjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubjectServiceTest {
    @Mock private SubjectRepository subjectRepository;
    @InjectMocks private SubjectService subjectService;

    @Test
    void getPublicSubjectReturnsActiveSubjectSummary() {
        UUID id = UUID.randomUUID();
        Subject subject = Subject.builder()
                .code("MATH10")
                .name("Toán 10")
                .slug("toan-10")
                .educationLevel(EducationLevel.HIGH_SCHOOL)
                .description("Toán lớp 10")
                .build();
        ReflectionTestUtils.setField(subject, "id", id);
        when(subjectRepository.findByIdAndIsActiveTrue(id)).thenReturn(Optional.of(subject));

        var result = subjectService.getPublicSubject(id);

        assertEquals(id, result.id());
        assertEquals("Toán 10", result.name());
        assertEquals(EducationLevel.HIGH_SCHOOL, result.educationLevel());
    }

    @Test
    void getPublicSubjectHidesMissingOrInactiveSubject() {
        UUID id = UUID.randomUUID();
        when(subjectRepository.findByIdAndIsActiveTrue(id)).thenReturn(Optional.empty());

        BusinessException error = assertThrows(BusinessException.class, () -> subjectService.getPublicSubject(id));

        assertEquals(ErrorCode.SUBJECT_NOT_FOUND, error.getErrorCode());
    }
}
