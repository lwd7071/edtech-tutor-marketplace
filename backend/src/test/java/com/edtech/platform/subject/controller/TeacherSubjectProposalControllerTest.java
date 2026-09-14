package com.edtech.platform.subject.controller;

import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.subject.domain.EducationLevel;
import com.edtech.platform.subject.domain.ProposalStatus;
import com.edtech.platform.subject.dto.CreateSubjectProposalRequest;
import com.edtech.platform.subject.dto.SubjectProposalView;
import com.edtech.platform.subject.service.SubjectProposalService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TeacherSubjectProposalControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SubjectProposalService subjectProposalService;

    @InjectMocks
    private TeacherSubjectProposalController controller;

    private final UUID teacherUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        new HandlerMethodArgumentResolver() {
                            @Override
                            public boolean supportsParameter(MethodParameter parameter) {
                                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                            }

                            @Override
                            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                                return new AuthenticatedUser(teacherUserId, "teacher@example.com", "TEACHER");
                            }
                        }
                )
                .build();
    }

    @Test
    void createProposal_shouldReturn201() throws Exception {
        CreateSubjectProposalRequest request = new CreateSubjectProposalRequest(
                "AP Calculus", EducationLevel.HIGH_SCHOOL, "Advanced placement calculus"
        );
        SubjectProposalView view = new SubjectProposalView(
                UUID.randomUUID(), "AP Calculus", EducationLevel.HIGH_SCHOOL, "Advanced placement calculus",
                ProposalStatus.PENDING, null, null, null, 1L
        );
        when(subjectProposalService.createProposal(eq(teacherUserId), any())).thenReturn(view);

        mockMvc.perform(post("/api/teacher/subject-proposals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.proposedName").value("AP Calculus"));

        verify(subjectProposalService).createProposal(eq(teacherUserId), any());
    }

    @Test
    void getProposals_shouldReturn200() throws Exception {
        SubjectProposalView view = new SubjectProposalView(
                UUID.randomUUID(), "AP Calculus", EducationLevel.HIGH_SCHOOL, "Advanced placement calculus",
                ProposalStatus.PENDING, null, null, null, 1L
        );
        when(subjectProposalService.getProposals(eq(teacherUserId), (String) isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(view)));

        mockMvc.perform(get("/api/teacher/subject-proposals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].proposedName").value("AP Calculus"));

        verify(subjectProposalService).getProposals(eq(teacherUserId), (String) isNull(), any(Pageable.class));
    }
}
