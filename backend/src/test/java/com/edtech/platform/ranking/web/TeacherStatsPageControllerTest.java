package com.edtech.platform.ranking.web;

import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
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

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(TeacherStatsPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeacherStatsPageControllerTest {
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
    }

    @Test
    void rendersServerSideStatisticsAndUsesSessionPrincipal() throws Exception {
        when(statistics.getTeacherStats(teacherProfileId)).thenReturn(TeacherStatsView.builder()
                .teacherId(teacherProfileId).averageRating(new BigDecimal("4.75")).bayesianRating(new BigDecimal("4.50"))
                .reviewCount(12).completedSessionCount(28).completionRate(new BigDecimal("0.8750"))
                .trialSessionCount(4).trialConversionRate(new BigDecimal("0.2500"))
                .globalRank(3).calculatedAt(Instant.parse("2026-10-28T12:00:00Z")).build());

        mockMvc.perform(get("/teacher/stats"))
                .andExpect(status().isOk())
                .andExpect(view().name("workspace/teacher-stats"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Buổi học hoàn thành")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("87,5%")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script"))));
        verify(statistics).getTeacherStats(teacherProfileId);
    }

    @Test
    void showsEmptyStateWhenStatsHaveNotBeenCreated() throws Exception {
        when(statistics.getTeacherStats(teacherProfileId)).thenThrow(new BusinessException(ErrorCode.TEACHER_STATS_NOT_FOUND));
        mockMvc.perform(get("/teacher/stats"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa có dữ liệu thống kê")));
    }

    @Test
    void showsRetryStateForServiceFailure() throws Exception {
        when(statistics.getTeacherStats(teacherProfileId)).thenThrow(new IllegalStateException("unavailable"));
        mockMvc.perform(get("/teacher/stats"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Thử tải lại")));
    }
}
