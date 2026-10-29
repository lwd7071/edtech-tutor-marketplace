package com.edtech.platform.booking.web;

import com.edtech.platform.booking.domain.SettlementStatus;
import com.edtech.platform.booking.dto.response.BookingSettlementAdminView;
import com.edtech.platform.booking.service.BookingReadService;
import com.edtech.platform.booking.service.BookingService;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.security.AuthenticatedUser;
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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class AdminBookingSettlementsPageController {
    private static final Logger log = LoggerFactory.getLogger(AdminBookingSettlementsPageController.class);
    private static final int PAGE_SIZE = 20;
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm", Locale.forLanguageTag("vi-VN")).withZone(VIETNAM_ZONE);
    private static final List<String> FILTERS = List.of("ALL", "AWAITING_CONFIRMATION", "HELD", "DISPUTE_PENDING",
            "REOPENED", "AWAITING_ADMIN_DECISION", "RELEASED", "RETAINED");

    private final BookingReadService reads;
    private final BookingService bookings;
    private final Clock clock;

    @ModelAttribute
    void preventPrivatePageCaching(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, max-age=0");
    }

    @GetMapping("/admin/booking-settlements")
    public String list(@AuthenticationPrincipal AuthenticatedUser user,
                       @RequestParam(defaultValue = "ALL") String status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        requireAdmin(user);
        String selected = normalizeStatus(status);
        int safePage = Math.max(0, Math.min(page, 100_000));
        model.addAttribute("activeWorkspaceNav", "settlements");
        model.addAttribute("selectedStatus", selected);
        model.addAttribute("statusOptions", FILTERS.stream().map(value -> new StatusOption(value, statusLabel(value))).toList());
        model.addAttribute("pageNumber", safePage);
        model.addAttribute("settlementsUnavailable", false);
        model.addAttribute("settlementRows", List.of());
        model.addAttribute("totalElements", 0L);
        model.addAttribute("totalPages", 0);
        model.addAttribute("hasPrevious", false);
        model.addAttribute("hasNext", false);
        model.addAttribute("now", clock.instant());
        try {
            SettlementStatus filter = "ALL".equals(selected) ? null : SettlementStatus.valueOf(selected);
            Page<BookingSettlementAdminView> result = reads.adminSettlements(filter,
                    PageRequest.of(safePage, PAGE_SIZE, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
            if (safePage > 0 && result.getTotalElements() > 0 && result.isEmpty()) {
                return "redirect:/admin/booking-settlements?status=" + selected + "&page=" + Math.max(0, result.getTotalPages() - 1);
            }
            List<SettlementRow> rows = result.getContent().stream().map(this::row).toList();
            model.addAttribute("settlementRows", rows);
            model.addAttribute("totalElements", result.getTotalElements());
            model.addAttribute("totalPages", result.getTotalPages());
            model.addAttribute("hasPrevious", result.hasPrevious());
            model.addAttribute("hasNext", result.hasNext());
        } catch (RuntimeException exception) {
            log.warn("Admin booking settlement queue is unavailable");
            model.addAttribute("settlementRows", List.of());
            model.addAttribute("settlementsUnavailable", true);
        }
        return "workspace/admin-booking-settlements";
    }

    @PostMapping("/admin/booking-settlements/{id}/reopen")
    public String reopen(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                         @RequestParam long version, @RequestParam(required = false, defaultValue = "") String note,
                         @RequestParam(defaultValue = "ALL") String status, @RequestParam(defaultValue = "0") int page,
                         RedirectAttributes redirect) {
        return mutate(user, id, version, note, "reopen", status, page, redirect);
    }

    @PostMapping("/admin/booking-settlements/{id}/release")
    public String release(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                          @RequestParam long version, @RequestParam(required = false, defaultValue = "") String note,
                          @RequestParam(defaultValue = "ALL") String status, @RequestParam(defaultValue = "0") int page,
                          RedirectAttributes redirect) {
        return mutate(user, id, version, note, "release", status, page, redirect);
    }

    @PostMapping("/admin/booking-settlements/{id}/retain")
    public String retain(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                         @RequestParam long version, @RequestParam(required = false, defaultValue = "") String note,
                         @RequestParam(defaultValue = "ALL") String status, @RequestParam(defaultValue = "0") int page,
                         RedirectAttributes redirect) {
        return mutate(user, id, version, note, "retain", status, page, redirect);
    }

    private String mutate(AuthenticatedUser user, UUID bookingId, long version, String note, String action,
                          String status, int page, RedirectAttributes redirect) {
        requireAdmin(user);
        String destination = location(status, page);
        String cleanNote = note == null ? "" : note.trim();
        if (cleanNote.isBlank()) {
            redirect.addFlashAttribute("actionError", "Nhập ghi chú cho quyết định này.");
            redirect.addFlashAttribute("openActionId", bookingId.toString());
            redirect.addFlashAttribute("actionNote", note);
            redirect.addFlashAttribute("openAction", action);
            return "redirect:" + destination;
        }
        try {
            switch (action) {
                case "reopen" -> bookings.reopenSettlement(user.id(), bookingId, version, cleanNote);
                case "release" -> bookings.adminDecision(user.id(), bookingId, version, false, cleanNote);
                case "retain" -> bookings.adminDecision(user.id(), bookingId, version, true, cleanNote);
                default -> throw new IllegalArgumentException("Unsupported settlement action");
            }
            redirect.addFlashAttribute("actionSuccess", switch (action) {
                case "reopen" -> "Đã mở lại bước xác nhận với gia sư và học viên.";
                case "release" -> "Đã quyết định giải ngân cho gia sư.";
                default -> "Đã quyết định giữ khoản tiền trong quy trình xử lý.";
            });
        } catch (RuntimeException exception) {
            if (exception instanceof BusinessException business) redirect.addFlashAttribute("actionError", business.getErrorCode().getDefaultMessage());
            else redirect.addFlashAttribute("actionError", "Không thể cập nhật quyết toán. Hãy tải lại danh sách để kiểm tra trạng thái mới nhất.");
            redirect.addFlashAttribute("openActionId", bookingId.toString());
            redirect.addFlashAttribute("actionNote", note);
            redirect.addFlashAttribute("openAction", action);
            if (exception instanceof BusinessException) log.info("Admin booking settlement decision was rejected");
            else log.warn("Admin booking settlement decision failed ({})", exception.getClass().getSimpleName());
        }
        return "redirect:" + destination;
    }

    private SettlementRow row(BookingSettlementAdminView value) {
        boolean reopenedExpired = value.status() == SettlementStatus.REOPENED
                && value.reopenDeadline() != null && !clock.instant().isBefore(value.reopenDeadline())
                && !(value.teacherConfirmedAt() != null && value.studentConfirmedAt() != null);
        return new SettlementRow(value, statusLabel(value.status().name()), dateLabel(value.startTime()),
                dateLabel(value.endTime()), dateLabel(value.confirmationDeadline()), dateLabel(value.reopenDeadline()),
                dateLabel(value.disputedAt()), dateLabel(value.studentConfirmedAt()), dateLabel(value.teacherConfirmedAt()),
                money(value.netAmountVnd()), reopenedExpired);
    }

    private String normalizeStatus(String value) {
        String normalized = value == null ? "ALL" : value.trim().toUpperCase(Locale.ROOT);
        return FILTERS.contains(normalized) ? normalized : "ALL";
    }
    private String location(String status, int page) {
        return "/admin/booking-settlements?status=" + normalizeStatus(status) + "&page=" + Math.max(0, Math.min(page, 100_000));
    }
    private String statusLabel(String value) {
        return switch (value) {
            case "ALL" -> "Tất cả trạng thái"; case "AWAITING_CONFIRMATION" -> "Chờ xác nhận";
            case "HELD" -> "Đang giữ tiền"; case "DISPUTE_PENDING" -> "Có khiếu nại";
            case "REOPENED" -> "Đã mở lại"; case "AWAITING_ADMIN_DECISION" -> "Chờ quyết định Admin";
            case "RELEASED" -> "Đã giải ngân"; case "RETAINED" -> "Đã giữ lại"; default -> "Đang cập nhật";
        };
    }
    private String dateLabel(Instant value) { return value == null ? "Chưa xác định" : DATE_TIME.format(value); }
    private String money(Long amount) { return amount == null ? "Chưa phân bổ" : String.format(Locale.forLanguageTag("vi-VN"), "%,d ₫", amount); }
    private void requireAdmin(AuthenticatedUser user) {
        if (user == null || !"ADMIN".equals(user.role())) throw new AccessDeniedException("Admin workspace only");
    }

    public record StatusOption(String value, String label) {}
    public record SettlementRow(BookingSettlementAdminView data, String statusLabel, String startLabel,
                                String endLabel, String confirmationDeadlineLabel, String reopenDeadlineLabel,
                                String disputedLabel, String studentConfirmedLabel, String teacherConfirmedLabel,
                                String amountLabel, boolean reopenedExpired) {}
}
