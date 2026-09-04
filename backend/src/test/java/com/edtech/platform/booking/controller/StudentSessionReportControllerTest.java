package com.edtech.platform.booking.controller;

import com.edtech.platform.booking.domain.DeliveryMode;
import com.edtech.platform.booking.dto.response.SessionReportView;
import com.edtech.platform.booking.service.BookingService;
import com.edtech.platform.common.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StudentSessionReportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private StudentSessionReportController controller;

    private final UUID studentUserId = UUID.randomUUID();

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
                        return new AuthenticatedUser(studentUserId, "student@example.com", "STUDENT");
                    }
                })
                .build();
    }

    @Test
    void listSessionReports_shouldReturn200Paged() throws Exception {
        UUID reportId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        Instant now = Instant.now();

        SessionReportView view = new SessionReportView(
                reportId, bookingId, teacherId, subjectId,
                now.minusSeconds(3600), now, DeliveryMode.ONLINE,
                "https://rec.example.com", "Content", "Feedback", "Note",
                (short) 5, now
        );

        when(bookingService.findStudentSessionReports(eq(studentUserId), any()))
                .thenReturn(new PageImpl<>(List.of(view)));

        mockMvc.perform(get("/api/student/session-reports")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(reportId.toString()))
                .andExpect(jsonPath("$.data[0].bookingId").value(bookingId.toString()))
                .andExpect(jsonPath("$.data[0].teacherId").value(teacherId.toString()))
                .andExpect(jsonPath("$.data[0].content").value("Content"))
                .andExpect(jsonPath("$.meta.totalElements").value(1));
    }
}
