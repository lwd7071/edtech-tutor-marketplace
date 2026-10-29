package com.edtech.platform.booking.web;

import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.booking.dto.request.CreateTrialRequest;
import com.edtech.platform.booking.service.TrialRequestService;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentTrialRequestPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class StudentTrialRequestPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private TrialRequestService trials;
    @MockBean private JwtTokenProvider tokens;
    @MockBean private UserStatusCacheService statuses;
    @MockBean private SessionService sessions;
    @MockBean private Clock clock;

    private final UUID studentId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();

    @BeforeEach
    void authenticateStudent() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(studentId, "student@example.test", "STUDENT"), null, List.of()));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsTrialRequestFromVietnameseLocalTimeAndRedirectsToRequestList() throws Exception {
        mockMvc.perform(post("/student/trials/requests")
                        .param("teacherId", teacherId.toString())
                        .param("subjectId", subjectId.toString())
                        .param("preferredStartTime", "2026-10-30T19:30")
                        .param("note", "  Ôn phần đại số  "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/requests?tab=trial"))
                .andExpect(flash().attribute("trialRequestSuccess",
                        "Đã gửi yêu cầu học thử. Gia sư sẽ phản hồi thời gian phù hợp."));

        verify(trials).create(eq(studentId), eq(new CreateTrialRequest(teacherId, subjectId,
                Instant.parse("2026-10-30T12:30:00Z"), "Ôn phần đại số")));
    }

    @Test
    void invalidFieldsUsePrgAndDoNotCallService() throws Exception {
        mockMvc.perform(post("/student/trials/requests")
                        .param("teacherId", teacherId.toString())
                        .param("subjectId", subjectId.toString())
                        .param("preferredStartTime", "không phải giờ"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teachers/" + teacherId + "#trial"))
                .andExpect(flash().attribute("trialRequestError", "Chọn môn học và thời gian mong muốn hợp lệ."));
        verify(trials, never()).create(eq(studentId), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void pendingRequestConflictReturnsSafeMessageAndKeepsInput() throws Exception {
        when(trials.create(eq(studentId), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new BusinessException(ErrorCode.TRIAL_REQUEST_ALREADY_PENDING));

        mockMvc.perform(post("/student/trials/requests")
                        .param("teacherId", teacherId.toString())
                        .param("subjectId", subjectId.toString())
                        .param("preferredStartTime", "2026-10-30T19:30")
                        .param("note", "Mục tiêu của em"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teachers/" + teacherId + "#trial"))
                .andExpect(flash().attribute("trialRequestError", "Bạn đã có một yêu cầu học thử đang chờ với gia sư này."))
                .andExpect(flash().attribute("trialFormSubjectId", subjectId.toString()))
                .andExpect(flash().attribute("trialFormPreferredStartTime", "2026-10-30T19:30"))
                .andExpect(flash().attribute("trialFormNote", "Mục tiêu của em"));
    }

    @Test
    void malformedTeacherIdReturnsToMarketplaceInsteadOfTrustingRedirectInput() throws Exception {
        mockMvc.perform(post("/student/trials/requests")
                        .param("teacherId", "//evil.example")
                        .param("subjectId", subjectId.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teachers"));
        verify(trials, never()).create(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
