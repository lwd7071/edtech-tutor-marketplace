package com.edtech.platform.dashboard.controller;

import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.dashboard.dto.response.StudentDashboardView;
import com.edtech.platform.dashboard.service.StudentDashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.nullValue;

@ExtendWith(MockitoExtension.class)
class StudentDashboardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private StudentDashboardService service;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new StudentDashboardController(service))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json().featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build()))
                .build();
    }

    @Test
    void getDashboardReturnsEnvelopeAndProjection() throws Exception {
        UUID studentId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(studentId, "student@example.test", "STUDENT");
        when(service.getDashboard(studentId)).thenReturn(new StudentDashboardView(
                Instant.parse("2026-09-14T10:00:00Z"), 12, 3, 4, 2));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null));

        mockMvc.perform(get("/api/student/dashboard").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(nullValue()))
                .andExpect(jsonPath("$.data.nextBookingStartTime").value("2026-09-14T10:00:00Z"))
                .andExpect(jsonPath("$.data.remainingSessions").value(12))
                .andExpect(jsonPath("$.data.todoAssignments").value(3))
                .andExpect(jsonPath("$.data.unreadNotifications").value(4))
                .andExpect(jsonPath("$.data.pendingRequests").value(2))
                .andExpect(jsonPath("$.errors").value(nullValue()))
                .andExpect(jsonPath("$.meta").value(nullValue()));

        verify(service).getDashboard(studentId);
        SecurityContextHolder.clearContext();
    }
}
