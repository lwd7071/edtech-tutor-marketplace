package com.edtech.platform.finance.web;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.finance.command.ApproveExtensionCommand;
import com.edtech.platform.finance.command.RejectFinanceCommand;
import com.edtech.platform.finance.dto.response.ExtensionRequestView;
import com.edtech.platform.finance.service.ExtensionService;
import com.edtech.platform.finance.service.FinanceCommandExecutor;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class AdminExtensionsPageController {
    private static final Logger log = LoggerFactory.getLogger(AdminExtensionsPageController.class);
    private static final int PAGE_SIZE = 20;
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm", Locale.forLanguageTag("vi-VN")).withZone(VIETNAM_ZONE);
    private static final DateTimeFormatter LOCAL_INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final DateTimeFormatter LOCAL_VALUE = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm").withZone(VIETNAM_ZONE);
    private static final List<String> FILTERS = List.of("ALL", "PENDING", "APPROVED", "REJECTED");

    private final ExtensionService extensions;
    private final FinanceCommandExecutor commands;

    @ModelAttribute
    void preventPrivatePageCaching(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, max-age=0");
    }

    @GetMapping("/admin/extensions")
    public String list(@AuthenticationPrincipal AuthenticatedUser user,
                       @RequestParam(defaultValue = "ALL") String status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        requireAdmin(user);
        String selected = normalizeStatus(status);
        int safePage = Math.max(0, Math.min(page, 100_000));
        model.addAttribute("activeWorkspaceNav", "extensions");
        model.addAttribute("selectedStatus", selected);
        model.addAttribute("statusOptions", FILTERS.stream().map(value -> new StatusOption(value, statusLabel(value))).toList());
        model.addAttribute("pageNumber", safePage);
        model.addAttribute("pageSize", PAGE_SIZE);
        model.addAttribute("extensionsUnavailable", false);
        model.addAttribute("extensionRows", List.of());
        model.addAttribute("totalElements", 0L);
        model.addAttribute("totalPages", 0);
        model.addAttribute("hasPrevious", false);
        model.addAttribute("hasNext", false);
        try {
            Page<ExtensionRequestView> result = extensions.findAdminExtensions(
                    "ALL".equals(selected) ? null : selected,
                    PageRequest.of(safePage, PAGE_SIZE, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
            if (safePage > 0 && result.getTotalElements() > 0 && result.isEmpty()) {
                return "redirect:/admin/extensions?status=" + selected + "&page=" + Math.max(0, result.getTotalPages() - 1);
            }
            List<ExtensionRow> rows = result.getContent().stream().map(this::row).toList();
            model.addAttribute("extensionRows", rows);
            model.addAttribute("totalElements", result.getTotalElements());
            model.addAttribute("totalPages", result.getTotalPages());
            model.addAttribute("hasPrevious", result.hasPrevious());
            model.addAttribute("hasNext", result.hasNext());
            model.addAttribute("idempotencyKeys", rows.stream().collect(Collectors.toMap(
                    row -> row.data().id().toString(), row -> UUID.randomUUID().toString())));
        } catch (RuntimeException exception) {
            log.warn("Admin extension queue is unavailable");
            model.addAttribute("extensionRows", List.of());
            model.addAttribute("extensionsUnavailable", true);
        }
        return "workspace/admin-extensions";
    }

    @PostMapping("/admin/extensions/{id}/approve")
    public String approve(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                          @RequestParam String approvedExpiryDate, @RequestParam(required = false, defaultValue = "") String adminNote,
                          @RequestParam String idempotencyKey,
                          @RequestParam(defaultValue = "ALL") String status, @RequestParam(defaultValue = "0") int page,
                          RedirectAttributes redirect) {
        requireAdmin(user);
        String destination = location(status, page);
        LocalDateTime localExpiry;
        try { localExpiry = LocalDateTime.parse(approvedExpiryDate, LOCAL_INPUT); }
        catch (RuntimeException exception) { localExpiry = null; }
        String cleanNote = adminNote.trim();
        if (localExpiry == null) {
            redirect.addFlashAttribute("actionError", "Nhập ngày hết hạn mới hợp lệ.");
            redirect.addFlashAttribute("openApproveId", id.toString());
            redirect.addFlashAttribute("approveExpiry", approvedExpiryDate);
            redirect.addFlashAttribute("approveNote", adminNote);
            return "redirect:" + destination;
        }
        try {
            UUID key = UUID.fromString(idempotencyKey);
            Instant expiry = localExpiry.atZone(VIETNAM_ZONE).toInstant();
            ApproveExtensionCommand command = new ApproveExtensionCommand(expiry, blankToNull(cleanNote));
            commands.execute(user.id(), "ADMIN_EXTENSION_APPROVE", key,
                    new ExtensionActionFingerprint(id, command), ExtensionRequestView.class,
                    () -> ApiResponse.ok(extensions.approveExtension(user.id(), id, command)));
            redirect.addFlashAttribute("actionSuccess", "Đã duyệt gia hạn gói học.");
        } catch (RuntimeException exception) {
            logFailure("approval", exception);
            redirect.addFlashAttribute("actionError", safeMessage(exception));
            redirect.addFlashAttribute("openApproveId", id.toString());
            redirect.addFlashAttribute("approveExpiry", approvedExpiryDate);
            redirect.addFlashAttribute("approveNote", adminNote);
        }
        return "redirect:" + destination;
    }

    @PostMapping("/admin/extensions/{id}/reject")
    public String reject(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                         @RequestParam(required = false, defaultValue = "") String reason,
                         @RequestParam String idempotencyKey,
                         @RequestParam(defaultValue = "ALL") String status, @RequestParam(defaultValue = "0") int page,
                         RedirectAttributes redirect) {
        requireAdmin(user);
        String destination = location(status, page);
        String cleanReason = reason.trim();
        if (cleanReason.isBlank() || cleanReason.length() > 1000) {
            redirect.addFlashAttribute("actionError", cleanReason.isBlank() ? "Nhập lý do từ chối." : "Lý do từ chối tối đa 1.000 ký tự.");
            redirect.addFlashAttribute("openRejectId", id.toString());
            redirect.addFlashAttribute("rejectReason", reason);
            return "redirect:" + destination;
        }
        try {
            UUID key = UUID.fromString(idempotencyKey);
            RejectFinanceCommand command = new RejectFinanceCommand(cleanReason, 0);
            commands.execute(user.id(), "ADMIN_EXTENSION_REJECT", key,
                    new ExtensionActionFingerprint(id, command), ExtensionRequestView.class,
                    () -> ApiResponse.ok(extensions.rejectExtension(user.id(), id, command)));
            redirect.addFlashAttribute("actionSuccess", "Đã từ chối yêu cầu gia hạn.");
        } catch (RuntimeException exception) {
            logFailure("rejection", exception);
            redirect.addFlashAttribute("actionError", safeMessage(exception));
            redirect.addFlashAttribute("openRejectId", id.toString());
            redirect.addFlashAttribute("rejectReason", reason);
        }
        return "redirect:" + destination;
    }

    private ExtensionRow row(ExtensionRequestView value) {
        return new ExtensionRow(value, statusLabel(value.status().name()), dateLabel(value.createdAt()),
                dateLabel(value.requestedExpiryDate()), value.requestedExpiryDate() == null ? "" : LOCAL_VALUE.format(value.requestedExpiryDate()),
                value.approvedExpiryDate() == null ? null : dateLabel(value.approvedExpiryDate()));
    }
    private String normalizeStatus(String value) {
        String normalized = value == null ? "ALL" : value.trim().toUpperCase(Locale.ROOT);
        return FILTERS.contains(normalized) ? normalized : "ALL";
    }
    private String location(String status, int page) {
        return "/admin/extensions?status=" + normalizeStatus(status) + "&page=" + Math.max(0, Math.min(page, 100_000));
    }
    private String statusLabel(String value) {
        return switch (value) { case "ALL" -> "Tất cả trạng thái"; case "PENDING" -> "Chờ xử lý";
            case "APPROVED" -> "Đã gia hạn"; case "REJECTED" -> "Đã từ chối"; default -> "Đang cập nhật"; };
    }
    private String dateLabel(Instant value) { return value == null ? "Chưa xác định" : DATE_TIME.format(value); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value; }
    private String safeMessage(RuntimeException exception) {
        return exception instanceof BusinessException business ? business.getErrorCode().getDefaultMessage()
                : "Không thể cập nhật yêu cầu. Hãy tải lại danh sách để kiểm tra trạng thái mới nhất.";
    }
    private void logFailure(String action, RuntimeException exception) {
        if (exception instanceof BusinessException) log.info("Admin extension {} was rejected", action);
        else log.warn("Admin extension {} failed ({})", action, exception.getClass().getSimpleName());
    }
    private void requireAdmin(AuthenticatedUser user) {
        if (user == null || !"ADMIN".equals(user.role())) throw new AccessDeniedException("Admin workspace only");
    }

    public record StatusOption(String value, String label) {}
    public record ExtensionRow(ExtensionRequestView data, String statusLabel, String createdLabel,
                               String requestedExpiryLabel, String requestedExpiryInput, String approvedExpiryLabel) {}
    private record ExtensionActionFingerprint(UUID extensionId, Object action) {}
}
