package com.edtech.platform.booking.web;

import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.booking.domain.BookingRequestStatus;
import com.edtech.platform.booking.dto.response.BookingRequestView;
import com.edtech.platform.booking.service.BookingRequestService;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(TeacherBookingRequestsPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeacherBookingRequestsPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private BookingRequestService bookingRequests;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private UserStatusCacheService userStatusCacheService;
    @MockBean private SessionService sessionService;
    @MockBean private Clock clock;

    private final UUID teacherId = UUID.randomUUID();
    private final UUID requestId = UUID.randomUUID();

    @BeforeEach
    void authenticateTeacher() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(teacherId, "teacher@example.test", "TEACHER"), null, List.of()));
    }

    @Test
    void rendersNoJsGetFilterAndEmptyState() throws Exception {
        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        when(bookingRequests.findTeacherWorkspace(eq(teacherId), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        mockMvc.perform(get("/teacher/booking-requests").param("status", "ALL"))
                .andExpect(status().isOk()).andExpect(view().name("workspace/teacher-booking-requests"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("method=\"get\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa có yêu cầu đặt buổi")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script"))));
        verify(bookingRequests).findTeacherWorkspace(teacherId, null, pageable);
    }

    @Test
    void preservesSelectedStatusAndShowsPendingRequestActions() throws Exception {
        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        var request = new BookingRequestView(requestId, UUID.randomUUID(), UUID.randomUUID(), teacherId,
                UUID.randomUUID(), Instant.parse("2026-11-01T12:00:00Z"), "Mong học buổi tối",
                BookingRequestStatus.PENDING, null, null, Instant.parse("2026-10-29T00:00:00Z"), 0);
        when(bookingRequests.findTeacherWorkspace(eq(teacherId), eq(BookingRequestStatus.PENDING), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(new TeacherBookingRequestRow(request, "Nguyễn An", "Toán")), pageable, 1));

        mockMvc.perform(get("/teacher/booking-requests").param("status", "PENDING"))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Nguyễn An")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mong học buổi tối")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/teacher/booking-requests/" + requestId + "/accept")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"version\"")));
    }

    @Test
    void acceptsFinalTimeAndDeliveryAsNativeFormPost() throws Exception {
        mockMvc.perform(post("/teacher/booking-requests/{id}/accept", requestId)
                        .param("startTime", "2026-11-01T19:00")
                        .param("deliveryMode", "ONLINE").param("version", "0"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/teacher/booking-requests"));
        verify(bookingRequests).accept(eq(teacherId), eq(requestId), org.mockito.ArgumentMatchers.argThat(command ->
                command.deliveryMode().name().equals("ONLINE") && command.startTime().equals(Instant.parse("2026-11-01T12:00:00Z"))));
    }
}
