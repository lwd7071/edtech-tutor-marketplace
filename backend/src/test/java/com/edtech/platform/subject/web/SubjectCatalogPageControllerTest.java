package com.edtech.platform.subject.web;

import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.common.security.HtmlSessionAuthenticationFilter;
import com.edtech.platform.subject.domain.EducationLevel;
import com.edtech.platform.subject.dto.SubjectSummary;
import com.edtech.platform.subject.service.SubjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(SubjectCatalogPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class SubjectCatalogPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private SubjectService subjectService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private UserStatusCacheService userStatusCacheService;
    @MockBean private HtmlSessionAuthenticationFilter htmlSessionAuthenticationFilter;

    @Test
    void rendersSubjectsAndLinkToFilteredTeacherSearch() throws Exception {
        UUID id = UUID.randomUUID();
        SubjectSummary subject = new SubjectSummary(id, "MATH", "Toán học", "toan-hoc",
                EducationLevel.HIGH_SCHOOL, "Môn học theo chương trình trung học phổ thông.");
        when(subjectService.getPublicSubjects(eq("toán"), eq(EducationLevel.HIGH_SCHOOL), any()))
                .thenReturn(new PageImpl<>(List.of(subject)));

        mockMvc.perform(get("/subjects").param("keyword", "toán").param("educationLevel", "HIGH_SCHOOL"))
                .andExpect(status().isOk())
                .andExpect(view().name("subjects/index"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Toán học")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Trung học phổ thông")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("subjectId=" + id)));
    }

    @Test
    void invalidFiltersShowValidationAndSkipServiceCall() throws Exception {
        mockMvc.perform(get("/subjects").param("keyword", "x".repeat(101)).param("educationLevel", "UNKNOWN"))
                .andExpect(status().isOk())
                .andExpect(view().name("subjects/index"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tên môn học không được vượt quá 100 ký tự")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Cấp học không hợp lệ")));

        verify(subjectService, never()).getPublicSubjects(any(), any(), any());
    }

    @Test
    void serviceFailureRendersRetryState() throws Exception {
        when(subjectService.getPublicSubjects(eq(""), isNull(), any())).thenThrow(new IllegalStateException("unavailable"));

        mockMvc.perform(get("/subjects"))
                .andExpect(status().isOk())
                .andExpect(view().name("subjects/index"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tải được danh sách môn học")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Thử lại")));
    }

    @Test
    void rendersEmptySearchStateWithoutInventingSubjects() throws Exception {
        when(subjectService.getPublicSubjects(eq("Sinh học"), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 12, Sort.by("name").ascending()), 0));

        mockMvc.perform(get("/subjects").param("keyword", "Sinh học"))
                .andExpect(status().isOk())
                .andExpect(view().name("subjects/index"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Không tìm thấy môn học phù hợp")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Toán học"))));
    }
}
