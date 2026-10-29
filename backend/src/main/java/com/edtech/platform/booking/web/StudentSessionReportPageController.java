package com.edtech.platform.booking.web;

import com.edtech.platform.booking.dto.response.SessionReportView;
import com.edtech.platform.booking.service.BookingReadService;
import com.edtech.platform.common.security.AuthenticatedUser;
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
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Controller
@RequiredArgsConstructor
public class StudentSessionReportPageController {
    private static final Logger log = LoggerFactory.getLogger(StudentSessionReportPageController.class);
    private static final int PAGE_SIZE = 10;
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("HH:mm, EEEE dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))
            .withZone(VIETNAM_ZONE);

    private final BookingReadService bookingReads;

    @GetMapping("/student/session-reports")
    public String list(@AuthenticationPrincipal AuthenticatedUser user,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        int safePage = Math.max(0, page);
        model.addAttribute("activeWorkspaceNav", "reports");
        model.addAttribute("reportsUnavailable", false);
        model.addAttribute("pageSize", PAGE_SIZE);
        try {
            Page<SessionReportView> result = bookingReads.findStudentSessionReports(user.id(),
                    PageRequest.of(safePage, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "submittedAt")));
            if (safePage > 0 && result.getTotalElements() > 0 && result.isEmpty()) {
                return "redirect:/student/session-reports?page=" + Math.max(0, result.getTotalPages() - 1);
            }
            model.addAttribute("reportRows", result.getContent().stream().map(this::toRow).toList());
            model.addAttribute("currentPage", result.getNumber());
            model.addAttribute("totalPages", result.getTotalPages());
            model.addAttribute("totalElements", result.getTotalElements());
            model.addAttribute("hasPrevious", result.hasPrevious());
            model.addAttribute("hasNext", result.hasNext());
        } catch (RuntimeException ex) {
            log.warn("Student session report page is unavailable", ex);
            model.addAttribute("reportsUnavailable", true);
            model.addAttribute("reportRows", java.util.List.of());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalElements", 0L);
            model.addAttribute("hasPrevious", false);
            model.addAttribute("hasNext", false);
        }
        return "student/session-reports";
    }

    private ReportRow toRow(SessionReportView report) {
        String dateLabel = report.startTime() == null ? "Chưa có thời gian buổi học" : DATE_TIME.format(report.startTime());
        return new ReportRow(report, dateLabel, safeRecordUrl(report.recordLink()));
    }

    private String safeRecordUrl(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            URI uri = URI.create(value);
            if (uri.getHost() != null && uri.getRawUserInfo() == null
                    && ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))) {
                return uri.toASCIIString();
            }
        } catch (IllegalArgumentException ignored) {
            // Malformed URLs are shown as plain text content, never as links.
        }
        return null;
    }

    public record ReportRow(SessionReportView data, String dateLabel, String recordUrl) {}
}
