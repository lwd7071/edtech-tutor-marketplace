package com.edtech.platform.ranking.controller;

import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.ranking.dto.response.TeacherStatsView;
import com.edtech.platform.ranking.service.TeacherStatsService;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.teacher.facade.dto.TeacherSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeacherStatsController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeacherStatsControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private TeacherStatsService statistics;
    @MockBean private TeacherFacade teachers;
    @MockBean private JwtTokenProvider tokens;
    @MockBean private UserStatusCacheService statuses;
    @MockBean private SessionService sessions;
    @MockBean private Clock clock;

    private final UUID teacherUserId = UUID.randomUUID();
    private final UUID teacherProfileId = UUID.randomUUID();

    @BeforeEach
    void authenticateTeacher() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(teacherUserId, "teacher@example.test", "TEACHER"), null, List.of()));
        when(teachers.getTeacherByUserId(teacherUserId)).thenReturn(new TeacherSnapshot(teacherProfileId,
                teacherUserId, "APPROVED", true, true, "Teacher", null, null, null,
                true, false, List.of(), null, null, null, null));
        when(statistics.getTeacherStats(teacherProfileId)).thenReturn(
                TeacherStatsView.builder().teacherId(teacherProfileId).build());
    }

    @Test
    void resolvesTeacherProfileIdBeforeReadingStats() throws Exception {
        mockMvc.perform(get("/api/teacher/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teacherId").value(teacherProfileId.toString()));

        verify(teachers).getTeacherByUserId(teacherUserId);
        verify(statistics).getTeacherStats(teacherProfileId);
    }
}
