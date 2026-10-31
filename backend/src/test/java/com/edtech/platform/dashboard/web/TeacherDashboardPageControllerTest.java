package com.edtech.platform.dashboard.web;

import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.booking.domain.BookingStatus;
import com.edtech.platform.booking.domain.BookingRequestStatus;
import com.edtech.platform.booking.domain.DeliveryMode;
import com.edtech.platform.booking.domain.TrialRequestStatus;
import com.edtech.platform.booking.dto.response.BookingRequestView;
import com.edtech.platform.booking.dto.response.TrialRequestView;
import com.edtech.platform.booking.service.BookingRequestService;
import com.edtech.platform.booking.service.BookingReadService;
import com.edtech.platform.booking.service.TrialRequestService;
import com.edtech.platform.booking.web.TeacherBookingRequestRow;
import com.edtech.platform.booking.web.TeacherTrialRequestRow;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.finance.dto.response.WalletView;
import com.edtech.platform.finance.service.WalletQueryService;
import com.edtech.platform.learning.domain.SubmissionStatus;
import com.edtech.platform.learning.dto.response.SubmissionDetail;
import com.edtech.platform.learning.service.AssignmentReadService;
import com.edtech.platform.teacher.domain.ProfileStatus;
import com.edtech.platform.teacher.dto.TeacherProfileDetail;
import com.edtech.platform.teacher.service.TeacherProfileService;
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

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(TeacherDashboardPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeacherDashboardPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private TeacherProfileService profiles;
    @MockBean private BookingRequestService bookingRequests;
    @MockBean private BookingReadService bookings;
    @MockBean private TrialRequestService trials;
    @MockBean private AssignmentReadService assignments;
    @MockBean private WalletQueryService wallets;
    @MockBean private JwtTokenProvider tokens;
    @MockBean private UserStatusCacheService statuses;
    @MockBean private SessionService sessions;
    @MockBean private Clock clock;

    private final UUID teacherId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-10-28T10:00:00Z");

    @BeforeEach
    void authenticateTeacher() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(teacherId, "teacher@example.test", "TEACHER"), null, List.of()));
        when(clock.instant()).thenReturn(now);
    }

    @Test
    void overviewCombinesRealWorkspaceDataAndUsesUpcomingAndPendingQueries() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID bookingRequestId = UUID.randomUUID();
        UUID trialId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        Instant lessonTime = Instant.parse("2026-10-29T12:30:00Z");
        var bookingPage = PageRequest.of(0, 5, Sort.by(Sort.Order.asc("startTime"), Sort.Order.asc("id")));
        var bookingRequestsPage = PageRequest.of(0, 3, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        var trialsPage = PageRequest.of(0, 3, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        var assignmentsPage = PageRequest.of(0, 8, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        when(profiles.getProfile(teacherId)).thenReturn(profile(ProfileStatus.REJECTED, "Bổ sung thông tin giảng dạy"));
        BookingRequestView bookingRequest = new BookingRequestView(bookingRequestId, UUID.randomUUID(), studentId,
                UUID.randomUUID(), subjectId, lessonTime, null, BookingRequestStatus.PENDING, null, null, now, 0);
        when(bookingRequests.findTeacherWorkspace(teacherId, BookingRequestStatus.PENDING, bookingRequestsPage))
                .thenReturn(new PageImpl<>(List.of(new TeacherBookingRequestRow(bookingRequest, "Nguyễn An", "Toán")),
                        bookingRequestsPage, 1));
        when(bookings.findUpcomingTeacherWorkspace(teacherId, now, bookingPage)).thenReturn(new PageImpl<>(List.of(
                booking(bookingId, studentId, subjectId, lessonTime)), bookingPage, 1));
        TrialRequestView trial = new TrialRequestView(trialId, UUID.randomUUID(), studentId, subjectId,
                lessonTime, null, TrialRequestStatus.PENDING, null, null, null, now, 1);
        when(trials.findPendingTeacherWorkspace(teacherId, trialsPage)).thenReturn(new PageImpl<>(List.of(
                new TeacherTrialRequestRow(trial, "Nguyễn An", "Toán")), trialsPage, 1));
        SubmissionDetail submitted = new SubmissionDetail(submissionId, assignmentId, studentId, List.of(),
                now, SubmissionStatus.SUBMITTED, null, null, null, 0);
        when(assignments.teacherList(teacherId, assignmentsPage)).thenReturn(new PageImpl<>(List.of(
                assignment(assignmentId, submitted)), assignmentsPage, 1));
        when(wallets.getOrCreateWallet(teacherId)).thenReturn(new WalletView(UUID.randomUUID(), teacherId,
                4_000, 125_000, 0, 3, 60_000));

        mockMvc.perform(get("/teacher"))
                .andExpect(status().isOk()).andExpect(view().name("workspace/teacher-dashboard"))
                .andExpect(header().string("Cache-Control", "no-store, max-age=0"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Hồ sơ cần chỉnh sửa")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Bổ sung thông tin giảng dạy")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Yêu cầu đặt buổi đang chờ")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nguyễn An · Toán")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Thứ Năm, 29/10/2026 · 19:30")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Yêu cầu học thử đang chờ")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chờ chấm")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("125.000 ₫")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/teacher/booking-requests?status=PENDING")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/teacher/assignments/" + assignmentId)))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script"))));

        verify(bookings).findUpcomingTeacherWorkspace(teacherId, now, bookingPage);
        verify(bookingRequests).findTeacherWorkspace(teacherId, BookingRequestStatus.PENDING, bookingRequestsPage);
        verify(trials).findPendingTeacherWorkspace(teacherId, trialsPage);
        verify(trials, never()).findTeacherWorkspace(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(assignments).teacherList(teacherId, assignmentsPage);
    }

    @Test
    void independentSectionFailuresKeepPageAndShowNoRawExceptions() throws Exception {
        when(profiles.getProfile(teacherId)).thenThrow(new IllegalStateException("private profile details"));
        when(bookingRequests.findTeacherWorkspace(org.mockito.ArgumentMatchers.eq(teacherId),
                org.mockito.ArgumentMatchers.eq(BookingRequestStatus.PENDING), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new IllegalStateException("private request details"));
        when(bookings.findUpcomingTeacherWorkspace(org.mockito.ArgumentMatchers.eq(teacherId), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenThrow(new IllegalStateException("private booking details"));
        when(trials.findPendingTeacherWorkspace(org.mockito.ArgumentMatchers.eq(teacherId), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new IllegalStateException("private trial details"));
        when(assignments.teacherList(org.mockito.ArgumentMatchers.eq(teacherId), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new IllegalStateException("private assignment details"));
        when(wallets.getOrCreateWallet(teacherId)).thenThrow(new IllegalStateException("private wallet details"));

        mockMvc.perform(get("/teacher"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tải được trạng thái hồ sơ")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tải được yêu cầu đặt buổi")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tải được lịch dạy")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tải được yêu cầu học thử")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tải được bài tập")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tải được số dư")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private "))));
    }

    @Test
    void emptyOverviewUsesActionableEmptyStates() throws Exception {
        var bookingPage = PageRequest.of(0, 5, Sort.by(Sort.Order.asc("startTime"), Sort.Order.asc("id")));
        var bookingRequestsPage = PageRequest.of(0, 3, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        var trialsPage = PageRequest.of(0, 3, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        var assignmentsPage = PageRequest.of(0, 8, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        when(profiles.getProfile(teacherId)).thenReturn(profile(ProfileStatus.DRAFT, null));
        when(bookingRequests.findTeacherWorkspace(teacherId, BookingRequestStatus.PENDING, bookingRequestsPage))
                .thenReturn(new PageImpl<>(List.of(), bookingRequestsPage, 0));
        when(bookings.findUpcomingTeacherWorkspace(teacherId, now, bookingPage)).thenReturn(new PageImpl<>(List.of(), bookingPage, 0));
        when(trials.findPendingTeacherWorkspace(teacherId, trialsPage)).thenReturn(new PageImpl<>(List.of(), trialsPage, 0));
        when(assignments.teacherList(teacherId, assignmentsPage)).thenReturn(new PageImpl<>(List.of(), assignmentsPage, 0));
        when(wallets.getOrCreateWallet(teacherId)).thenReturn(new WalletView(UUID.randomUUID(), teacherId, 0, 0, 0, 0, 0));

        mockMvc.perform(get("/teacher"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa có buổi học sắp tới")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/teacher/availability")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Không có yêu cầu đặt buổi nào đang chờ")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Không có yêu cầu nào đang chờ phản hồi")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa có bài nộp trong các bài tập gần đây")));
    }

    private TeacherProfileDetail profile(ProfileStatus status, String rejectionReason) {
        return new TeacherProfileDetail(UUID.randomUUID(), "Gia sư", 4, List.of("vi"), true, false,
                null, "01", "001", null, status, rejectionReason, false, false);
    }

    private Map<String, Object> booking(UUID id, UUID studentId, UUID subjectId, Instant start) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("startTime", start);
        result.put("endTime", start.plusSeconds(3_600));
        result.put("deliveryMode", DeliveryMode.ONLINE);
        result.put("status", BookingStatus.SCHEDULED);
        result.put("student", Map.of("id", studentId, "fullName", "Nguyễn An"));
        result.put("subject", Map.of("id", subjectId, "name", "Toán"));
        return result;
    }

    private Map<String, Object> assignment(UUID id, SubmissionDetail submitted) {
        return Map.of("id", id, "title", "Phương trình bậc hai", "studentName", "Nguyễn An",
                "subjectName", "Toán", "submissions", List.of(submitted));
    }
}
