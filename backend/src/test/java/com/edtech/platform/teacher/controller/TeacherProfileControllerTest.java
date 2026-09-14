package com.edtech.platform.teacher.controller;

import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.teacher.domain.ProfileStatus;
import com.edtech.platform.teacher.dto.TeacherProfileDetail;
import com.edtech.platform.teacher.dto.UpdateTeacherProfileRequest;
import com.edtech.platform.teacher.service.TeacherProfileService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TeacherProfileControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private TeacherProfileService teacherProfileService;

    @InjectMocks
    private TeacherProfileController controller;

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
    void getProfile_shouldReturn200() throws Exception {
        TeacherProfileDetail detail = new TeacherProfileDetail(
                UUID.randomUUID(), "Bio", 5, List.of("Vietnamese"), true, false, "HN", null,
                ProfileStatus.DRAFT, null, false, false
        );
        when(teacherProfileService.getProfile(teacherUserId)).thenReturn(detail);

        mockMvc.perform(get("/api/teacher/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bio").value("Bio"));

        verify(teacherProfileService).getProfile(teacherUserId);
    }

    @Test
    void updateProfile_shouldReturn200() throws Exception {
        UpdateTeacherProfileRequest request = new UpdateTeacherProfileRequest(
                "Updated Bio", 6, List.of("English"), true, true, "HCM", null
        );
        TeacherProfileDetail detail = new TeacherProfileDetail(
                UUID.randomUUID(), "Updated Bio", 6, List.of("English"), true, true, "HCM", null,
                ProfileStatus.DRAFT, null, false, false
        );
        when(teacherProfileService.updateProfile(eq(teacherUserId), any())).thenReturn(detail);

        mockMvc.perform(put("/api/teacher/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bio").value("Updated Bio"));

        verify(teacherProfileService).updateProfile(eq(teacherUserId), any());
    }

    @Test
    void submitProfile_shouldReturn200() throws Exception {
        TeacherProfileDetail detail = new TeacherProfileDetail(
                UUID.randomUUID(), "Bio", 5, List.of("Vietnamese"), true, false, "HN", null,
                ProfileStatus.PENDING_APPROVAL, null, false, false
        );
        when(teacherProfileService.submitProfile(teacherUserId)).thenReturn(detail);

        mockMvc.perform(post("/api/teacher/profile/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileStatus").value("PENDING_APPROVAL"));

        verify(teacherProfileService).submitProfile(teacherUserId);
    }
}
