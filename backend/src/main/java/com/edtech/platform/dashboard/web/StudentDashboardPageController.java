package com.edtech.platform.dashboard.web;

import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.dashboard.dto.response.StudentDashboardView;
import com.edtech.platform.dashboard.service.StudentDashboardService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Controller
@RequiredArgsConstructor
public class StudentDashboardPageController {
    private static final Logger log = LoggerFactory.getLogger(StudentDashboardPageController.class);
    private static final DateTimeFormatter NEXT_LESSON_FORMAT = DateTimeFormatter
            .ofPattern("HH:mm · EEEE, dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private final StudentDashboardService dashboardService;

    @GetMapping("/student")
    public String dashboard(@AuthenticationPrincipal AuthenticatedUser user, Model model) {
        model.addAttribute("activeWorkspaceNav", "overview");
        try {
            StudentDashboardView summary = dashboardService.getDashboard(user.id());
            if (summary == null) throw new IllegalStateException("Dashboard summary is empty");
            model.addAttribute("summary", summary);
            model.addAttribute("nextLessonLabel", summary.nextBookingStartTime() == null
                    ? null : NEXT_LESSON_FORMAT.format(summary.nextBookingStartTime()));
        } catch (RuntimeException ex) {
            log.warn("Student dashboard summary is unavailable", ex);
            model.addAttribute("dashboardUnavailable", true);
        }
        return "student/dashboard";
    }
}
