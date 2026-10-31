package com.edtech.platform.booking.web;

import com.edtech.platform.booking.domain.BookingStatus;
import com.edtech.platform.booking.service.BookingReadService;
import com.edtech.platform.booking.service.BookingService;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class StudentBookingPageController {
    private static final Logger log = LoggerFactory.getLogger(StudentBookingPageController.class);
    private static final int PAGE_SIZE = 10;
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy · HH:mm", Locale.forLanguageTag("vi-VN"))
            .withZone(VIETNAM_ZONE);

    private final BookingReadService bookingReads;
    private final BookingService bookingCommands;

    @GetMapping("/student/bookings")
    public String list(@AuthenticationPrincipal AuthenticatedUser user,
                       @RequestParam(defaultValue = "ALL") String status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        requireStudent(user);
        String filter = normalizeStatus(status);
        int safePage = Math.max(0, Math.min(page, 100_000));
        model.addAttribute("activeWorkspaceNav", "bookings");
        model.addAttribute("bookingFilter", filter);
        model.addAttribute("bookingStatuses", statusOptions());
        model.addAttribute("filterInvalid", !"ALL".equalsIgnoreCase(status) && !statusOptions().stream()
                .map(StatusOption::value).anyMatch(status::equalsIgnoreCase));
        model.addAttribute("bookingsUnavailable", false);
        try {
            BookingStatus selected = "ALL".equals(filter) ? null : BookingStatus.valueOf(filter);
            Page<Map<String, Object>> result = bookingReads.list(user.id(), false, selected, null, null,
                    PageRequest.of(safePage, PAGE_SIZE, Sort.by(Sort.Order.desc("startTime"), Sort.Order.desc("id"))));
            if (safePage > 0 && result.getTotalElements() > 0 && result.isEmpty()) {
                return "redirect:/student/bookings?status=" + filter + "&page=" + (result.getTotalPages() - 1);
            }
            model.addAttribute("bookingRows", result.getContent().stream().map(this::row).toList());
            model.addAttribute("currentPage", result.getNumber());
            model.addAttribute("totalPages", result.getTotalPages());
            model.addAttribute("hasPrevious", result.hasPrevious());
            model.addAttribute("hasNext", result.hasNext());
        } catch (RuntimeException ex) {
            log.warn("Student booking list is unavailable");
            model.addAttribute("bookingsUnavailable", true);
            model.addAttribute("bookingRows", List.of());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("hasPrevious", false);
            model.addAttribute("hasNext", false);
        }
        return "student/bookings";
    }

    @GetMapping("/student/bookings/{id}")
    public String detail(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id, Model model,
                         jakarta.servlet.http.HttpServletResponse response) {
        requireStudent(user);
        model.addAttribute("activeWorkspaceNav", "bookings");
        model.addAttribute("bookingId", id);
        try {
            Map<String, Object> booking = bookingReads.detail(user.id(), id, false);
            model.addAttribute("booking", detail(booking));
            model.addAttribute("bookingUnavailable", false);
            return "student/booking-detail";
        } catch (BusinessException ex) {
            if (ex.getErrorCode() == ErrorCode.RESOURCE_NOT_FOUND || ex.getErrorCode() == ErrorCode.BOOKING_NOT_FOUND) {
                response.setStatus(HttpStatus.NOT_FOUND.value());
                return "student/booking-not-found";
            }
            log.warn("Student booking detail is unavailable");
            model.addAttribute("bookingUnavailable", true);
            return "student/booking-detail";
        } catch (RuntimeException ex) {
            log.warn("Student booking detail is unavailable");
            model.addAttribute("bookingUnavailable", true);
            return "student/booking-detail";
        }
    }

    @PostMapping("/student/bookings/{id}/confirm")
    public String confirm(@AuthenticationPrincipal AuthenticatedUser user,
                          @PathVariable UUID id,
                          @RequestParam long version,
                          RedirectAttributes redirect) {
        requireStudent(user);
        try {
            bookingCommands.confirmByStudent(user.id(), id, version);
            redirect.addFlashAttribute("bookingNotice", "Đã ghi nhận xác nhận buổi học.");
        } catch (BusinessException ex) {
            String message = ex.getErrorCode() == ErrorCode.BOOKING_CONFIRMATION_TOO_EARLY
                    ? "Chưa đến thời điểm xác nhận buổi học."
                    : ex.getErrorCode() == ErrorCode.CONCURRENT_MODIFICATION
                    ? "Thông tin buổi học vừa thay đổi. Hãy tải lại rồi thử lại."
                    : "Chưa xác nhận được buổi học. Hãy kiểm tra trạng thái và thử lại.";
            redirect.addFlashAttribute("bookingActionError", message);
        } catch (RuntimeException ex) {
            log.warn("Student booking confirmation failed");
            redirect.addFlashAttribute("bookingActionError", "Chưa xác nhận được buổi học. Hãy thử lại.");
        }
        return "redirect:/student/bookings/" + id;
    }

    private BookingRow row(Map<String, Object> value) {
        return new BookingRow(value, dateLabel(value.get("startTime")), statusLabel(value.get("status")),
                "ONLINE".equals(String.valueOf(value.get("deliveryMode"))) ? "Trực tuyến" : "Trực tiếp",
                Boolean.TRUE.equals(value.get("trial")));
    }

    @SuppressWarnings("unchecked")
    private BookingDetail detail(Map<String, Object> value) {
        Map<String, Object> teacher = (Map<String, Object>) value.getOrDefault("teacher", Map.of());
        Map<String, Object> subject = (Map<String, Object>) value.getOrDefault("subject", Map.of());
        Map<String, Object> report = value.get("sessionReport") instanceof Map<?, ?> map
                ? (Map<String, Object>) map : Map.of();
        return new BookingDetail(value, dateLabel(value.get("startTime")), dateLabel(value.get("endTime")),
                statusLabel(value.get("status")), "ONLINE".equals(String.valueOf(value.get("deliveryMode"))) ? "Trực tuyến" : "Trực tiếp",
                Boolean.TRUE.equals(value.get("trial")), Boolean.TRUE.equals(value.get("canConfirm")),
                String.valueOf(teacher.getOrDefault("fullName", "Gia sư")),
                String.valueOf(subject.getOrDefault("name", "Môn học")),
                safeMeetingLink(value.get("meetingLink")),
                report);
    }

    private String safeMeetingLink(Object value) {
        if (!(value instanceof String link) || link.isBlank()) return null;
        try {
            URI uri = URI.create(link);
            if (uri.getHost() != null && uri.getRawUserInfo() == null
                    && ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))) {
                return uri.toASCIIString();
            }
        } catch (IllegalArgumentException ignored) {
            // Untrusted values remain escaped text, never clickable links.
        }
        return null;
    }

    private String dateLabel(Object value) {
        if (value instanceof Instant instant) return DATE_TIME.format(instant);
        return "Chưa có thời gian";
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

    private String normalizeStatus(String raw) {
        if (raw == null || raw.isBlank() || "ALL".equalsIgnoreCase(raw.trim())) return "ALL";
        try { return BookingStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT)).name(); }
        catch (IllegalArgumentException ex) { return "ALL"; }
    }

    private List<StatusOption> statusOptions() {
        return List.of(new StatusOption("ALL", "Tất cả trạng thái"), new StatusOption("SCHEDULED", "Đã lên lịch"),
                new StatusOption("COMPLETED", "Đã hoàn thành"), new StatusOption("CANCELLED", "Đã hủy"),
                new StatusOption("EXPIRED", "Đã hết hạn"));
    }

    private void requireStudent(AuthenticatedUser user) {
        if (user == null || !"STUDENT".equals(user.role())) throw new AccessDeniedException("Student workspace only");
    }

    public record StatusOption(String value, String label) {}
    public record BookingRow(Map<String, Object> data, String startLabel, String statusLabel, String modeLabel, boolean trial) {}
    public record BookingDetail(Map<String, Object> data, String startLabel, String endLabel, String statusLabel,
                                String modeLabel, boolean trial, boolean canConfirm, String teacherName,
                                String subjectName, String meetingLinkUrl, Map<String, Object> sessionReport) {}
}
