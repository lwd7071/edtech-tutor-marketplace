package com.edtech.platform.booking.web;

import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.booking.domain.BookingStatus;
import com.edtech.platform.booking.domain.DeliveryMode;
import com.edtech.platform.booking.domain.TrialRequestStatus;
import com.edtech.platform.booking.dto.request.AcceptTrialRequest;
import com.edtech.platform.booking.dto.request.RejectTrialRequest;
import com.edtech.platform.booking.dto.response.BookingDetail;
import com.edtech.platform.booking.dto.response.TrialRequestView;
import com.edtech.platform.booking.service.TrialRequestService;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(TeacherTrialRequestsPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeacherTrialRequestsPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private TrialRequestService trials;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private UserStatusCacheService userStatusCacheService;
    @MockBean private SessionService sessions;
    @MockBean private Clock clock;

    private final UUID teacherId = UUID.randomUUID();
    private final UUID requestId = UUID.randomUUID();
    private final UUID bookingId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(teacherId, "teacher@example.test", "TEACHER"), null, List.of()));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
        when(clock.instant()).thenReturn(Instant.parse("2026-10-28T12:00:00Z"));
    }

    @Test
    void rendersOwnedTrialRequestsAndNativeResponseForms() throws Exception {
        var pageable = PageRequest.of(0, 12, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        when(trials.findTeacherWorkspace(teacherId, pageable)).thenReturn(new PageImpl<>(List.of(row()), pageable, 1));

        mockMvc.perform(get("/teacher/trial-requests"))
                .andExpect(status().isOk())
                .andExpect(view().name("workspace/teacher-trial-requests"))
                .andExpect(header().string("Cache-Control", "no-store, max-age=0"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nguyễn Minh An · Đại số")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mục tiêu học thử")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("method=\"post\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("datetime-local")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chọn giờ và chấp nhận")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Từ chối và gửi lý do")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nhập giờ Việt Nam")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script"))));
    }

    @Test
    void acceptsTrialWithVietnameseLocalTimesAndRedirectsToCreatedBooking() throws Exception {
        Instant start = Instant.parse("2026-10-29T12:00:00Z");
        Instant end = Instant.parse("2026-10-29T13:00:00Z");
        when(trials.accept(teacherId, requestId, new AcceptTrialRequest(start, end, DeliveryMode.ONLINE,
                "https://meeting.test/room", null, 3L)))
                .thenReturn(new BookingDetail(bookingId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
                        start, end, DeliveryMode.ONLINE, BookingStatus.SCHEDULED, true, false, null, 0L));

        mockMvc.perform(post("/teacher/trial-requests/{id}/accept", requestId)
                        .param("version", "3").param("page", "0")
                        .param("startTime", "2026-10-29T19:00").param("endTime", "2026-10-29T20:00")
                        .param("deliveryMode", "ONLINE").param("meetingLink", "https://meeting.test/room"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teacher/bookings/" + bookingId));
        verify(trials).accept(teacherId, requestId, new AcceptTrialRequest(start, end, DeliveryMode.ONLINE,
                "https://meeting.test/room", null, 3L));
    }

    @Test
    void rejectsWithCurrentVersionAndPreservesPage() throws Exception {
        mockMvc.perform(post("/teacher/trial-requests/{id}/reject", requestId)
                        .param("version", "2").param("page", "3").param("reason", "Không còn giờ phù hợp"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teacher/trial-requests?page=3"));
        verify(trials).reject(teacherId, requestId, new RejectTrialRequest("Không còn giờ phù hợp", 2L));
    }

    @Test
    void invalidTimeAndUnsafeLinkDoNotCallAcceptanceService() throws Exception {
        mockMvc.perform(post("/teacher/trial-requests/{id}/accept", requestId)
                        .param("version", "0").param("page", "0")
                        .param("startTime", "2026-10-30T19:00").param("endTime", "2026-10-30T18:00")
                        .param("deliveryMode", "ONLINE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teacher/trial-requests?page=0#trial-action-error"));
        mockMvc.perform(post("/teacher/trial-requests/{id}/accept", requestId)
                        .param("version", "0").param("page", "0")
                        .param("startTime", "2026-10-30T19:00").param("endTime", "2026-10-30T20:00")
                        .param("deliveryMode", "ONLINE").param("meetingLink", "javascript:alert(1)"))
                .andExpect(status().is3xxRedirection());
        verify(trials, never()).accept(eq(teacherId), eq(requestId), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void concurrentUpdateReturnsSafeRetryMessage() throws Exception {
        when(trials.reject(teacherId, requestId, new RejectTrialRequest("Bận", 1L)))
                .thenThrow(new BusinessException(ErrorCode.CONCURRENT_MODIFICATION));
        mockMvc.perform(post("/teacher/trial-requests/{id}/reject", requestId)
                        .param("version", "1").param("page", "0").param("reason", "Bận"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teacher/trial-requests?page=0#trial-action-error"));
    }

    private TeacherTrialRequestRow row() {
        UUID studentId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        TrialRequestView request = new TrialRequestView(requestId, UUID.randomUUID(), studentId, subjectId,
                Instant.parse("2026-10-30T12:00:00Z"), "Mục tiêu học thử", TrialRequestStatus.PENDING,
                null, null, null, Instant.parse("2026-10-28T11:00:00Z"), 3L);
        return new TeacherTrialRequestRow(request, "Nguyễn Minh An", "Đại số");
    }
}
