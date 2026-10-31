package com.edtech.platform.communication.web;

import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.communication.dto.notification.NotificationView;
import com.edtech.platform.communication.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class NotificationPageController {
    private static final Logger log = LoggerFactory.getLogger(NotificationPageController.class);
    private static final int PAGE_SIZE = 20;
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))
            .withZone(VIETNAM_ZONE);
    private static final List<FilterOption> FILTERS = List.of(
            new FilterOption("ALL", "Tất cả"), new FilterOption("UNREAD", "Chưa đọc"),
            new FilterOption("BOOKING", "Lịch học"), new FilterOption("ASSIGNMENT", "Bài tập"),
            new FilterOption("FINANCE", "Tài chính"), new FilterOption("SYSTEM", "Hệ thống"));

    private final NotificationService notifications;

    @GetMapping("/student/notifications")
    public String studentList(@AuthenticationPrincipal AuthenticatedUser user,
                              @RequestParam(defaultValue = "ALL") String filter,
                              @RequestParam(defaultValue = "0") int page, Model model) {
        requireRole(user, "STUDENT");
        return render(user, "STUDENT", filter, page, model);
    }

    @GetMapping("/teacher/notifications")
    public String teacherList(@AuthenticationPrincipal AuthenticatedUser user,
                              @RequestParam(defaultValue = "ALL") String filter,
                              @RequestParam(defaultValue = "0") int page, Model model) {
        requireRole(user, "TEACHER");
        return render(user, "TEACHER", filter, page, model);
    }

    @GetMapping("/admin/notifications")
    public String adminList(@AuthenticationPrincipal AuthenticatedUser user,
                            @RequestParam(defaultValue = "ALL") String filter,
                            @RequestParam(defaultValue = "0") int page, Model model) {
        requireRole(user, "ADMIN");
        return render(user, "ADMIN", filter, page, model);
    }

    @PostMapping("/student/notifications/{id}/read")
    public String markStudentRead(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                  @RequestParam(defaultValue = "ALL") String filter,
                                  @RequestParam(defaultValue = "0") int page, RedirectAttributes redirect) {
        requireRole(user, "STUDENT");
        markRead(id, user, redirect);
        return redirect("STUDENT", filter, page);
    }

    @PostMapping("/teacher/notifications/{id}/read")
    public String markTeacherRead(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                  @RequestParam(defaultValue = "ALL") String filter,
                                  @RequestParam(defaultValue = "0") int page, RedirectAttributes redirect) {
        requireRole(user, "TEACHER");
        markRead(id, user, redirect);
        return redirect("TEACHER", filter, page);
    }

    @PostMapping("/admin/notifications/{id}/read")
    public String markAdminRead(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                @RequestParam(defaultValue = "ALL") String filter,
                                @RequestParam(defaultValue = "0") int page, RedirectAttributes redirect) {
        requireRole(user, "ADMIN");
        markRead(id, user, redirect);
        return redirect("ADMIN", filter, page);
    }

    @PostMapping("/student/notifications/read-all")
    public String markStudentAllRead(@AuthenticationPrincipal AuthenticatedUser user,
                                     @RequestParam(defaultValue = "ALL") String filter,
                                     RedirectAttributes redirect) {
        requireRole(user, "STUDENT");
        markAllRead(user, redirect);
        return redirect("STUDENT", filter, 0);
    }

    @PostMapping("/teacher/notifications/read-all")
    public String markTeacherAllRead(@AuthenticationPrincipal AuthenticatedUser user,
                                     @RequestParam(defaultValue = "ALL") String filter,
                                     RedirectAttributes redirect) {
        requireRole(user, "TEACHER");
        markAllRead(user, redirect);
        return redirect("TEACHER", filter, 0);
    }

    @PostMapping("/admin/notifications/read-all")
    public String markAdminAllRead(@AuthenticationPrincipal AuthenticatedUser user,
                                   @RequestParam(defaultValue = "ALL") String filter,
                                   RedirectAttributes redirect) {
        requireRole(user, "ADMIN");
        markAllRead(user, redirect);
        return redirect("ADMIN", filter, 0);
    }

    private String render(AuthenticatedUser user, String role, String rawFilter, int requestedPage, Model model) {
        String filter = normalizeFilter(rawFilter);
        int safePage = Math.max(0, Math.min(requestedPage, 100_000));
        Boolean isRead = "UNREAD".equals(filter) ? false : null;
        String referenceType = List.of("BOOKING", "ASSIGNMENT", "FINANCE", "SYSTEM").contains(filter) ? filter : null;
        String base = "/" + role.toLowerCase(Locale.ROOT) + "/notifications";
        model.addAttribute("role", role);
        model.addAttribute("roleLabel", "STUDENT".equals(role) ? "Học viên" : ("ADMIN".equals(role) ? "Quản trị viên" : "Gia sư"));
        model.addAttribute("basePath", base);
        model.addAttribute("activeWorkspaceNav", "notifications");
        model.addAttribute("filter", filter);
        model.addAttribute("filters", filterOptions(role));
        model.addAttribute("filterInvalid", rawFilter != null && !rawFilter.equalsIgnoreCase(filter));
        model.addAttribute("pageSize", PAGE_SIZE);
        model.addAttribute("notificationsUnavailable", false);
        try {
            Page<NotificationView> result = notifications.getNotifications(user.id(), isRead, referenceType,
                    PageRequest.of(safePage, PAGE_SIZE, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
            if (safePage > 0 && result.getTotalElements() > 0 && result.isEmpty()) {
                return "redirect:" + base + "?filter=" + filter + "&page=" + (result.getTotalPages() - 1);
            }
            model.addAttribute("notificationRows", result.getContent().stream()
                    .map(item -> new NotificationRow(item, dateLabel(item), safeDestination(item, role))).toList());
            model.addAttribute("currentPage", result.getNumber());
            model.addAttribute("totalPages", result.getTotalPages());
            model.addAttribute("totalElements", result.getTotalElements());
            model.addAttribute("hasPrevious", result.hasPrevious());
            model.addAttribute("hasNext", result.hasNext());
        } catch (RuntimeException ex) {
            log.warn("{} notification page is unavailable", role, ex);
            model.addAttribute("notificationsUnavailable", true);
            model.addAttribute("notificationRows", List.of());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalElements", 0L);
            model.addAttribute("hasPrevious", false);
            model.addAttribute("hasNext", false);
        }
        return "workspace/notifications";
    }

    private List<FilterOption> filterOptions(String role) {
        if (!"TEACHER".equals(role)) return FILTERS;
        return FILTERS.stream().map(option -> switch (option.value()) {
            case "BOOKING" -> new FilterOption(option.value(), "Lịch dạy");
            case "FINANCE" -> new FilterOption(option.value(), "Thu nhập");
            default -> option;
        }).toList();
    }

    private String normalizeFilter(String value) {
        if (value == null) return "ALL";
        return FILTERS.stream().map(FilterOption::value).filter(option -> option.equalsIgnoreCase(value.trim()))
                .findFirst().orElse("ALL");
    }

    private void markRead(UUID id, AuthenticatedUser user, RedirectAttributes redirect) {
        try {
            notifications.markAsRead(id, user.id());
            redirect.addFlashAttribute("notice", "Đã đánh dấu thông báo là đã đọc.");
        } catch (RuntimeException ex) {
            log.warn("Unable to mark notification as read", ex);
            redirect.addFlashAttribute("actionError", "Chưa đánh dấu được thông báo. Hãy thử lại.");
        }
    }

    private void markAllRead(AuthenticatedUser user, RedirectAttributes redirect) {
        try {
            notifications.markAllAsRead(user.id());
            redirect.addFlashAttribute("notice", "Đã đánh dấu các thông báo là đã đọc.");
        } catch (RuntimeException ex) {
            log.warn("Unable to mark all notifications as read", ex);
            redirect.addFlashAttribute("actionError", "Chưa đánh dấu được các thông báo. Hãy thử lại.");
        }
    }

    private String safeDestination(NotificationView item, String role) {
        String value = item.getReferenceUrl();
        if (value == null || item.getReferenceId() == null) return null;
        String prefix = "STUDENT".equals(role) ? "/student/" : "/teacher/";
        try {
            URI uri = URI.create(value);
            if (uri.isAbsolute() || uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || !value.startsWith(prefix) || value.startsWith("//")) return null;
            String id = item.getReferenceId().toString();
            if ("STUDENT".equals(role) && List.of("/student/assignments/" + id, "/student/packages/" + id,
                    "/student/payment-result/" + id, "/student/bookings/" + id).contains(value)) return value;
            return null; // Other destination routes are not available in the SSR workspace yet.
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String dateLabel(NotificationView item) {
        return item.getCreatedAt() == null ? "Thời gian chưa xác định" : DATE_TIME.format(item.getCreatedAt());
    }

    private String redirect(String role, String rawFilter, int requestedPage) {
        String filter = normalizeFilter(rawFilter);
        return "redirect:/" + role.toLowerCase(Locale.ROOT) + "/notifications?filter=" + filter + "&page=" + Math.max(0, requestedPage);
    }

    private void requireRole(AuthenticatedUser user, String expectedRole) {
        if (user == null || !expectedRole.equals(user.role())) throw new AccessDeniedException("Wrong workspace role");
    }

    public record FilterOption(String value, String label) {}
    public record NotificationRow(NotificationView data, String dateLabel, String destination) {}
}
