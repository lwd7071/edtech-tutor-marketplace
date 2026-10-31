package com.edtech.platform.enrollment.web;

import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.booking.dto.request.CreateBookingRequestCommand;
import com.edtech.platform.booking.service.BookingRequestService;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.enrollment.domain.StudentPackageStatus;
import com.edtech.platform.enrollment.dto.StudentOwnedPackageView;
import com.edtech.platform.enrollment.service.StudentPackageReadService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class StudentPackagePageController {
    private static final Logger log = LoggerFactory.getLogger(StudentPackagePageController.class);
    private static final int PAGE_SIZE = 9;
    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy", VIETNAMESE)
            .withZone(VIETNAM_ZONE);
    private final StudentPackageReadService packages;
    private final BookingRequestService bookingRequests;

    @GetMapping("/student/packages")
    public String list(@AuthenticationPrincipal AuthenticatedUser user,
                       @RequestParam(required = false) String status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        StudentPackageStatus selected;
        boolean invalidFilter = false;
        try {
            selected = parseStatus(status);
        } catch (ResponseStatusException invalid) {
            selected = null;
            invalidFilter = true;
        }
        model.addAttribute("activeWorkspaceNav", "packages");
        model.addAttribute("selectedStatus", selected == null ? "ALL" : selected.name());
        model.addAttribute("statusFilterInvalid", invalidFilter);
        model.addAttribute("statusOptions", statusOptions());
        model.addAttribute("pageSize", PAGE_SIZE);
        model.addAttribute("packagesUnavailable", false);

        int safePage = Math.max(0, page);
        try {
            Page<StudentOwnedPackageView> result = packages.list(user.id(), selected, PageRequest.of(safePage, PAGE_SIZE));
            if (safePage > 0 && result.getTotalElements() > 0 && result.isEmpty()) {
                int lastPage = Math.max(0, result.getTotalPages() - 1);
                return "redirect:/student/packages?page=" + lastPage
                        + (selected == null ? "" : "&status=" + selected.name());
            }
            model.addAttribute("packageRows", result.getContent().stream().map(this::toRow).toList());
            model.addAttribute("currentPage", result.getNumber());
            model.addAttribute("totalPages", result.getTotalPages());
            model.addAttribute("totalElements", result.getTotalElements());
            model.addAttribute("hasPrevious", result.hasPrevious());
            model.addAttribute("hasNext", result.hasNext());
        } catch (RuntimeException ex) {
            log.warn("Student package list is unavailable", ex);
            model.addAttribute("packagesUnavailable", true);
        }
        return "student/packages";
    }

    @GetMapping("/student/packages/{id}")
    public String detail(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user, Model model) {
        model.addAttribute("activeWorkspaceNav", "packages");
        try {
            StudentOwnedPackageView ownedPackage = packages.detail(id, user.id())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            model.addAttribute("packageRow", toRow(ownedPackage));
            model.addAttribute("pendingBookingRequest",
                    bookingRequests.findPendingForStudentPackage(user.id(), id));
        } catch (ResponseStatusException notFound) {
            throw notFound;
        } catch (RuntimeException ex) {
            log.warn("Student package detail is unavailable", ex);
            model.addAttribute("packageUnavailable", true);
        }
        return "student/package-detail";
    }

    @PostMapping("/student/packages/{id}/booking-request")
    public String requestBooking(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user,
                                 @RequestParam String preferredStartTime,
                                 @RequestParam(required = false) String studentNote,
                                 RedirectAttributes redirect) {
        LocalDateTime localTime;
        try { localTime = LocalDateTime.parse(preferredStartTime); }
        catch (DateTimeParseException invalid) {
            redirect.addFlashAttribute("bookingRequestError", "Thời gian chưa hợp lệ. Hãy chọn ngày và giờ trong tương lai.");
            return "redirect:/student/packages/" + id;
        }
        String note = studentNote == null ? null : studentNote.trim();
        if (note != null && note.length() > 500) {
            redirect.addFlashAttribute("bookingRequestError", "Ghi chú không được dài quá 500 ký tự.");
            return "redirect:/student/packages/" + id;
        }
        try {
            bookingRequests.create(user.id(), new CreateBookingRequestCommand(
                    id, localTime.atZone(VIETNAM_ZONE).toInstant(), note));
            redirect.addFlashAttribute("bookingRequestSuccess", "Đã gửi yêu cầu. Gia sư sẽ phản hồi thời gian cụ thể.");
        } catch (BusinessException business) {
            redirect.addFlashAttribute("bookingRequestError", "Yêu cầu chưa được gửi. Hãy kiểm tra gói học và thời gian bạn chọn.");
        } catch (RuntimeException unavailable) {
            log.warn("Student booking request is unavailable", unavailable);
            redirect.addFlashAttribute("bookingRequestError", "Chưa gửi được yêu cầu do hệ thống tạm thời bận. Hãy thử lại.");
        }
        return "redirect:/student/packages/" + id;
    }

    @PostMapping("/student/packages/{packageId}/booking-request/{requestId}/cancel")
    public String cancelBookingRequest(@PathVariable UUID packageId, @PathVariable UUID requestId,
                                       @RequestParam long version,
                                       @AuthenticationPrincipal AuthenticatedUser user,
                                       RedirectAttributes redirect) {
        try {
            bookingRequests.cancel(user.id(), requestId, version);
            redirect.addFlashAttribute("bookingRequestSuccess", "Đã hủy yêu cầu đặt buổi.");
        } catch (BusinessException business) {
            redirect.addFlashAttribute("bookingRequestError", "Không thể hủy yêu cầu này. Tải lại trang để xem trạng thái mới nhất.");
        } catch (RuntimeException unavailable) {
            log.warn("Student booking request cancellation is unavailable", unavailable);
            redirect.addFlashAttribute("bookingRequestError", "Chưa hủy được yêu cầu do hệ thống tạm thời bận.");
        }
        return "redirect:/student/packages/" + packageId;
    }

    private StudentPackageStatus parseStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank() || "ALL".equalsIgnoreCase(rawStatus)) return null;
        try {
            return StudentPackageStatus.valueOf(rawStatus.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException invalid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái gói học không hợp lệ");
        }
    }

    private PackageRow toRow(StudentOwnedPackageView view) {
        return new PackageRow(view, statusLabel(view.status()),
                NumberFormat.getNumberInstance(VIETNAMESE).format(view.purchasePriceVnd()) + " ₫",
                view.expiresAt() == null ? "Không giới hạn" : DATE_FORMAT.format(view.expiresAt()));
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "ACTIVE" -> "Đang hoạt động";
            case "PENDING_PAYMENT" -> "Chờ thanh toán";
            case "COMPLETED" -> "Đã hoàn thành";
            case "LOCKED_EXPIRED" -> "Đã hết hạn";
            case "REFUND_PENDING" -> "Đang xử lý hoàn tiền";
            case "REFUNDED" -> "Đã hoàn tiền";
            default -> "Trạng thái khác";
        };
    }

    private List<StatusOption> statusOptions() {
        return List.of(new StatusOption("ALL", "Tất cả gói học"),
                new StatusOption("ACTIVE", "Đang hoạt động"),
                new StatusOption("PENDING_PAYMENT", "Chờ thanh toán"),
                new StatusOption("COMPLETED", "Đã hoàn thành"),
                new StatusOption("LOCKED_EXPIRED", "Đã hết hạn"),
                new StatusOption("REFUND_PENDING", "Đang xử lý hoàn tiền"),
                new StatusOption("REFUNDED", "Đã hoàn tiền"));
    }

    public record StatusOption(String value, String label) {}
    public record PackageRow(StudentOwnedPackageView data, String statusLabel, String priceLabel, String expiryLabel) {}
}
