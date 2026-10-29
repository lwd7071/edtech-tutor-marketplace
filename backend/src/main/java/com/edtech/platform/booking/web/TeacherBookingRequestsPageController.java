package com.edtech.platform.booking.web;

import com.edtech.platform.booking.domain.BookingRequestStatus;
import com.edtech.platform.booking.domain.DeliveryMode;
import com.edtech.platform.booking.dto.request.AcceptBookingRequestCommand;
import com.edtech.platform.booking.dto.request.RejectBookingRequestCommand;
import com.edtech.platform.booking.service.BookingRequestService;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class TeacherBookingRequestsPageController {
    private static final Logger log = LoggerFactory.getLogger(TeacherBookingRequestsPageController.class);
    private static final int PAGE_SIZE = 10;
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final BookingRequestService bookingRequests;

    @GetMapping("/teacher/booking-requests")
    public String list(@AuthenticationPrincipal AuthenticatedUser user,
                       @RequestParam(defaultValue = "ALL") String status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model, HttpServletResponse response) {
        noStore(response);
        BookingRequestStatus selected = parse(status);
        int safePage = Math.max(0, Math.min(page, 100_000));
        var pageable = PageRequest.of(safePage, PAGE_SIZE,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        model.addAttribute("activeWorkspaceNav", "booking-requests");
        model.addAttribute("selectedStatus", selected == null ? "ALL" : selected.name());
        model.addAttribute("statusInvalid", status != null && !status.isBlank()
                && !"ALL".equalsIgnoreCase(status) && selected == null);
        model.addAttribute("statusOptions", List.of("ALL", "PENDING", "ACCEPTED", "REJECTED", "CANCELLED"));
        model.addAttribute("deliveryModes", Arrays.stream(DeliveryMode.values()).toList());
        try {
            Page<TeacherBookingRequestRow> result = bookingRequests.findTeacherWorkspace(user.id(), selected, pageable);
            model.addAttribute("requestRows", result.getContent());
            model.addAttribute("currentPage", result.getNumber());
            model.addAttribute("totalPages", result.getTotalPages());
            model.addAttribute("hasPrevious", result.hasPrevious());
            model.addAttribute("hasNext", result.hasNext());
            model.addAttribute("requestsUnavailable", false);
        } catch (RuntimeException failure) {
            log.warn("Teacher booking requests are unavailable", failure);
            model.addAttribute("requestRows", List.of());
            model.addAttribute("requestsUnavailable", true);
        }
        return "workspace/teacher-booking-requests";
    }

    @PostMapping("/teacher/booking-requests/{id}/accept")
    public String accept(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user,
                         @RequestParam String startTime, @RequestParam DeliveryMode deliveryMode,
                         @RequestParam(required = false) String meetingLink,
                         @RequestParam(required = false) String locationAddress,
                         @RequestParam long version, RedirectAttributes redirect) {
        try {
            LocalDateTime localStart = LocalDateTime.parse(startTime);
            bookingRequests.accept(user.id(), id, new AcceptBookingRequestCommand(
                    localStart.atZone(VIETNAM_ZONE).toInstant(), deliveryMode,
                    meetingLink, locationAddress, version));
            redirect.addFlashAttribute("requestSuccess", "Đã xác nhận thời gian và lên lịch buổi học.");
        } catch (BusinessException failure) {
            redirect.addFlashAttribute("requestError", "Không thể xác nhận yêu cầu. Hãy tải lại trang và kiểm tra gói học.");
        } catch (RuntimeException failure) {
            redirect.addFlashAttribute("requestError", "Thông tin chưa hợp lệ. Hãy kiểm tra thời gian và hình thức học.");
        }
        return "redirect:/teacher/booking-requests";
    }

    @PostMapping("/teacher/booking-requests/{id}/reject")
    public String reject(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user,
                         @RequestParam String reason, @RequestParam long version,
                         RedirectAttributes redirect) {
        try {
            bookingRequests.reject(user.id(), id, new RejectBookingRequestCommand(reason, version));
            redirect.addFlashAttribute("requestSuccess", "Đã từ chối yêu cầu.");
        } catch (BusinessException failure) {
            redirect.addFlashAttribute("requestError", "Không thể từ chối yêu cầu. Hãy tải lại trang và kiểm tra trạng thái.");
        }
        return "redirect:/teacher/booking-requests";
    }

    private BookingRequestStatus parse(String raw) {
        if (raw == null || raw.isBlank() || "ALL".equalsIgnoreCase(raw)) return null;
        try { return BookingRequestStatus.valueOf(raw.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException invalid) { return null; }
    }

    private void noStore(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
    }
}
