package com.edtech.platform.admin.web;

import com.edtech.platform.admin.dto.response.AdminUserView;
import com.edtech.platform.admin.service.AdminApprovalService;
import com.edtech.platform.admin.service.AdminUserDirectoryService;
import com.edtech.platform.admin.service.AuditContext;
import com.edtech.platform.common.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class AdminUsersPageController {
    private static final Logger log = LoggerFactory.getLogger(AdminUsersPageController.class);
    private static final List<Integer> PAGE_SIZES = List.of(20, 50, 100);
    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "fullName", "email", "role", "status", "lastLoginAt");
    private static final DateTimeFormatter VIETNAM_DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private final AdminUserDirectoryService directory;
    private final AdminApprovalService approvals;

    @ModelAttribute
    void preventPrivatePageCaching(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, max-age=0");
    }

    @GetMapping("/admin/users")
    public String list(@AuthenticationPrincipal AuthenticatedUser actor,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String role,
                       @RequestParam(required = false) String status,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(defaultValue = "createdAt,desc") String sort,
                       @RequestParam(required = false) String sortField,
                       @RequestParam(required = false) String sortDirection,
                       Model model) {
        requireAdmin(actor);
        int safePage = Math.max(0, page);
        int safeSize = PAGE_SIZES.contains(size) ? size : 20;
        String cleanKeyword = keyword == null ? "" : keyword.trim();
        boolean invalidRole = role != null && !role.isBlank() && !Set.of("STUDENT", "TEACHER").contains(role);
        boolean invalidStatus = status != null && !status.isBlank() && !Set.of("ACTIVE", "LOCKED").contains(status);
        String cleanRole = invalidRole || role == null || role.isBlank() ? "" : role;
        String cleanStatus = invalidStatus || status == null || status.isBlank() ? "" : status;
        SortSelection selection = sortSelection(sortField, sortDirection, sort);

        model.addAttribute("keyword", cleanKeyword);
        model.addAttribute("role", cleanRole);
        model.addAttribute("status", cleanStatus);
        model.addAttribute("pageNumber", safePage);
        model.addAttribute("pageSize", safeSize);
        model.addAttribute("sortValue", selection.value());
        model.addAttribute("sortField", selection.field());
        model.addAttribute("sortDirection", selection.direction());
        model.addAttribute("sortFields", List.of(
                new SortOption("createdAt", "Ngày tạo"), new SortOption("fullName", "Họ tên"),
                new SortOption("email", "Email"), new SortOption("role", "Vai trò"),
                new SortOption("status", "Trạng thái"), new SortOption("lastLoginAt", "Lần đăng nhập cuối")));
        model.addAttribute("invalidFilters", invalidRole || invalidStatus || selection.invalid());
        model.addAttribute("invalidKeyword", cleanKeyword.length() > 100);
        model.addAttribute("usersUnavailable", false);
        model.addAttribute("users", List.of());
        model.addAttribute("totalElements", 0L);
        model.addAttribute("totalPages", 0);
        model.addAttribute("hasPrevious", false);
        model.addAttribute("hasNext", false);

        if (cleanKeyword.length() <= 100) {
            try {
                Page<AdminUserView> result = directory.findUsers(emptyToNull(cleanKeyword), emptyToNull(cleanRole),
                        emptyToNull(cleanStatus), PageRequest.of(safePage, safeSize,
                                Sort.by(selection.direction(), selection.field(), "id")));
                model.addAttribute("users", result.getContent().stream().map(UserRow::from).toList());
                model.addAttribute("totalElements", result.getTotalElements());
                model.addAttribute("totalPages", result.getTotalPages());
                model.addAttribute("hasPrevious", result.hasPrevious());
                model.addAttribute("hasNext", result.hasNext());
                if (result.getTotalPages() > 0 && safePage >= result.getTotalPages()) {
                    return "redirect:" + listLocation(cleanKeyword, cleanRole, cleanStatus,
                            result.getTotalPages() - 1, safeSize, selection.value());
                }
            } catch (RuntimeException exception) {
                log.warn("Admin user directory is unavailable");
                model.addAttribute("users", List.of());
                model.addAttribute("totalElements", 0L);
                model.addAttribute("totalPages", 0);
                model.addAttribute("hasPrevious", false);
                model.addAttribute("hasNext", false);
                model.addAttribute("usersUnavailable", true);
            }
        }
        return "workspace/admin-users";
    }

    @PostMapping("/admin/users/{id}/status")
    public String changeStatus(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID id,
                               @RequestParam(required = false) String status,
                               @RequestParam(required = false, defaultValue = "") String reason,
                               @RequestParam(required = false, defaultValue = "") String keyword,
                               @RequestParam(required = false, defaultValue = "") String role,
                               @RequestParam(required = false, defaultValue = "") String statusFilter,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size,
                               @RequestParam(defaultValue = "createdAt,desc") String sort,
                               HttpServletRequest request, RedirectAttributes redirect) {
        requireAdmin(actor);
        String location = listLocation(keyword.trim(), normalizeFilter(role, Set.of("STUDENT", "TEACHER")),
                normalizeFilter(statusFilter, Set.of("ACTIVE", "LOCKED")), Math.max(0, page),
                PAGE_SIZES.contains(size) ? size : 20, sortSelection(null, null, sort).value());
        String cleanReason = reason.trim();
        if (!("ACTIVE".equals(status) || "LOCKED".equals(status)) || cleanReason.isBlank()
                || cleanReason.length() > 1000) {
            redirect.addFlashAttribute("actionError", "Chọn trạng thái hợp lệ và nhập lý do (tối đa 1.000 ký tự).");
            redirect.addFlashAttribute("moderationUserId", id.toString());
            redirect.addFlashAttribute("moderationReason", reason);
            return "redirect:" + location;
        }
        try {
            approvals.changeUserStatus(id, actor.id(), status, cleanReason,
                    new AuditContext(request.getRemoteAddr(), request.getHeader("User-Agent")));
            redirect.addFlashAttribute("actionSuccess", "Đã cập nhật trạng thái tài khoản.");
        } catch (RuntimeException exception) {
            log.warn("Admin user status update failed");
            redirect.addFlashAttribute("actionError", "Không thể cập nhật tài khoản. Có thể trạng thái đã thay đổi; hãy tải lại danh sách.");
            redirect.addFlashAttribute("moderationUserId", id.toString());
            redirect.addFlashAttribute("moderationReason", reason);
        }
        return "redirect:" + location;
    }

    private String listLocation(String keyword, String role, String status, int page, int size, String sort) {
        StringBuilder location = new StringBuilder("/admin/users?page=").append(page).append("&size=").append(size)
                .append("&sort=").append(sort);
        if (!keyword.isBlank()) location.append("&keyword=").append(java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8));
        if (!role.isBlank()) location.append("&role=").append(role);
        if (!status.isBlank()) location.append("&status=").append(status);
        return location.toString();
    }

    private SortSelection sortSelection(String field, String direction, String raw) {
        String candidate = field != null || direction != null
                ? String.valueOf(field) + "," + String.valueOf(direction) : raw;
        if (candidate != null) {
            String[] parts = candidate.split(",", -1);
            if (parts.length == 2 && SORT_FIELDS.contains(parts[0])
                    && ("asc".equalsIgnoreCase(parts[1]) || "desc".equalsIgnoreCase(parts[1]))) {
                Sort.Direction parsedDirection = Sort.Direction.fromString(parts[1]);
                return new SortSelection(parts[0], parsedDirection,
                        parts[0] + "," + parsedDirection.name().toLowerCase(), false);
            }
        }
        return new SortSelection("createdAt", Sort.Direction.DESC, "createdAt,desc",
                candidate != null && !candidate.isBlank());
    }

    private String normalizeFilter(String value, Set<String> allowed) {
        return value == null || value.isBlank() || !allowed.contains(value) ? "" : value;
    }

    private String emptyToNull(String value) { return value == null || value.isBlank() ? null : value; }

    private void requireAdmin(AuthenticatedUser actor) {
        if (actor == null || !"ADMIN".equals(actor.role())) throw new AccessDeniedException("Admin workspace only");
    }

    private record SortSelection(String field, Sort.Direction direction, String value, boolean invalid) {}
    public record SortOption(String value, String label) {}
    public record UserRow(AdminUserView data, String createdAt, String lastLoginAt) {
        static UserRow from(AdminUserView user) {
            return new UserRow(user, format(user.createdAt()), user.lastLoginAt() == null
                    ? "Chưa đăng nhập" : format(user.lastLoginAt()));
        }
        private static String format(Instant value) { return value == null ? "Chưa có dữ liệu" : VIETNAM_DATE_TIME.format(value); }
    }
}
