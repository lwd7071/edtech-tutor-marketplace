package com.edtech.platform.teacher.controller;

import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.teacher.dto.AssignSubjectRequest;
import com.edtech.platform.teacher.dto.TeacherSubjectView;
import com.edtech.platform.teacher.service.TeacherSubjectService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TeacherSubjectControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private TeacherSubjectService teacherSubjectService;

    @InjectMocks
    private TeacherSubjectController controller;

    private final UUID teacherUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return new AuthenticatedUser(teacherUserId, "teacher@example.com", "TEACHER");
                    }
                })
                .build();
    }

    @Test
    void getSubjects_shouldReturn200() throws Exception {
        TeacherSubjectView view = new TeacherSubjectView(
                UUID.randomUUID(),
                new TeacherSubjectView.SubjectDto(UUID.randomUUID(), "MATH", "Mathematics", "HIGH_SCHOOL"),
                "Grade 12", "5 years", true
        );
        when(teacherSubjectService.getSubjects(teacherUserId)).thenReturn(List.of(view));

        mockMvc.perform(get("/api/teacher/subjects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].subject.code").value("MATH"));

        verify(teacherSubjectService).getSubjects(teacherUserId);
    }

    @Test
    void assignSubject_shouldReturn201() throws Exception {
        UUID subjectId = UUID.randomUUID();
        AssignSubjectRequest request = new AssignSubjectRequest("Grade 12", "5 years");
        TeacherSubjectView view = new TeacherSubjectView(
                UUID.randomUUID(),
                new TeacherSubjectView.SubjectDto(subjectId, "MATH", "Mathematics", "HIGH_SCHOOL"),
                "Grade 12", "5 years", true
        );
        when(teacherSubjectService.assignSubject(eq(teacherUserId), eq(subjectId), any())).thenReturn(view);

        mockMvc.perform(post("/api/teacher/subjects/{subjectId}", subjectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.subject.code").value("MATH"));

        verify(teacherSubjectService).assignSubject(eq(teacherUserId), eq(subjectId), any());
    }

    @Test
    void unassignSubject_shouldReturn204() throws Exception {
        UUID subjectId = UUID.randomUUID();

        mockMvc.perform(delete("/api/teacher/subjects/{subjectId}", subjectId))
                .andExpect(status().isNoContent());

        verify(teacherSubjectService).unassignSubject(teacherUserId, subjectId);
    }
}
