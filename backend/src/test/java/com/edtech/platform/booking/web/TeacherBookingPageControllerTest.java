package com.edtech.platform.booking.web;

import com.edtech.platform.booking.domain.CancelInitiatedBy;
import com.edtech.platform.booking.dto.request.CancelBookingRequest;
import com.edtech.platform.booking.dto.request.CompleteBookingRequest;
import com.edtech.platform.booking.dto.request.SessionReportRequest;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(TeacherBookingPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeacherBookingPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private BookingReadService bookingReads;
    @MockBean private BookingService bookingCommands;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private UserStatusCacheService userStatusCacheService;
    @MockBean private SessionService sessions;
    @MockBean private Clock clock;

    private final UUID teacherId = UUID.randomUUID();
    private final UUID bookingId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(teacherId, "teacher@example.test", "TEACHER"), null, List.of()));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
        when(clock.instant()).thenReturn(Instant.parse("2026-10-28T12:00:00Z"));
    }

    @Test
    void rendersBookingListWithNativeGetFilterAndBatchReadModel() throws Exception {
        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("startTime"), Sort.Order.desc("id")));
        when(bookingReads.listTeacherWorkspace(eq(teacherId), eq(null), eq(null), eq(null), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(listItem("SCHEDULED", false)), pageable, 1));

        mockMvc.perform(get("/teacher/bookings"))
                .andExpect(status().isOk()).andExpect(view().name("workspace/teacher-bookings"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Lịch dạy")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nguyễn Minh An")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Đại số")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("method=\"get\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script"))));
        verify(bookingReads).listTeacherWorkspace(teacherId, null, null, null, pageable);
    }

    @Test
    void keepsStatusFilterInPaginationAndNormalizesInvalidFilter() throws Exception {
        var pageable = PageRequest.of(2, 10, Sort.by(Sort.Order.desc("startTime"), Sort.Order.desc("id")));
        when(bookingReads.listTeacherWorkspace(eq(teacherId), eq(com.edtech.platform.booking.domain.BookingStatus.CANCELLED), eq(null), eq(null), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(listItem("CANCELLED", false)), pageable, 25));
        mockMvc.perform(get("/teacher/bookings").param("status", "cancelled").param("page", "2"))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("status=CANCELLED")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("page=1")));

        var first = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("startTime"), Sort.Order.desc("id")));
        when(bookingReads.listTeacherWorkspace(eq(teacherId), eq(null), eq(null), eq(null), eq(first)))
                .thenReturn(new PageImpl<>(List.of(), first, 0));
        mockMvc.perform(get("/teacher/bookings").param("status", "not-a-status"))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("không hợp lệ")));
    }

    @Test
    void rendersEmptyAndUnavailableListStates() throws Exception {
        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("startTime"), Sort.Order.desc("id")));
        when(bookingReads.listTeacherWorkspace(eq(teacherId), eq(null), eq(null), eq(null), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));
        mockMvc.perform(get("/teacher/bookings"))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa có buổi học")));
        when(bookingReads.listTeacherWorkspace(eq(teacherId), eq(null), eq(null), eq(null), eq(pageable)))
                .thenThrow(new IllegalStateException("private failure"));
        mockMvc.perform(get("/teacher/bookings"))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tải được lịch dạy")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private failure"))));
    }

    @Test
    void dateRangeUsesVietnameseDayBoundariesAndSurvivesPagination() throws Exception {
        var pageable = PageRequest.of(1, 10, Sort.by(Sort.Order.desc("startTime"), Sort.Order.desc("id")));
        Instant from = Instant.parse("2026-10-28T17:00:00Z");
        Instant to = Instant.parse("2026-10-31T17:00:00Z");
        when(bookingReads.listTeacherWorkspace(eq(teacherId), eq(null), eq(from), eq(to), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(listItem("SCHEDULED", false)), pageable, 11));

        mockMvc.perform(get("/teacher/bookings").param("from", "2026-10-29").param("to", "2026-10-31").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("from=2026-10-29")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("to=2026-10-31")));
        verify(bookingReads).listTeacherWorkspace(teacherId, null, from, to, pageable);
    }

    @Test
    void invalidDateRangeFallsBackSafelyAndFilteredEmptyStateCanReset() throws Exception {
        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("startTime"), Sort.Order.desc("id")));
        when(bookingReads.listTeacherWorkspace(eq(teacherId), eq(null), eq(null), eq(null), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));
        when(bookingReads.listTeacherWorkspace(eq(teacherId), eq(null), eq(Instant.parse("2026-10-28T17:00:00Z")),
                eq(null), eq(pageable))).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        mockMvc.perform(get("/teacher/bookings").param("from", "not-a-date"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Khoảng ngày không hợp lệ")));
        mockMvc.perform(get("/teacher/bookings").param("from", "2026-10-29"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Không có buổi học khớp bộ lọc")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Xóa bộ lọc")));
    }

    @Test
    void detailShowsOnlyAllowedActionsAndEscapedSafeLinks() throws Exception {
        when(bookingReads.detail(teacherId, bookingId, true)).thenReturn(detail("COMPLETED", false, true, false));
        mockMvc.perform(get("/teacher/bookings/{id}", bookingId))
                .andExpect(status().isOk()).andExpect(view().name("workspace/teacher-booking-detail"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Gửi báo cáo và xác nhận")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"https://meeting.test/room\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Hủy buổi học"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script"))));

        when(bookingReads.detail(teacherId, bookingId, true)).thenReturn(detail("COMPLETED", false, false, true));
        mockMvc.perform(get("/teacher/bookings/{id}", bookingId))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"https://files.test/report\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Gửi báo cáo và xác nhận"))));
    }

    @Test
    void scheduledEndedTrialShowsReportAndCancelForms() throws Exception {
        when(bookingReads.detail(teacherId, bookingId, true)).thenReturn(detail("SCHEDULED", true, false, false));
        mockMvc.perform(get("/teacher/bookings/{id}", bookingId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Gửi báo cáo buổi học")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Hủy buổi học")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Xác nhận buổi học"))));
    }

    @Test
    void hidesDetailForMissingOrUnownedBookingAndOffersRetryForReadFailure() throws Exception {
        when(bookingReads.detail(teacherId, bookingId, true)).thenThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        mockMvc.perform(get("/teacher/bookings/{id}", bookingId))
                .andExpect(status().isNotFound()).andExpect(content().string(org.hamcrest.Matchers.containsString("không thuộc tài khoản gia sư")));

        doThrow(new IllegalStateException("private failure")).when(bookingReads).detail(teacherId, bookingId, true);
        mockMvc.perform(get("/teacher/bookings/{id}", bookingId))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Thử tải lại")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private failure"))));
    }

    @Test
    void completeSubmitsReportAndPreservesNativePrg() throws Exception {
        mockMvc.perform(post("/teacher/bookings/{id}/complete", bookingId)
                        .param("version", "4").param("recordLink", "https://files.test/new")
                        .param("content", "Ôn tập phương trình").param("feedback", "Làm bài tốt")
                        .param("followUpNote", "Luyện thêm").param("teacherSelfRating", "5"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/teacher/bookings/" + bookingId));
        verify(bookingCommands).complete(teacherId, bookingId, new CompleteBookingRequest(4L,
                new SessionReportRequest("https://files.test/new", "Ôn tập phương trình", "Làm bài tốt", "Luyện thêm", 5)));
    }

    @Test
    void cancelAndDisputeUseTeacherPrincipalAndCurrentVersion() throws Exception {
        mockMvc.perform(post("/teacher/bookings/{id}/cancel", bookingId).param("version", "4").param("reason", "Gia sư bận"))
                .andExpect(status().is3xxRedirection());
        verify(bookingCommands).cancel(teacherId, bookingId, new CancelBookingRequest(4L, "Gia sư bận", CancelInitiatedBy.TEACHER));

        mockMvc.perform(post("/teacher/bookings/{id}/dispute", bookingId).param("version", "4").param("reason", "Thiếu xác nhận"))
                .andExpect(status().is3xxRedirection());
        verify(bookingCommands).dispute(teacherId, bookingId, 4L, "Thiếu xác nhận");
    }

    private Map<String, Object> listItem(String status, boolean trial) {
        return Map.of("id", bookingId, "status", status, "deliveryMode", "ONLINE", "trial", trial,
                "outsideAvailabilityWarning", false, "startTime", Instant.parse("2026-10-29T12:00:00Z"),
                "endTime", Instant.parse("2026-10-29T13:00:00Z"),
                "student", Map.of("fullName", "Nguyễn Minh An"), "subject", Map.of("name", "Đại số"));
    }

    private Map<String, Object> detail(String status, boolean trial, boolean canConfirm, boolean includeReport) {
        Map<String, Object> report = includeReport ? Map.of("content", "Ôn phương trình", "feedback", "Làm tốt",
                "followUpNote", "Luyện thêm", "recordLink", "https://files.test/report", "teacherSelfRating", 5) : Map.of();
        Map<String, Object> settlement = new java.util.LinkedHashMap<>();
        settlement.put("status", "AWAITING_CONFIRMATION");
        settlement.put("teacherConfirmedAt", canConfirm ? null : Instant.parse("2026-10-28T11:01:00Z"));
        settlement.put("studentConfirmedAt", Instant.parse("2026-10-28T11:02:00Z"));
        settlement.put("confirmationDeadline", Instant.parse("2026-10-29T11:00:00Z"));
        return Map.ofEntries(Map.entry("id", bookingId), Map.entry("version", 4L), Map.entry("status", status),
                Map.entry("deliveryMode", "ONLINE"), Map.entry("trial", trial), Map.entry("canConfirm", canConfirm),
                Map.entry("canDispute", false), Map.entry("outsideAvailabilityWarning", false),
                Map.entry("startTime", Instant.parse("2026-10-28T10:00:00Z")),
                Map.entry("endTime", Instant.parse("2026-10-28T11:00:00Z")),
                Map.entry("teacher", Map.of("fullName", "Trần Thu Hà")), Map.entry("student", Map.of("fullName", "Nguyễn Minh An")),
                Map.entry("subject", Map.of("name", "Đại số")), Map.entry("sessionReport", report),
                Map.entry("settlementStatus", "AWAITING_CONFIRMATION"), Map.entry("settlement", settlement),
                Map.entry("meetingLink", "https://meeting.test/room"));
    }
}
