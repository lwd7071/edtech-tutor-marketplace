package com.edtech.platform.booking.web;

import com.edtech.platform.booking.domain.DeliveryMode;
import com.edtech.platform.booking.dto.request.AcceptTrialRequest;
import com.edtech.platform.booking.dto.request.RejectTrialRequest;
import com.edtech.platform.booking.dto.response.BookingDetail;
import com.edtech.platform.booking.service.TrialRequestService;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class TeacherTrialRequestsPageController {
    private static final Logger log = LoggerFactory.getLogger(TeacherTrialRequestsPageController.class);
    private static final int PAGE_SIZE = 12;
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter
            .ofPattern("EEEE, dd/MM/yyyy · HH:mm", Locale.forLanguageTag("vi-VN"))
            .withZone(VIETNAM_ZONE);
    private static final DateTimeFormatter LOCAL_INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final TrialRequestService trials;
    private final Clock clock;

    @GetMapping("/teacher/trial-requests")
    public String list(@AuthenticationPrincipal AuthenticatedUser user,
                       @RequestParam(defaultValue = "0") int page,
                       Model model, HttpServletResponse response) {
        requireTeacher(user);
        noStore(response);
        int safePage = Math.max(0, Math.min(page, 100_000));
        model.addAttribute("activeWorkspaceNav", "trial-requests");
        model.addAttribute("trialRequestsUnavailable", false);
        model.addAttribute("currentPage", safePage);
        try {
            Page<TeacherTrialRequestRow> result = trials.findTeacherWorkspace(user.id(), PageRequest.of(safePage,
                    PAGE_SIZE, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
            if (safePage > 0 && result.getTotalElements() > 0 && result.isEmpty()) {
                return "redirect:/teacher/trial-requests?page=" + (result.getTotalPages() - 1);
            }
            model.addAttribute("trialRows", result.getContent());
            model.addAttribute("totalRequests", result.getTotalElements());
            model.addAttribute("totalPages", result.getTotalPages());
            model.addAttribute("hasPrevious", result.hasPrevious());
            model.addAttribute("hasNext", result.hasNext());
            model.addAttribute("currentPage", result.getNumber());
            model.addAttribute("todayLocal", LocalDateTime.ofInstant(clock.instant(), VIETNAM_ZONE).format(LOCAL_INPUT));
        } catch (RuntimeException ex) {
            log.warn("Teacher trial requests are unavailable");
            model.addAttribute("trialRequestsUnavailable", true);
            model.addAttribute("trialRows", List.of());
            model.addAttribute("totalRequests", 0L);
            model.addAttribute("totalPages", 0);
            model.addAttribute("hasPrevious", false);
            model.addAttribute("hasNext", false);
            model.addAttribute("todayLocal", "");
        }
        return "workspace/teacher-trial-requests";
    }

    @PostMapping("/teacher/trial-requests/{id}/accept")
    public String accept(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                         @RequestParam long version, @RequestParam int page,
                         @RequestParam(required = false) String startTime,
                         @RequestParam(required = false) String endTime,
                         @RequestParam(required = false) DeliveryMode deliveryMode,
                         @RequestParam(required = false) String meetingLink,
                         @RequestParam(required = false) String locationAddress,
                         RedirectAttributes redirect) {
        requireTeacher(user);
        String safeLink = blankToNull(meetingLink);
        String safeAddress = blankToNull(locationAddress);
        TrialActionForm form = new TrialActionForm("accept", version, page, startTime, endTime, deliveryMode,
                meetingLink, locationAddress, "");
        LocalDateTime start = parseLocal(startTime);
        LocalDateTime end = parseLocal(endTime);
        if (start == null || end == null) {
            return actionError(redirect, id, page, form, "Nhập thời gian bắt đầu và kết thúc.");
        }
        Instant startInstant = start.atZone(VIETNAM_ZONE).toInstant();
        Instant endInstant = end.atZone(VIETNAM_ZONE).toInstant();
        if (!startInstant.isBefore(endInstant)) {
            return actionError(redirect, id, page, form, "Giờ kết thúc phải sau giờ bắt đầu.");
        }
        if (startInstant.isBefore(clock.instant())) {
            return actionError(redirect, id, page, form, "Thời gian bắt đầu phải ở trong tương lai.");
        }
        if (deliveryMode == null) {
            return actionError(redirect, id, page, form, "Chọn hình thức học.");
        }
        if (safeLink != null && !validHttpUrl(safeLink)) {
            return actionError(redirect, id, page, form, "Link buổi học phải bắt đầu bằng HTTP hoặc HTTPS.");
        }

        try {
            BookingDetail booking = trials.accept(user.id(), id,
                    new AcceptTrialRequest(startInstant, endInstant, deliveryMode, safeLink, safeAddress, version));
            redirect.addFlashAttribute("bookingNotice", "Đã tạo buổi học thử.");
            return "redirect:/teacher/bookings/" + booking.id();
        } catch (BusinessException ex) {
            return actionError(redirect, id, page, form, publicError(ex.getErrorCode()));
        } catch (RuntimeException ex) {
            log.warn("Teacher trial request could not be accepted");
            return actionError(redirect, id, page, form, "Chưa chấp nhận được yêu cầu. Tải lại danh sách rồi thử lại.");
        }
    }

    @PostMapping("/teacher/trial-requests/{id}/reject")
    public String reject(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                         @RequestParam long version, @RequestParam int page,
                         @RequestParam(required = false) String reason, RedirectAttributes redirect) {
        requireTeacher(user);
        TrialActionForm form = new TrialActionForm("reject", version, page, "", "", null, "", "", reason);
        if (reason == null || reason.isBlank()) return actionError(redirect, id, page, form, "Nhập lý do từ chối.");
        try {
            trials.reject(user.id(), id, new RejectTrialRequest(reason.trim(), version));
            redirect.addFlashAttribute("trialNotice", "Đã từ chối yêu cầu học thử.");
        } catch (BusinessException ex) {
            return actionError(redirect, id, page, form, publicError(ex.getErrorCode()));
        } catch (RuntimeException ex) {
            log.warn("Teacher trial request could not be rejected");
            return actionError(redirect, id, page, form, "Chưa từ chối được yêu cầu. Tải lại danh sách rồi thử lại.");
        }
        return redirectList(page);
    }

    private String actionError(RedirectAttributes redirect, UUID id, int page, TrialActionForm form, String error) {
        redirect.addFlashAttribute("trialActionError", error);
        redirect.addFlashAttribute("trialResponseRequestId", id.toString());
        redirect.addFlashAttribute("trialResponseAction", form.action());
        redirect.addFlashAttribute("trialActionForm", form);
        return redirectList(page) + "#trial-action-error";
    }

    private String redirectList(int page) {
        return "redirect:/teacher/trial-requests?page=" + Math.max(0, Math.min(page, 100_000));
    }

    private LocalDateTime parseLocal(String value) {
        if (value == null || value.isBlank()) return null;
        try { return LocalDateTime.parse(value, LOCAL_INPUT); }
        catch (RuntimeException ex) { return null; }
    }

    private boolean validHttpUrl(String value) {
        try {
            URI uri = new URI(value);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && !uri.getHost().isBlank();
        } catch (URISyntaxException ex) { return false; }
    }

    private String publicError(ErrorCode code) {
        return switch (code) {
            case CONCURRENT_MODIFICATION -> "Yêu cầu đã thay đổi. Tải lại danh sách rồi kiểm tra trạng thái mới.";
            case RESOURCE_NOT_FOUND -> "Yêu cầu không tồn tại hoặc không thuộc tài khoản của bạn.";
            case TRIAL_REQUEST_INVALID_STATE -> "Yêu cầu này đã được phản hồi trước đó.";
            case TRIAL_ALREADY_USED -> "Học viên đã có buổi học thử với bạn.";
            case BOOKING_TIME_CONFLICT -> "Khung giờ này trùng với lịch của bạn hoặc học viên.";
            case BOOKING_IN_PAST -> "Thời gian bắt đầu phải ở trong tương lai.";
            case BOOKING_INVALID_TIME_RANGE -> "Giờ kết thúc phải sau giờ bắt đầu.";
            default -> "Chưa thể phản hồi yêu cầu. Tải lại danh sách rồi thử lại.";
        };
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private void noStore(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
    }
    private void requireTeacher(AuthenticatedUser user) {
        if (user == null || !"TEACHER".equals(user.role())) throw new AccessDeniedException("Teacher workspace only");
    }

    public record TrialActionForm(String action, long version, int page, String startTime, String endTime, DeliveryMode deliveryMode,
                                  String meetingLink, String locationAddress, String reason) {}
}
