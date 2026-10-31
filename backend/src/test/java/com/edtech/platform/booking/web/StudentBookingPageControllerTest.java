package com.edtech.platform.booking.web;

import com.edtech.platform.booking.service.BookingReadService;
import com.edtech.platform.booking.service.BookingService;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.auth.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(StudentBookingPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class StudentBookingPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private BookingReadService bookingReads;
    @MockBean private BookingService bookingCommands;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private UserStatusCacheService userStatusCacheService;
    @MockBean private SessionService sessions;
    @MockBean private Clock clock;

    private final UUID studentId = UUID.randomUUID();
    private final UUID bookingId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(studentId, "student@example.test", "STUDENT"), null, List.of()));
    }

    @Test
    void rendersBookingListAndKeepsFilterInNoScriptHtml() throws Exception {
        when(bookingReads.list(eq(studentId), eq(false), eq(null), eq(null), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(bookingRow()), org.springframework.data.domain.PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/student/bookings"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/bookings"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Đại số")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Gia sư: Trần Thu Hà")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("method=\"get\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script"))));
    }

    @Test
    void rendersEmptyAndUnavailableBookingList() throws Exception {
        when(bookingReads.list(eq(studentId), eq(false), eq(null), eq(null), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(), org.springframework.data.domain.PageRequest.of(0, 10), 0));
        mockMvc.perform(get("/student/bookings"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa có lịch học")));

        when(bookingReads.list(eq(studentId), eq(false), eq(null), eq(null), eq(null), any()))
                .thenThrow(new IllegalStateException("unavailable"));
        mockMvc.perform(get("/student/bookings"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tải được lịch học")));
    }

    @Test
    void rendersOwnedDetailAndOnlyOffersConfirmWhenServiceAllows() throws Exception {
        when(bookingReads.detail(eq(studentId), eq(bookingId), eq(false))).thenReturn(bookingDetail(true));
        mockMvc.perform(get("/student/bookings/{id}", bookingId))
                .andExpect(status().isOk())
                .andExpect(view().name("student/booking-detail"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Xác nhận buổi học")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"https://meeting.test/room\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script"))));

        when(bookingReads.detail(eq(studentId), eq(bookingId), eq(false))).thenReturn(bookingDetail(false));
        mockMvc.perform(get("/student/bookings/{id}", bookingId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Xác nhận buổi học"))));
    }

    @Test
    void hidesBookingWhenItIsNotOwned() throws Exception {
        when(bookingReads.detail(eq(studentId), eq(bookingId), eq(false)))
                .thenThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        mockMvc.perform(get("/student/bookings/{id}", bookingId))
                .andExpect(status().isNotFound())
                .andExpect(view().name("student/booking-not-found"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("không thuộc tài khoản của bạn")));
    }

    @Test
    void untrustedMeetingLinkIsShownAsTextInsteadOfClickableUrl() throws Exception {
        var detail = new java.util.LinkedHashMap<>(bookingDetail(false));
        detail.put("meetingLink", "javascript:alert(1)");
        when(bookingReads.detail(eq(studentId), eq(bookingId), eq(false))).thenReturn(detail);

        mockMvc.perform(get("/student/bookings/{id}", bookingId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("javascript:alert(1)")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("href=\"javascript:"))));
    }

    @Test
    void confirmUsesPrincipalVersionAndRedirectsBackToDetail() throws Exception {
        mockMvc.perform(post("/student/bookings/{id}/confirm", bookingId).param("version", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/bookings/" + bookingId));
        verify(bookingCommands).confirmByStudent(studentId, bookingId, 3L);
    }

    private Map<String, Object> bookingRow() {
        return Map.of("id", bookingId, "status", "SCHEDULED", "deliveryMode", "ONLINE", "trial", false,
                "startTime", Instant.parse("2026-10-28T12:00:00Z"),
                "teacher", Map.of("fullName", "Trần Thu Hà"), "subject", Map.of("name", "Đại số"));
    }

    private Map<String, Object> bookingDetail(boolean canConfirm) {
        return Map.ofEntries(Map.entry("id", bookingId), Map.entry("status", "COMPLETED"),
                Map.entry("deliveryMode", "ONLINE"), Map.entry("trial", false), Map.entry("canConfirm", canConfirm),
                Map.entry("version", 3L), Map.entry("startTime", Instant.parse("2026-10-28T12:00:00Z")),
                Map.entry("endTime", Instant.parse("2026-10-28T13:00:00Z")),
                Map.entry("meetingLink", "https://meeting.test/room"),
                Map.entry("teacher", Map.of("fullName", "Trần Thu Hà")),
                Map.entry("subject", Map.of("name", "Đại số")), Map.entry("sessionReport", Map.of()));
    }
}
