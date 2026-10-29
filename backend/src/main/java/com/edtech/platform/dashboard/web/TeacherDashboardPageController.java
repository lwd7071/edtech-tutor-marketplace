package com.edtech.platform.dashboard.web;

import com.edtech.platform.booking.domain.BookingStatus;
import com.edtech.platform.booking.domain.BookingRequestStatus;
import com.edtech.platform.booking.service.BookingRequestService;
import com.edtech.platform.booking.service.BookingReadService;
import com.edtech.platform.booking.service.TrialRequestService;
import com.edtech.platform.booking.web.TeacherBookingRequestRow;
import com.edtech.platform.booking.web.TeacherTrialRequestRow;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.finance.dto.response.WalletView;
import com.edtech.platform.finance.service.WalletQueryService;
import com.edtech.platform.learning.dto.response.SubmissionDetail;
import com.edtech.platform.learning.service.AssignmentReadService;
import com.edtech.platform.teacher.domain.ProfileStatus;
import com.edtech.platform.teacher.dto.TeacherProfileDetail;
import com.edtech.platform.teacher.service.TeacherProfileService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.text.NumberFormat;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class TeacherDashboardPageController {
    private static final Logger log = LoggerFactory.getLogger(TeacherDashboardPageController.class);
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter
            .ofPattern("EEEE, dd/MM/yyyy · HH:mm", Locale.forLanguageTag("vi-VN"))
            .withZone(VIETNAM_ZONE);
    private static final Sort UPCOMING_SORT = Sort.by(Sort.Order.asc("startTime"), Sort.Order.asc("id"));
    private static final Sort RECENT_SORT = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final TeacherProfileService profiles;
    private final BookingRequestService bookingRequests;
    private final BookingReadService bookings;
    private final TrialRequestService trials;
    private final AssignmentReadService assignments;
    private final WalletQueryService wallets;
    private final Clock clock;

    @ModelAttribute
    void preventPrivatePageCaching(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, max-age=0");
    }

    @GetMapping("/teacher")
    public String dashboard(@AuthenticationPrincipal AuthenticatedUser user, Model model) {
        model.addAttribute("activeWorkspaceNav", "overview");
        addProfile(user.id(), model);
        addPendingBookingRequests(user.id(), model);
        addUpcomingBookings(user.id(), model);
        addPendingTrials(user.id(), model);
        addAssignmentsWithSubmissions(user.id(), model);
        addWallet(user.id(), model);
        return "workspace/teacher-dashboard";
    }

    private void addPendingBookingRequests(UUID teacherUserId, Model model) {
        model.addAttribute("bookingRequestsUnavailable", false);
        try {
            Page<TeacherBookingRequestRow> page = bookingRequests.findTeacherWorkspace(teacherUserId,
                    BookingRequestStatus.PENDING, PageRequest.of(0, 3, RECENT_SORT));
            List<BookingRequestRow> rows = page.getContent().stream().map(row -> new BookingRequestRow(
                    row.request().id().toString(), row.studentName(), row.subjectName(),
                    DATE_TIME.format(row.request().preferredStartTime()))).toList();
            model.addAttribute("bookingRequestRows", rows);
            model.addAttribute("pendingBookingRequestCount", page.getTotalElements());
        } catch (RuntimeException ex) {
            log.warn("Teacher booking requests are unavailable");
            model.addAttribute("bookingRequestsUnavailable", true);
            model.addAttribute("bookingRequestRows", List.of());
            model.addAttribute("pendingBookingRequestCount", 0L);
        }
    }

    private void addProfile(UUID teacherUserId, Model model) {
        model.addAttribute("profileUnavailable", false);
        try {
            TeacherProfileDetail profile = profiles.getProfile(teacherUserId);
            ProfileStatus status = profile.profileStatus();
            model.addAttribute("profileStatusLabel", profileStatusLabel(status));
            model.addAttribute("profileActionLabel", status == ProfileStatus.APPROVED ? "Xem hồ sơ" : "Tiếp tục hồ sơ");
            model.addAttribute("profileRejected", status == ProfileStatus.REJECTED);
            model.addAttribute("profileRejectionReason", status == ProfileStatus.REJECTED ? profile.rejectionReason() : null);
        } catch (RuntimeException ex) {
            log.warn("Teacher dashboard profile is unavailable");
            model.addAttribute("profileUnavailable", true);
        }
    }

    private void addUpcomingBookings(UUID teacherUserId, Model model) {
        model.addAttribute("bookingsUnavailable", false);
        try {
            Page<Map<String, Object>> page = bookings.findUpcomingTeacherWorkspace(teacherUserId, clock.instant(),
                    PageRequest.of(0, 5, UPCOMING_SORT));
            List<BookingRow> rows = page.getContent().stream().map(this::bookingRow).toList();
            model.addAttribute("bookingRows", rows);
            model.addAttribute("nextBooking", rows.isEmpty() ? null : rows.getFirst());
        } catch (RuntimeException ex) {
            log.warn("Teacher dashboard bookings are unavailable");
            model.addAttribute("bookingsUnavailable", true);
            model.addAttribute("bookingRows", List.of());
            model.addAttribute("nextBooking", null);
        }
    }

    private BookingRow bookingRow(Map<String, Object> data) {
        Map<?, ?> student = nestedMap(data.get("student"));
        Map<?, ?> subject = nestedMap(data.get("subject"));
        Instant start = instant(data.get("startTime"));
        Object id = data.get("id");
        if (id == null || start == null) throw new IllegalStateException("Upcoming booking read view is incomplete");
        String mode = String.valueOf(data.get("deliveryMode"));
        return new BookingRow(String.valueOf(id), text(subject.get("name"), "Môn học"),
                text(student.get("fullName"), "Học viên"), start.toString(), DATE_TIME.format(start),
                "ONLINE".equals(mode) ? "Online" : "Trực tiếp");
    }

    private void addPendingTrials(UUID teacherUserId, Model model) {
        model.addAttribute("trialsUnavailable", false);
        try {
            Page<TeacherTrialRequestRow> page = trials.findPendingTeacherWorkspace(teacherUserId,
                    PageRequest.of(0, 3, RECENT_SORT));
            List<TrialRow> rows = page.getContent().stream().map(row -> new TrialRow(row.request().id().toString(),
                    row.studentName(), row.subjectName(), row.request().preferredStartTime().toString(),
                    DATE_TIME.format(row.request().preferredStartTime()))).toList();
            model.addAttribute("trialRows", rows);
            model.addAttribute("pendingTrialCount", page.getTotalElements());
        } catch (RuntimeException ex) {
            log.warn("Teacher dashboard trial requests are unavailable");
            model.addAttribute("trialsUnavailable", true);
            model.addAttribute("trialRows", List.of());
            model.addAttribute("pendingTrialCount", 0L);
        }
    }

    private void addAssignmentsWithSubmissions(UUID teacherUserId, Model model) {
        model.addAttribute("assignmentsUnavailable", false);
        try {
            Page<Map<String, Object>> page = assignments.teacherList(teacherUserId,
                    PageRequest.of(0, 8, RECENT_SORT));
            List<AssignmentRow> rows = page.getContent().stream()
                    .filter(data -> submissionStatus(data) != null)
                    .limit(3)
                    .map(this::assignmentRow)
                    .toList();
            model.addAttribute("assignmentRows", rows);
        } catch (RuntimeException ex) {
            log.warn("Teacher dashboard assignment submissions are unavailable");
            model.addAttribute("assignmentsUnavailable", true);
            model.addAttribute("assignmentRows", List.of());
        }
    }

    private AssignmentRow assignmentRow(Map<String, Object> data) {
        Object id = data.get("id");
        if (id == null) throw new IllegalStateException("Assignment read view is incomplete");
        String status = submissionStatus(data);
        return new AssignmentRow(String.valueOf(id), text(data.get("title"), "Bài tập"),
                text(data.get("studentName"), "Học viên"), text(data.get("subjectName"), "Môn học"),
                "SUBMITTED".equals(status) ? "Chờ chấm" : "Đã chấm");
    }

    private String submissionStatus(Map<String, Object> data) {
        Object raw = data.get("submissions");
        if (!(raw instanceof List<?> list) || list.isEmpty()) return null;
        Object submission = list.getFirst();
        if (submission instanceof SubmissionDetail detail) return detail.getStatus() == null ? null : detail.getStatus().name();
        if (submission instanceof Map<?, ?> map && map.get("status") != null) return String.valueOf(map.get("status"));
        return null;
    }

    private void addWallet(UUID teacherUserId, Model model) {
        model.addAttribute("walletUnavailable", false);
        try {
            WalletView wallet = wallets.getOrCreateWallet(teacherUserId);
            model.addAttribute("availableBalanceLabel", money(wallet.availableBalanceVnd()));
            model.addAttribute("heldBalanceLabel", money(wallet.heldBalanceVnd()));
        } catch (RuntimeException ex) {
            log.warn("Teacher dashboard wallet is unavailable");
            model.addAttribute("walletUnavailable", true);
        }
    }

    private String profileStatusLabel(ProfileStatus status) {
        if (status == null) return "Hồ sơ chưa gửi xét duyệt";
        return switch (status) {
            case DRAFT -> "Hồ sơ chưa gửi xét duyệt";
            case PENDING_APPROVAL -> "Hồ sơ đang chờ xét duyệt";
            case APPROVED -> "Hồ sơ đã được duyệt";
            case REJECTED -> "Hồ sơ cần chỉnh sửa";
        };
    }

    private String money(long amountVnd) {
        return NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(amountVnd) + " ₫";
    }

    private String text(Object value, String fallback) {
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }

    private Map<?, ?> nestedMap(Object value) {
        return value instanceof Map<?, ?> map ? map : Map.of();
    }

    private Instant instant(Object value) {
        if (value instanceof Instant timestamp) return timestamp;
        if (value instanceof String text) return Instant.parse(text);
        return null;
    }

    public record BookingRow(String id, String subjectName, String studentName, String startTime,
                             String dateLabel, String deliveryModeLabel) {}
    public record BookingRequestRow(String id, String studentName, String subjectName, String dateLabel) {}
    public record TrialRow(String id, String studentName, String subjectName, String preferredStartTime,
                           String dateLabel) {}
    public record AssignmentRow(String id, String title, String studentName, String subjectName,
                                String submissionLabel) {}
}
