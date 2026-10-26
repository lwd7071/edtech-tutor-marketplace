package com.edtech.platform.subject.controller;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.exception.GlobalExceptionHandler;
import com.edtech.platform.subject.domain.EducationLevel;
import com.edtech.platform.subject.dto.SubjectSummary;
import com.edtech.platform.subject.service.SubjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PublicSubjectControllerTest {
    @Mock private SubjectService subjectService;
    @InjectMocks private PublicSubjectController controller;

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getSubjectByIdReturnsPublicSummary() throws Exception {
        UUID id = UUID.randomUUID();
        when(subjectService.getPublicSubject(id)).thenReturn(new SubjectSummary(
                id, "MATH10", "Toán 10", "toan-10", EducationLevel.HIGH_SCHOOL, "Toán lớp 10"));

        mockMvc().perform(get("/api/public/subjects/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id.toString()))
                .andExpect(jsonPath("$.data.name").value("Toán 10"))
                .andExpect(jsonPath("$.data.educationLevel").value("HIGH_SCHOOL"));
    }

    @Test
    void getSubjectByIdReturnsNotFoundForUnavailableSubject() throws Exception {
        UUID id = UUID.randomUUID();
        when(subjectService.getPublicSubject(id)).thenThrow(new BusinessException(ErrorCode.SUBJECT_NOT_FOUND));

        mockMvc().perform(get("/api/public/subjects/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].code").value("SUBJECT_NOT_FOUND"));
    }
}
