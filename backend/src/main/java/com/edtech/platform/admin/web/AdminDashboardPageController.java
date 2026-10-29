package com.edtech.platform.admin.web;

import com.edtech.platform.admin.dto.response.AdminDashboardView;
import com.edtech.platform.admin.service.AdminDashboardService;
import com.edtech.platform.common.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.text.NumberFormat;
import java.util.Locale;

@Controller
@RequiredArgsConstructor
public class AdminDashboardPageController {
    private static final Logger log = LoggerFactory.getLogger(AdminDashboardPageController.class);

    private final AdminDashboardService dashboardService;

    @ModelAttribute
    void preventPrivatePageCaching(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, max-age=0");
    }

    @GetMapping({"/admin", "/admin/dashboard"})
    public String dashboard(@AuthenticationPrincipal AuthenticatedUser user, Model model) {
        requireAdmin(user);
        model.addAttribute("activeWorkspaceNav", "dashboard");
        model.addAttribute("dashboardUnavailable", false);
        try {
            AdminDashboardView dashboard = dashboardService.getDashboard();
            model.addAttribute("dashboard", dashboard);
            model.addAttribute("totalGmvLabel", money(dashboard.totalGmvVnd()));
            model.addAttribute("totalCommissionLabel", money(dashboard.totalCommissionVnd()));
            model.addAttribute("pendingPayoutAmountLabel", money(dashboard.pendingPayoutsAmountVnd()));
            model.addAttribute("totalTeachersLabel", count(dashboard.totalTeachers()));
            model.addAttribute("totalStudentsLabel", count(dashboard.totalStudents()));
            model.addAttribute("totalBookingsLabel", count(dashboard.totalBookings()));
            model.addAttribute("completedBookingsLabel", count(dashboard.completedBookings()));
            model.addAttribute("scheduledBookingsLabel", count(dashboard.scheduledBookings()));
            model.addAttribute("cancelledBookingsLabel", count(dashboard.cancelledBookings()));
        } catch (RuntimeException ex) {
            log.warn("Admin dashboard is unavailable");
            model.addAttribute("dashboard", null);
            model.addAttribute("dashboardUnavailable", true);
        }
        return "workspace/admin-dashboard";
    }

    private void requireAdmin(AuthenticatedUser user) {
        if (user == null || !"ADMIN".equals(user.role())) {
            throw new AccessDeniedException("Admin workspace only");
        }
    }

    private String money(long amount) {
        return NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(amount) + " ₫";
    }

    private String count(long value) {
        return NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(value);
    }
}
