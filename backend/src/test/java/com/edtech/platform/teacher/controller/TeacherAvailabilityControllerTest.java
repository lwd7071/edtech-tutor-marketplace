package com.edtech.platform.teacher.controller;

import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.teacher.dto.AvailabilityItem;
import com.edtech.platform.teacher.dto.AvailabilityView;
import com.edtech.platform.teacher.dto.ReplaceAvailabilityRequest;
import com.edtech.platform.teacher.service.TeacherAvailabilityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TeacherAvailabilityControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private TeacherAvailabilityService teacherAvailabilityService;

    @InjectMocks
    private TeacherAvailabilityController controller;

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
    void getAvailabilities_shouldReturn200() throws Exception {
        AvailabilityView view = new AvailabilityView(
                UUID.randomUUID(), DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0), "Asia/Ho_Chi_Minh", true
        );
        when(teacherAvailabilityService.getAvailabilities(teacherUserId)).thenReturn(List.of(view));

        mockMvc.perform(get("/api/teacher/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].dayOfWeek").value("MONDAY"));

        verify(teacherAvailabilityService).getAvailabilities(teacherUserId);
    }

    @Test
    void replaceAvailabilities_shouldReturn200() throws Exception {
        ReplaceAvailabilityRequest request = new ReplaceAvailabilityRequest(
                List.of(new AvailabilityItem(
                        DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0), "Asia/Ho_Chi_Minh", true
                ))
        );
        AvailabilityView view = new AvailabilityView(
                UUID.randomUUID(), DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0), "Asia/Ho_Chi_Minh", true
        );
        when(teacherAvailabilityService.replaceAvailabilities(eq(teacherUserId), any())).thenReturn(List.of(view));

        mockMvc.perform(put("/api/teacher/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].dayOfWeek").value("MONDAY"));

        verify(teacherAvailabilityService).replaceAvailabilities(eq(teacherUserId), any());
    }
}
