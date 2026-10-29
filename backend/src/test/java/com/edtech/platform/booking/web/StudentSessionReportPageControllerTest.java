package com.edtech.platform.booking.web;

import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.booking.domain.DeliveryMode;
import com.edtech.platform.booking.dto.response.SessionReportView;
import com.edtech.platform.booking.service.BookingReadService;
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

import java.time.Instant;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(StudentSessionReportPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class StudentSessionReportPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private BookingReadService bookingReads;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private UserStatusCacheService userStatusCacheService;
    @MockBean private SessionService sessionService;
    @MockBean private Clock clock;

    private final UUID studentId = UUID.randomUUID();

    @BeforeEach
    void authenticateStudent() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(studentId, "student@example.com", "STUDENT"), null, List.of()));
    }

    @Test
    void rendersOwnedReportsAndNativePagination() throws Exception {
        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "submittedAt"));
        when(bookingReads.findStudentSessionReports(eq(studentId), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(report("https://records.example/session")), pageable, 11));

        mockMvc.perform(get("/student/session-reports"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/session-reports"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Báo cáo buổi học")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nhận xét sau buổi học")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("records.example/session")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Trang sau")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script"))));
    }

    @Test
    void unsafeRecordLinkIsNotRenderedAsAnchor() throws Exception {
        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "submittedAt"));
        when(bookingReads.findStudentSessionReports(eq(studentId), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(report("javascript:alert(1)")), pageable, 1));

        mockMvc.perform(get("/student/session-reports"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("javascript:"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Xem bản ghi"))));
    }

    @Test
    void emptyAndUnavailableStatesAreRendered() throws Exception {
        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "submittedAt"));
        when(bookingReads.findStudentSessionReports(eq(studentId), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));
        mockMvc.perform(get("/student/session-reports"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa có báo cáo buổi học")));

        when(bookingReads.findStudentSessionReports(eq(studentId), eq(pageable))).thenThrow(new IllegalStateException("unavailable"));
        mockMvc.perform(get("/student/session-reports"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tải được báo cáo buổi học")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Thử tải lại")));
    }

    private SessionReportView report(String recordLink) {
        return new SessionReportView(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                Instant.parse("2026-10-27T12:00:00Z"), Instant.parse("2026-10-27T13:00:00Z"),
                DeliveryMode.ONLINE, recordLink, "Nội dung buổi học", "Nhận xét sau buổi học", "Ôn lại chương 2", (short) 5,
                Instant.parse("2026-10-27T13:00:00Z"));
    }
}
