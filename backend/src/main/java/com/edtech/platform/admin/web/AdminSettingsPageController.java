package com.edtech.platform.admin.web;

import com.edtech.platform.admin.dto.request.UpdatePlatformSettingsRequest;
import com.edtech.platform.admin.dto.response.PlatformSettingsView;
import com.edtech.platform.admin.service.AdminSettingsService;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class AdminSettingsPageController {
    private static final Logger log = LoggerFactory.getLogger(AdminSettingsPageController.class);
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm", Locale.forLanguageTag("vi-VN")).withZone(VIETNAM_ZONE);
    private final AdminSettingsService settings;
    private final Validator validator;

    @ModelAttribute
    void preventPrivatePageCaching(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, max-age=0");
    }

    @GetMapping("/admin/settings")
    public String view(@AuthenticationPrincipal AuthenticatedUser user, Model model) {
        requireAdmin(user);
        model.addAttribute("activeWorkspaceNav", "settings");
        model.addAttribute("settingsUnavailable", false);
        try {
            PlatformSettingsView value = settings.getSettings();
            model.addAttribute("settings", value);
            if (!model.containsAttribute("commissionRateInput")) model.addAttribute("commissionRateInput", value.commissionRate().toPlainString());
            if (!model.containsAttribute("bayesianMinimumReviewsInput")) model.addAttribute("bayesianMinimumReviewsInput", value.bayesianMinimumReviews());
            if (!model.containsAttribute("bookingReminderHoursInput")) model.addAttribute("bookingReminderHoursInput", value.bookingReminderHours());
            if (!model.containsAttribute("bookingExpirationHoursInput")) model.addAttribute("bookingExpirationHoursInput", value.bookingExpirationHours());
            model.addAttribute("updatedAtLabel", dateLabel(value.updatedAt()));
        } catch (RuntimeException exception) {
            log.warn("Admin settings are unavailable");
            model.addAttribute("settings", null);
            model.addAttribute("settingsUnavailable", true);
        }
        return "workspace/admin-settings";
    }

    @PostMapping("/admin/settings")
    public String update(@AuthenticationPrincipal AuthenticatedUser user,
                         @RequestParam String commissionRate,
                         @RequestParam String bayesianMinimumReviews,
                         @RequestParam String bookingReminderHours,
                         @RequestParam String bookingExpirationHours,
                         RedirectAttributes redirect) {
        requireAdmin(user);
        rememberValues(redirect, commissionRate, bayesianMinimumReviews, bookingReminderHours, bookingExpirationHours);
        UpdatePlatformSettingsRequest request;
        try {
            request = new UpdatePlatformSettingsRequest(new BigDecimal(commissionRate.trim()),
                    Integer.parseInt(bayesianMinimumReviews.trim()), Integer.parseInt(bookingReminderHours.trim()),
                    Integer.parseInt(bookingExpirationHours.trim()));
        } catch (RuntimeException exception) {
            redirect.addFlashAttribute("actionError", "Nhập đúng định dạng số cho tất cả trường.");
            return "redirect:/admin/settings";
        }
        Set<ConstraintViolation<UpdatePlatformSettingsRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            redirect.addFlashAttribute("actionError", validationMessage(violations));
            return "redirect:/admin/settings";
        }
        try {
            settings.updateSettings(user.id(), request);
            redirect.addFlashAttribute("actionSuccess", "Đã lưu cài đặt nền tảng.");
            clearValues(redirect);
        } catch (RuntimeException exception) {
            redirect.addFlashAttribute("actionError", exception instanceof BusinessException business
                    ? business.getErrorCode().getDefaultMessage()
                    : "Không thể lưu cài đặt. Hãy tải lại để kiểm tra giá trị hiện tại.");
            log.warn("Admin settings update failed ({})", exception.getClass().getSimpleName());
        }
        return "redirect:/admin/settings";
    }

    private void rememberValues(RedirectAttributes redirect, String commission, String minimumReviews,
                                String reminderHours, String expirationHours) {
        redirect.addFlashAttribute("commissionRateInput", commission);
        redirect.addFlashAttribute("bayesianMinimumReviewsInput", minimumReviews);
        redirect.addFlashAttribute("bookingReminderHoursInput", reminderHours);
        redirect.addFlashAttribute("bookingExpirationHoursInput", expirationHours);
    }
    private void clearValues(RedirectAttributes redirect) {
        redirect.getFlashAttributes().remove("commissionRateInput");
        redirect.getFlashAttributes().remove("bayesianMinimumReviewsInput");
        redirect.getFlashAttributes().remove("bookingReminderHoursInput");
        redirect.getFlashAttributes().remove("bookingExpirationHoursInput");
    }
    private String validationMessage(Set<ConstraintViolation<UpdatePlatformSettingsRequest>> violations) {
        return "Không lưu được cài đặt: " + violations.stream().map(ConstraintViolation::getPropertyPath)
                .map(path -> switch (path.toString()) {
                    case "commissionRate" -> "hoa hồng phải từ 0 đến 100%";
                    case "bayesianMinimumReviews" -> "số đánh giá tối thiểu phải từ 0 trở lên";
                    case "bookingReminderHours" -> "số giờ nhắc lịch phải ít nhất 1";
                    case "bookingExpirationHours" -> "hạn xác nhận lịch phải ít nhất 1 giờ";
                    default -> "kiểm tra lại các giá trị";
                })
                .distinct().sorted().reduce((left, right) -> left + " " + right).orElse("kiểm tra lại các giá trị.");
    }
    private String dateLabel(Instant value) { return value == null ? "Chưa xác định" : DATE_TIME.format(value); }
    private void requireAdmin(AuthenticatedUser user) {
        if (user == null || !"ADMIN".equals(user.role())) throw new AccessDeniedException("Admin workspace only");
    }
}
