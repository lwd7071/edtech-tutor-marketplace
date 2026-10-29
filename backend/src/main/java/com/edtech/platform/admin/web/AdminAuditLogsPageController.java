package com.edtech.platform.admin.web;

import com.edtech.platform.admin.domain.AuditAction;
import com.edtech.platform.admin.dto.response.AuditLogView;
import com.edtech.platform.admin.service.AuditLogQueryService;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class AdminAuditLogsPageController {
    private static final Logger log = LoggerFactory.getLogger(AdminAuditLogsPageController.class);
    private static final int PAGE_SIZE = 20;
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm", Locale.forLanguageTag("vi-VN")).withZone(VIETNAM_ZONE);
    private final AuditLogQueryService auditLogs;
    private final ObjectMapper mapper;

    @ModelAttribute
    void preventPrivatePageCaching(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, max-age=0");
    }

    @GetMapping("/admin/audit-logs")
    public String list(@AuthenticationPrincipal AuthenticatedUser user,
                       @RequestParam(required = false) String actorId,
                       @RequestParam(required = false) String action,
                       @RequestParam(required = false) String targetType,
                       @RequestParam(required = false) String targetId,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        requireAdmin(user);
        int safePage = Math.max(0, Math.min(page, 100_000));
        String cleanActor = clean(actorId);
        String cleanTarget = clean(targetId);
        String cleanType = clean(targetType);
        String selectedAction = normalizeAction(action);
        model.addAttribute("activeWorkspaceNav", "audit-logs");
        model.addAttribute("actorIdFilter", cleanActor);
        model.addAttribute("targetIdFilter", cleanTarget);
        model.addAttribute("targetTypeFilter", cleanType);
        model.addAttribute("selectedAction", selectedAction);
        model.addAttribute("actionOptions", Arrays.stream(AuditAction.values()).map(value -> new ActionOption(value.name(), actionLabel(value))).toList());
        model.addAttribute("pageNumber", safePage);
        model.addAttribute("auditUnavailable", false);
        model.addAttribute("auditRows", List.of());
        model.addAttribute("totalElements", 0L);
        model.addAttribute("totalPages", 0);
        model.addAttribute("hasPrevious", false);
        model.addAttribute("hasNext", false);
        String inputError = uuidError("mã người thực hiện", cleanActor) != null
                ? uuidError("mã người thực hiện", cleanActor) : uuidError("mã đối tượng", cleanTarget);
        if (inputError == null && cleanType != null && cleanType.length() > 50)
            inputError = "Loại đối tượng không được vượt quá 50 ký tự.";
        if (inputError != null) {
            model.addAttribute("auditUnavailable", true);
            model.addAttribute("auditError", inputError);
            return "workspace/admin-audit-logs";
        }
        try {
            Page<AuditLogView> result = auditLogs.findAuditLogs(parseUuid(cleanActor), parseAction(selectedAction), cleanType,
                    parseUuid(cleanTarget), PageRequest.of(safePage, PAGE_SIZE,
                            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
            if (safePage > 0 && result.getTotalElements() > 0 && result.isEmpty()) {
                return "redirect:/admin/audit-logs" + query(selectedAction, cleanActor, cleanType, cleanTarget, result.getTotalPages() - 1);
            }
            List<AuditRow> rows = result.getContent().stream().map(this::row).toList();
            model.addAttribute("auditRows", rows);
            model.addAttribute("totalElements", result.getTotalElements());
            model.addAttribute("totalPages", result.getTotalPages());
            model.addAttribute("hasPrevious", result.hasPrevious());
            model.addAttribute("hasNext", result.hasNext());
        } catch (RuntimeException exception) {
            log.warn("Admin audit log query is unavailable");
            model.addAttribute("auditUnavailable", true);
            model.addAttribute("auditError", "Chưa tải được nhật ký thao tác.");
        }
        return "workspace/admin-audit-logs";
    }

    private AuditRow row(AuditLogView value) {
        return new AuditRow(value, actionLabel(value.action()), dateLabel(value.createdAt()),
                json(value.beforeData()), json(value.afterData()));
    }
    private String json(Map<String, Object> value) {
        if (value == null || value.isEmpty()) return "Không có dữ liệu";
        try { return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value); }
        catch (JsonProcessingException exception) { return "Không thể hiển thị dữ liệu nhật ký."; }
    }
    private String actionLabel(AuditAction action) { return actionLabel(action == null ? "" : action.name()); }
    private String actionLabel(String value) {
        return switch (value) {
            case "TEACHER_APPROVED" -> "Duyệt gia sư"; case "TEACHER_REJECTED" -> "Từ chối gia sư";
            case "SUBJECT_PROPOSAL_APPROVED" -> "Duyệt đề xuất môn"; case "SUBJECT_PROPOSAL_REJECTED" -> "Từ chối đề xuất môn";
            case "USER_LOCKED" -> "Khóa người dùng"; case "USER_UNLOCKED" -> "Mở khóa người dùng";
            case "REFUND_APPROVED" -> "Duyệt hoàn tiền"; case "REFUND_REJECTED" -> "Từ chối hoàn tiền";
            case "REFUND_COMPLETED" -> "Hoàn tất hoàn tiền"; case "PAYOUT_PROCESSING" -> "Bắt đầu chi trả";
            case "PAYOUT_REJECTED" -> "Từ chối rút tiền"; case "PAYOUT_COMPLETED" -> "Hoàn tất chi trả";
            case "EXTENSION_APPROVED" -> "Duyệt gia hạn"; case "EXTENSION_REJECTED" -> "Từ chối gia hạn";
            case "PLATFORM_SETTINGS_UPDATED" -> "Cập nhật cài đặt";
            case "BOOKING_SETTLEMENT_REOPENED" -> "Mở lại quyết toán";
            case "BOOKING_SETTLEMENT_RELEASED" -> "Giải ngân quyết toán";
            case "BOOKING_SETTLEMENT_RETAINED" -> "Giữ khoản quyết toán";
            default -> value.replace('_', ' ').toLowerCase(Locale.ROOT);
        };
    }
    private String normalizeAction(String value) {
        if (value == null || value.isBlank()) return "ALL";
        try { return AuditAction.valueOf(value.trim().toUpperCase(Locale.ROOT)).name(); }
        catch (IllegalArgumentException exception) { return "ALL"; }
    }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String uuidError(String label, String value) {
        if (value == null) return null;
        try { UUID.fromString(value); return null; }
        catch (IllegalArgumentException exception) { return "Nhập đúng " + label + " dạng UUID hoặc xóa bộ lọc này."; }
    }
    private UUID parseUuid(String value) { return value == null ? null : UUID.fromString(value); }
    private AuditAction parseAction(String value) { return "ALL".equals(value) ? null : AuditAction.valueOf(value); }
    private String dateLabel(Instant value) { return value == null ? "Chưa xác định" : DATE_TIME.format(value); }
    private String query(String action, String actor, String type, String target, int page) {
        StringBuilder query = new StringBuilder("?page=").append(Math.max(0, page));
        if (!"ALL".equals(action)) query.append("&action=").append(action);
        if (actor != null) query.append("&actorId=").append(actor);
        if (type != null) query.append("&targetType=").append(java.net.URLEncoder.encode(type, java.nio.charset.StandardCharsets.UTF_8));
        if (target != null) query.append("&targetId=").append(target);
        return query.toString();
    }
    private void requireAdmin(AuthenticatedUser user) {
        if (user == null || !"ADMIN".equals(user.role())) throw new AccessDeniedException("Admin workspace only");
    }

    public record ActionOption(String value, String label) {}
    public record AuditRow(AuditLogView data, String actionLabel, String createdLabel,
                           String beforeJson, String afterJson) {}
}
