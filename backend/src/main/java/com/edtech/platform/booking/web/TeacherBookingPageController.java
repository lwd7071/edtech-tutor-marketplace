package com.edtech.platform.booking.web;

import com.edtech.platform.booking.domain.BookingStatus;
import com.edtech.platform.booking.domain.CancelInitiatedBy;
import com.edtech.platform.booking.dto.request.CancelBookingRequest;
import com.edtech.platform.booking.dto.request.CompleteBookingRequest;
import com.edtech.platform.booking.dto.request.SessionReportRequest;
import com.edtech.platform.booking.service.BookingReadService;
import com.edtech.platform.booking.service.BookingService;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class TeacherBookingPageController {
    private static final Logger log = LoggerFactory.getLogger(TeacherBookingPageController.class);
    private static final int PAGE_SIZE = 10;
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter
            .ofPattern("EEEE, dd/MM/yyyy · HH:mm", Locale.forLanguageTag("vi-VN"))
            .withZone(VIETNAM_ZONE);

    private final BookingReadService bookingReads;
    private final BookingService bookingCommands;
    private final Clock clock;

    @GetMapping("/teacher/bookings")
    public String list(@AuthenticationPrincipal AuthenticatedUser user,
                       @RequestParam(defaultValue = "ALL") String status,
                       @RequestParam(required = false) String from,
                       @RequestParam(required = false) String to,
                       @RequestParam(defaultValue = "0") int page,
                       Model model, HttpServletResponse response) {
        requireTeacher(user);
        noStore(response);
        String filter = normalizeStatus(status);
        int safePage = Math.max(0, Math.min(page, 100_000));
        LocalDate fromDay = parseDate(from);
        LocalDate toDay = parseDate(to);
        boolean dateFilterInvalid = (from != null && !from.isBlank() && fromDay == null)
                || (to != null && !to.isBlank() && toDay == null)
                || (fromDay != null && toDay != null && fromDay.isAfter(toDay));
        if (dateFilterInvalid) { fromDay = null; toDay = null; }
        Instant fromInstant = null;
        Instant toInstant = null;
        if (!dateFilterInvalid) {
            try {
                fromInstant = fromDay == null ? null : fromDay.atStartOfDay(VIETNAM_ZONE).toInstant();
                toInstant = toDay == null ? null : toDay.plusDays(1).atStartOfDay(VIETNAM_ZONE).toInstant();
            } catch (DateTimeException ex) {
                dateFilterInvalid = true;
                fromDay = null;
                toDay = null;
            }
        }
        model.addAttribute("activeWorkspaceNav", "bookings");
        model.addAttribute("bookingFilter", filter);
        model.addAttribute("fromDate", fromDay == null ? "" : fromDay.toString());
        model.addAttribute("toDate", toDay == null ? "" : toDay.toString());
        model.addAttribute("dateFilterInvalid", dateFilterInvalid);
        boolean statusFilterInvalid = !"ALL".equalsIgnoreCase(status)
                && statusOptions().stream().noneMatch(option -> option.value().equalsIgnoreCase(status));
        model.addAttribute("hasFilters", !"ALL".equals(filter) || fromDay != null || toDay != null
                || dateFilterInvalid || statusFilterInvalid);
        model.addAttribute("bookingStatuses", statusOptions());
        model.addAttribute("filterInvalid", statusFilterInvalid);
        model.addAttribute("bookingsUnavailable", false);
        try {
            BookingStatus selected = "ALL".equals(filter) ? null : BookingStatus.valueOf(filter);
            Page<Map<String, Object>> result = bookingReads.listTeacherWorkspace(user.id(), selected, fromInstant, toInstant,
                    PageRequest.of(safePage, PAGE_SIZE, Sort.by(Sort.Order.desc("startTime"), Sort.Order.desc("id"))));
            if (safePage > 0 && result.getTotalElements() > 0 && result.isEmpty()) {
                return "redirect:/teacher/bookings?status=" + filter + "&from=" + (fromDay == null ? "" : fromDay)
                        + "&to=" + (toDay == null ? "" : toDay) + "&page=" + (result.getTotalPages() - 1);
            }
            model.addAttribute("bookingRows", result.getContent().stream().map(this::row).toList());
            model.addAttribute("currentPage", result.getNumber());
            model.addAttribute("totalPages", result.getTotalPages());
            model.addAttribute("hasPrevious", result.hasPrevious());
            model.addAttribute("hasNext", result.hasNext());
            model.addAttribute("totalBookings", result.getTotalElements());
        } catch (RuntimeException ex) {
            log.warn("Teacher booking list is unavailable");
            model.addAttribute("bookingsUnavailable", true);
            model.addAttribute("bookingRows", List.of());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("hasPrevious", false);
            model.addAttribute("hasNext", false);
            model.addAttribute("totalBookings", 0L);
        }
        return "workspace/teacher-bookings";
    }

    @GetMapping("/teacher/bookings/{id}")
    public String detail(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                         Model model, HttpServletResponse response) {
        requireTeacher(user);
        noStore(response);
        return renderDetail(user, id, model, response);
    }

    @PostMapping("/teacher/bookings/{id}/complete")
    public String complete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                           @RequestParam long version, @RequestParam(required = false) String recordLink,
                           @RequestParam(required = false) String content, @RequestParam(required = false) String feedback,
                           @RequestParam(required = false) String followUpNote,
                           @RequestParam(required = false) Integer teacherSelfRating,
                           RedirectAttributes redirect) {
        requireTeacher(user);
        ReportForm form = new ReportForm(version, recordLink, content, feedback, followUpNote, teacherSelfRating);
        if (content == null || content.isBlank()) {
            redirect.addFlashAttribute("bookingActionError", "Nhập nội dung báo cáo buổi học.");
            redirect.addFlashAttribute("bookingActionFocus", "report");
            redirect.addFlashAttribute("reportForm", form);
            return redirectTo(id);
        }
        if (teacherSelfRating != null && (teacherSelfRating < 1 || teacherSelfRating > 5)) {
            redirect.addFlashAttribute("bookingActionError", "Điểm tự đánh giá phải từ 1 đến 5.");
            redirect.addFlashAttribute("bookingActionFocus", "report");
            redirect.addFlashAttribute("reportForm", form);
            return redirectTo(id);
        }
        try {
            bookingCommands.complete(user.id(), id, new CompleteBookingRequest(version,
                    new SessionReportRequest(recordLink, content, feedback, followUpNote, teacherSelfRating)));
            redirect.addFlashAttribute("bookingNotice", "Đã gửi báo cáo buổi học.");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("bookingActionError", errorMessage(ex.getErrorCode()));
            redirect.addFlashAttribute("bookingActionFocus", "report");
            redirect.addFlashAttribute("reportForm", form);
        } catch (RuntimeException ex) {
            log.warn("Teacher booking completion failed");
            redirect.addFlashAttribute("bookingActionError", "Chưa gửi được báo cáo. Hãy tải lại trang rồi thử lại.");
            redirect.addFlashAttribute("bookingActionFocus", "report");
            redirect.addFlashAttribute("reportForm", form);
        }
        return redirectTo(id);
    }

    @PostMapping("/teacher/bookings/{id}/cancel")
    public String cancel(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                         @RequestParam long version, @RequestParam(required = false) String reason,
                         RedirectAttributes redirect) {
        requireTeacher(user);
        CancelForm form = new CancelForm(version, reason);
        if (reason == null || reason.isBlank() || reason.length() > 1000) {
            redirect.addFlashAttribute("bookingActionError", reason != null && reason.length() > 1000
                    ? "Lý do hủy không được dài quá 1.000 ký tự." : "Nhập lý do hủy buổi học.");
            redirect.addFlashAttribute("bookingActionFocus", "cancel");
            redirect.addFlashAttribute("cancelForm", form);
            return redirectTo(id);
        }
        try {
            bookingCommands.cancel(user.id(), id,
                    new CancelBookingRequest(version, reason, CancelInitiatedBy.TEACHER));
            redirect.addFlashAttribute("bookingNotice", "Đã hủy buổi học.");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("bookingActionError", errorMessage(ex.getErrorCode()));
            redirect.addFlashAttribute("bookingActionFocus", "cancel");
            redirect.addFlashAttribute("cancelForm", form);
        } catch (RuntimeException ex) {
            log.warn("Teacher booking cancellation failed");
            redirect.addFlashAttribute("bookingActionError", "Chưa hủy được buổi học. Hãy tải lại trang rồi thử lại.");
            redirect.addFlashAttribute("bookingActionFocus", "cancel");
            redirect.addFlashAttribute("cancelForm", form);
        }
        return redirectTo(id);
    }

    @PostMapping("/teacher/bookings/{id}/dispute")
    public String dispute(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                          @RequestParam long version, @RequestParam(required = false) String reason,
                          RedirectAttributes redirect) {
        requireTeacher(user);
        DisputeForm form = new DisputeForm(version, reason);
        if (reason == null || reason.isBlank()) {
            redirect.addFlashAttribute("bookingActionError", "Nhập lý do khiếu nại quyết toán.");
            redirect.addFlashAttribute("bookingActionFocus", "dispute");
            redirect.addFlashAttribute("disputeForm", form);
            return redirectTo(id);
        }
        try {
            bookingCommands.dispute(user.id(), id, version, reason);
            redirect.addFlashAttribute("bookingNotice", "Đã gửi khiếu nại quyết toán.");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("bookingActionError", errorMessage(ex.getErrorCode()));
            redirect.addFlashAttribute("bookingActionFocus", "dispute");
            redirect.addFlashAttribute("disputeForm", form);
        } catch (RuntimeException ex) {
            log.warn("Teacher booking dispute failed");
            redirect.addFlashAttribute("bookingActionError", "Chưa gửi được khiếu nại. Hãy tải lại trang rồi thử lại.");
            redirect.addFlashAttribute("bookingActionFocus", "dispute");
            redirect.addFlashAttribute("disputeForm", form);
        }
        return redirectTo(id);
    }

    private String renderDetail(AuthenticatedUser user, UUID id, Model model, HttpServletResponse response) {
        model.addAttribute("activeWorkspaceNav", "bookings");
        model.addAttribute("bookingId", id);
        model.addAttribute("bookingNotFound", false);
        model.addAttribute("bookingUnavailable", false);
        try {
            Map<String, Object> source = bookingReads.detail(user.id(), id, true);
            model.addAttribute("booking", detailView(source));
        } catch (BusinessException ex) {
            if (ex.getErrorCode() == ErrorCode.RESOURCE_NOT_FOUND || ex.getErrorCode() == ErrorCode.BOOKING_NOT_FOUND) {
                response.setStatus(HttpStatus.NOT_FOUND.value());
                model.addAttribute("bookingNotFound", true);
            } else {
                log.warn("Teacher booking detail is unavailable");
                model.addAttribute("bookingUnavailable", true);
            }
        } catch (RuntimeException ex) {
            log.warn("Teacher booking detail is unavailable");
            model.addAttribute("bookingUnavailable", true);
        }
        model.addAttribute("reportForm", model.getAttribute("reportForm") == null ? ReportForm.empty() : model.getAttribute("reportForm"));
        model.addAttribute("cancelForm", model.getAttribute("cancelForm") == null ? CancelForm.empty() : model.getAttribute("cancelForm"));
        model.addAttribute("disputeForm", model.getAttribute("disputeForm") == null ? DisputeForm.empty() : model.getAttribute("disputeForm"));
        return "workspace/teacher-booking-detail";
    }

    private BookingRow row(Map<String, Object> value) {
        Map<String, Object> student = nested(value.get("student"));
        Map<String, Object> subject = nested(value.get("subject"));
        return new BookingRow((UUID) value.get("id"), name(student, "fullName", "Học viên"),
                name(subject, "name", "Môn học"), dateLabel(value.get("startTime")),
                dateLabel(value.get("endTime")), String.valueOf(value.get("status")), statusLabel(value.get("status")), modeLabel(value.get("deliveryMode")),
                Boolean.TRUE.equals(value.get("trial")), Boolean.TRUE.equals(value.get("outsideAvailabilityWarning")));
    }

    private BookingDetailView detailView(Map<String, Object> value) {
        Map<String, Object> student = nested(value.get("student"));
        Map<String, Object> subject = nested(value.get("subject"));
        Map<String, Object> report = nested(value.get("sessionReport"));
        Instant start = value.get("startTime") instanceof Instant instant ? instant : null;
        Instant end = value.get("endTime") instanceof Instant instant ? instant : null;
        String status = String.valueOf(value.get("status"));
        boolean trial = Boolean.TRUE.equals(value.get("trial"));
        boolean hasReport = !report.isEmpty();
        boolean canSubmitReport = !hasReport && (trial
                ? "SCHEDULED".equals(status) && end != null && !end.isAfter(Instant.now(clock))
                : Boolean.TRUE.equals(value.get("canConfirm")));
        return new BookingDetailView((UUID) value.get("id"), number(value.get("version")),
                name(student, "fullName", "Học viên"), name(subject, "name", "Môn học"),
                dateLabel(start), dateLabel(end), status, statusLabel(status), modeLabel(value.get("deliveryMode")),
                trial, Boolean.TRUE.equals(value.get("outsideAvailabilityWarning")),
                safeHttpUrl(value.get("meetingLink")), text(value.get("locationAddress")), text(value.get("cancelReason")),
                Boolean.TRUE.equals(value.get("canDispute")), "SCHEDULED".equals(status), canSubmitReport,
                text(report.get("recordLink")), safeHttpUrl(report.get("recordLink")), text(report.get("content")),
                text(report.get("feedback")), text(report.get("followUpNote")),
                report.get("teacherSelfRating") == null ? null : number(report.get("teacherSelfRating")),
                value.get("settlementStatus") == null ? null : String.valueOf(value.get("settlementStatus")),
                settlementLabel(value.get("settlementStatus")), nested(value.get("settlement")));
    }

    private String errorMessage(ErrorCode code) {
        return switch (code) {
            case CONCURRENT_MODIFICATION -> "Buổi học vừa thay đổi. Tải lại trang để xem thông tin mới.";
            case RESOURCE_NOT_FOUND, BOOKING_NOT_FOUND -> "Không tìm thấy buổi học thuộc tài khoản của bạn.";
            case BOOKING_NOT_ENDED, BOOKING_CONFIRMATION_TOO_EARLY -> "Chưa đến thời điểm xác nhận buổi học.";
            case BOOKING_CONFIRMATION_EXPIRED -> "Đã hết thời hạn xác nhận buổi học.";
            case BOOKING_CONFIRMATION_ALREADY_EXISTS -> "Bạn đã gửi xác nhận cho buổi học này.";
            case BOOKING_REPORT_REQUIRED -> "Nhập nội dung báo cáo buổi học.";
            case SESSION_REPORT_ALREADY_EXISTS -> "Buổi học đã có báo cáo.";
            case BOOKING_CANCEL_REASON_REQUIRED -> "Nhập lý do hủy buổi học.";
            case BOOKING_DISPUTE_INVALID_STATE -> "Buổi học hiện không ở trạng thái có thể khiếu nại.";
            case BOOKING_DISPUTE_REASON_REQUIRED -> "Nhập lý do khiếu nại quyết toán.";
            case BOOKING_INVALID_STATE -> "Buổi học hiện không thể thực hiện thao tác này.";
            default -> "Chưa thực hiện được thao tác. Tải lại trang rồi thử lại.";
        };
    }

    private List<StatusOption> statusOptions() {
        return List.of(new StatusOption("ALL", "Tất cả trạng thái"),
                new StatusOption("SCHEDULED", "Đã lên lịch"), new StatusOption("COMPLETED", "Đã hoàn thành"),
                new StatusOption("CANCELLED", "Đã hủy"), new StatusOption("EXPIRED", "Đã hết hạn"));
    }

    private String normalizeStatus(String raw) {
        if (raw == null || raw.isBlank() || "ALL".equalsIgnoreCase(raw.trim())) return "ALL";
        try { return BookingStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT)).name(); }
        catch (IllegalArgumentException ex) { return "ALL"; }
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return LocalDate.parse(raw.trim()); }
        catch (DateTimeException ex) { return null; }
    }

    private String statusLabel(Object value) {
        if (value == null) return "Chưa xác định";
        return switch (String.valueOf(value)) {
            case "SCHEDULED" -> "Đã lên lịch";
            case "COMPLETED" -> "Đã hoàn thành";
            case "CANCELLED" -> "Đã hủy";
            case "EXPIRED" -> "Đã hết hạn";
            default -> "Chưa xác định";
        };
    }

    private String modeLabel(Object value) {
        return "ONLINE".equals(String.valueOf(value)) ? "Trực tuyến" : "Trực tiếp";
    }

    private String settlementLabel(Object value) {
        if (value == null) return "";
        return switch (String.valueOf(value)) {
            case "AWAITING_CONFIRMATION" -> "Đang chờ xác nhận";
            case "HELD" -> "Đang giữ để quyết toán";
            case "DISPUTE_PENDING" -> "Đang xem xét khiếu nại";
            case "REOPENED" -> "Đã mở lại xác nhận";
            case "AWAITING_ADMIN_DECISION" -> "Chờ Admin quyết định";
            case "RELEASED" -> "Đã quyết toán";
            case "RETAINED" -> "Đã giữ lại";
            default -> "Chưa xác định";
        };
    }

    private String dateLabel(Object value) {
        return value instanceof Instant instant ? DATE_TIME.format(instant) : "Chưa có thời gian";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> nested(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private String name(Map<String, Object> source, String key, String fallback) {
        Object value = source.get(key);
        return value instanceof String text && !text.isBlank() ? text : fallback;
    }

    private String text(Object value) { return value instanceof String text ? text : ""; }

    private long number(Object value) { return value instanceof Number number ? number.longValue() : 0L; }

    private String safeHttpUrl(Object value) {
        if (!(value instanceof String url) || url.isBlank()) return null;
        try {
            URI uri = URI.create(url);
            if (uri.getHost() != null && uri.getRawUserInfo() == null
                    && ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())))
                return uri.toASCIIString();
        } catch (IllegalArgumentException ignored) { /* Keep unsafe values out of links. */ }
        return null;
    }

    private String redirectTo(UUID id) { return "redirect:/teacher/bookings/" + id; }

    private void noStore(HttpServletResponse response) { response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store"); }

    private void requireTeacher(AuthenticatedUser user) {
        if (user == null || !"TEACHER".equals(user.role())) throw new AccessDeniedException("Teacher workspace only");
    }

    public record StatusOption(String value, String label) {}
    public record BookingRow(UUID id, String studentName, String subjectName, String startLabel, String endLabel,
                             String statusCode, String statusLabel, String modeLabel, boolean trial, boolean outsideAvailabilityWarning) {}
    public record BookingDetailView(UUID id, long version, String studentName, String subjectName,
                                    String startLabel, String endLabel, String statusCode, String statusLabel,
                                    String modeLabel, boolean trial, boolean outsideAvailabilityWarning,
                                    String meetingLink, String locationAddress, String cancelReason,
                                    boolean canDispute, boolean canCancel, boolean canSubmitReport,
                                    String reportRecordText, String reportRecordLink, String reportContent,
                                    String reportFeedback, String reportFollowUpNote, Long teacherSelfRating,
                                    String settlementStatus, String settlementStatusLabel, Map<String, Object> settlement) {}
    public record ReportForm(long version, String recordLink, String content, String feedback,
                             String followUpNote, Integer teacherSelfRating) {
        public static ReportForm empty() { return new ReportForm(0, "", "", "", "", null); }
    }
    public record CancelForm(long version, String reason) {
        public static CancelForm empty() { return new CancelForm(0, ""); }
    }
    public record DisputeForm(long version, String reason) {
        public static DisputeForm empty() { return new DisputeForm(0, ""); }
    }
}
