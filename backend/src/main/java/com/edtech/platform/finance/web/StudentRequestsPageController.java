package com.edtech.platform.finance.web;

import com.edtech.platform.booking.domain.TrialRequestStatus;
import com.edtech.platform.booking.dto.response.TrialRequestView;
import com.edtech.platform.booking.service.TrialRequestService;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.enrollment.dto.StudentOwnedPackageView;
import com.edtech.platform.enrollment.service.StudentPackageReadService;
import com.edtech.platform.finance.dto.request.CreateExtensionRequest;
import com.edtech.platform.finance.dto.request.CreateRefundRequest;
import com.edtech.platform.finance.dto.response.ExtensionRequestView;
import com.edtech.platform.finance.dto.response.RefundRequestView;
import com.edtech.platform.finance.service.ExtensionService;
import com.edtech.platform.finance.service.FinanceCommandExecutor;
import com.edtech.platform.finance.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class StudentRequestsPageController {
    private static final Logger log = LoggerFactory.getLogger(StudentRequestsPageController.class);
    private static final int PAGE_SIZE = 10;
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))
            .withZone(VIETNAM_ZONE);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))
            .withZone(VIETNAM_ZONE);
    private final RefundService refunds;
    private final ExtensionService extensions;
    private final TrialRequestService trials;
    private final StudentPackageReadService packages;
    private final FinanceCommandExecutor financeCommands;
    private final Clock clock;

    @GetMapping("/student/requests")
    public String page(@AuthenticationPrincipal AuthenticatedUser user,
                       @RequestParam(defaultValue = "trial") String tab,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false) String packageId,
                       Model model) {
        requireStudent(user);
        String selectedTab = normalizeTab(tab);
        int safePage = Math.max(0, Math.min(page, 100_000));
        model.addAttribute("activeWorkspaceNav", "requests");
        model.addAttribute("selectedTab", selectedTab);
        model.addAttribute("tabs", List.of(new Tab("trial", "Học thử"), new Tab("refund", "Hoàn tiền"), new Tab("extension", "Gia hạn")));
        model.addAttribute("pageSize", PAGE_SIZE);
        model.addAttribute("listUnavailable", false);
        model.addAttribute("requestRows", List.of());
        model.addAttribute("currentPage", 0);
        model.addAttribute("totalPages", 0);
        model.addAttribute("totalElements", 0L);
        model.addAttribute("hasPrevious", false);
        model.addAttribute("hasNext", false);
        loadSelectedList(user.id(), selectedTab, safePage, model);
        if (Boolean.TRUE.equals(model.getAttribute("pageRedirect"))) {
            String suffix = ownedPackageQuery(packageId);
            return "redirect:/student/requests?tab=" + selectedTab + "&page=" + model.getAttribute("lastPage") + suffix;
        }
        loadPackageContext(user.id(), packageId, model);
        if (packageId != null && !packageId.isBlank()) model.addAttribute("packageId", packageId);
        model.addAttribute("refundIdempotencyKey", UUID.randomUUID());
        model.addAttribute("extensionIdempotencyKey", UUID.randomUUID());
        model.addAttribute("minimumExpiryDate", LocalDate.now(clock.withZone(VIETNAM_ZONE)).plusDays(1));
        return "student/requests";
    }

    @PostMapping("/student/requests/refund")
    public String createRefund(@AuthenticationPrincipal AuthenticatedUser user,
                               @RequestParam String studentPackageId,
                               @RequestParam String packageVersion,
                               @RequestParam String idempotencyKey,
                               @RequestParam String reason,
                               @RequestParam String requestedSessions,
                               @RequestParam String bankName,
                               @RequestParam String bankBin,
                               @RequestParam String accountNumber,
                               @RequestParam String accountHolderName,
                               RedirectAttributes redirect) {
        requireStudent(user);
        UUID packageUuid = parseId(studentPackageId);
        try {
            long version = Long.parseLong(packageVersion);
            int sessions = Integer.parseInt(requestedSessions);
            UUID key = UUID.fromString(idempotencyKey);
            String holder = accountHolderName.trim().toUpperCase(Locale.ROOT);
            if (reason == null || reason.trim().length() < 10 || reason.length() > 1000
                    || sessions < 1 || bankName.isBlank() || !bankBin.matches("[0-9]{6}")
                    || !accountNumber.matches("[A-Za-z0-9]{6,25}") || holder.length() < 3 || holder.length() > 150) {
                throw new IllegalArgumentException("invalid refund form");
            }
            CreateRefundRequest command = new CreateRefundRequest(packageUuid, reason.trim(), sessions,
                    bankName.trim(), bankBin, accountNumber, holder, version);
            financeCommands.execute(user.id(), "STUDENT_REFUND_CREATE", key, command, RefundRequestView.class,
                    () -> ApiResponse.ok(refunds.createRefund(user.id(), command)));
            redirect.addFlashAttribute("notice", "Đã gửi yêu cầu hoàn tiền. Gói học đang chờ xử lý.");
            return "redirect:/student/requests?tab=refund";
        } catch (RuntimeException ex) {
            logFailure("refund", ex);
            redirect.addFlashAttribute("actionError", safeMessage(ex));
            return "redirect:/student/requests?tab=refund&packageId=" + packageUuid;
        }
    }

    @PostMapping("/student/requests/extension")
    public String createExtension(@AuthenticationPrincipal AuthenticatedUser user,
                                  @RequestParam String studentPackageId,
                                  @RequestParam String packageVersion,
                                  @RequestParam String idempotencyKey,
                                  @RequestParam String requestedExpiryDate,
                                  @RequestParam String reason,
                                  RedirectAttributes redirect) {
        requireStudent(user);
        UUID packageUuid = parseId(studentPackageId);
        try {
            long version = Long.parseLong(packageVersion);
            UUID key = UUID.fromString(idempotencyKey);
            if (reason == null || reason.trim().length() < 10 || reason.length() > 1000) {
                throw new IllegalArgumentException("invalid extension form");
            }
            Instant expiry = LocalDate.parse(requestedExpiryDate).atStartOfDay(VIETNAM_ZONE).toInstant();
            CreateExtensionRequest command = new CreateExtensionRequest(packageUuid, reason.trim(), expiry, version);
            financeCommands.execute(user.id(), "STUDENT_EXTENSION_CREATE", key, command, ExtensionRequestView.class,
                    () -> ApiResponse.ok(extensions.createExtension(user.id(), command)));
            redirect.addFlashAttribute("notice", "Đã gửi yêu cầu gia hạn để quản trị viên xem xét.");
            return "redirect:/student/requests?tab=extension";
        } catch (RuntimeException ex) {
            logFailure("extension", ex);
            redirect.addFlashAttribute("actionError", safeMessage(ex));
            return "redirect:/student/requests?tab=extension&packageId=" + packageUuid;
        }
    }

    private void loadSelectedList(UUID studentId, String tab, int page, Model model) {
        PageRequest pageable = PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
        try {
            List<RequestRow> rows;
            Page<?> result;
            if ("refund".equals(tab)) {
                Page<RefundRequestView> data = refunds.findStudentRefunds(studentId, pageable);
                rows = data.getContent().stream().map(this::refundRow).toList(); result = data;
            } else if ("extension".equals(tab)) {
                Page<ExtensionRequestView> data = extensions.findStudentExtensions(studentId, pageable);
                rows = data.getContent().stream().map(this::extensionRow).toList(); result = data;
            } else {
                Page<TrialRequestView> data = trials.findForStudent(studentId, null, pageable);
                rows = data.getContent().stream().map(this::trialRow).toList(); result = data;
            }
            if (page > 0 && result.getTotalElements() > 0 && result.isEmpty()) {
                model.addAttribute("pageRedirect", true);
                model.addAttribute("lastPage", Math.max(0, result.getTotalPages() - 1));
                return;
            }
            model.addAttribute("requestRows", rows);
            model.addAttribute("currentPage", result.getNumber());
            model.addAttribute("totalPages", result.getTotalPages());
            model.addAttribute("totalElements", result.getTotalElements());
            model.addAttribute("hasPrevious", result.hasPrevious());
            model.addAttribute("hasNext", result.hasNext());
        } catch (RuntimeException ex) {
            log.warn("Student {} requests page is unavailable", tab, ex);
            model.addAttribute("listUnavailable", true);
        }
    }

    private void loadPackageContext(UUID studentId, String rawPackageId, Model model) {
        if (rawPackageId == null || rawPackageId.isBlank()) return;
        UUID packageId;
        try { packageId = UUID.fromString(rawPackageId); }
        catch (IllegalArgumentException ex) { model.addAttribute("packageUnavailable", true); return; }
        try {
            StudentOwnedPackageView pkg = packages.detail(packageId, studentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            model.addAttribute("packageRow", new PackageRow(pkg, dateOnlyLabel(pkg.expiresAt())));
            model.addAttribute("canRequestRefund", List.of("ACTIVE", "LOCKED_EXPIRED").contains(pkg.status()) && pkg.remainingSessions() > 0);
            model.addAttribute("canRequestExtension", "LOCKED_EXPIRED".equals(pkg.status()));
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("Unable to load owned package for request form", ex);
            model.addAttribute("packageUnavailable", true);
        }
    }

    private RequestRow refundRow(RefundRequestView value) {
        String sessions = value.approvedSessions() > 0 && value.approvedSessions() != value.requestedSessions()
                ? value.requestedSessions() + " buổi yêu cầu · " + value.approvedSessions() + " buổi được duyệt"
                : value.requestedSessions() + " buổi";
        String amount = value.refundAmountVnd() == null ? "Số tiền đang được xem xét"
                : String.format(Locale.forLanguageTag("vi-VN"), "%,d ₫", value.refundAmountVnd());
        String bank = value.bankName() == null ? "" : value.bankName() + (value.accountNumberMasked() == null ? "" : " · " + value.accountNumberMasked());
        String detail = join(sessions, amount, bank, value.adminNote() == null ? value.reason() : value.adminNote());
        return new RequestRow(shortId(value.id()), "Hoàn tiền · " + amount, detail, refundStatus(value.status().name()), dateLabel(value.createdAt()));
    }

    private RequestRow extensionRow(ExtensionRequestView value) {
        String detail = join("Hạn đề nghị: " + dateOnlyLabel(value.requestedExpiryDate()),
                value.approvedExpiryDate() == null ? null : "Hạn được duyệt: " + dateOnlyLabel(value.approvedExpiryDate()),
                value.adminNote() == null ? value.reason() : value.adminNote());
        return new RequestRow(shortId(value.id()), "Gia hạn gói học", detail, extensionStatus(value.status().name()), dateLabel(value.createdAt()));
    }

    private RequestRow trialRow(TrialRequestView value) {
        String detail = join(value.note(), value.rejectionReason() == null ? null : "Lý do từ chối: " + value.rejectionReason());
        return new RequestRow(shortId(value.id()), "Học thử · " + dateLabel(value.preferredStartTime()), detail,
                trialStatus(value.status().name()), dateLabel(value.createdAt()));
    }

    private String refundStatus(String status) { return switch (status) { case "PENDING" -> "Chờ xử lý"; case "APPROVED" -> "Đã duyệt"; case "PROCESSING" -> "Đang xử lý"; case "REFUNDED" -> "Đã hoàn tiền"; case "REJECTED" -> "Bị từ chối"; default -> "Đang cập nhật"; }; }
    private String extensionStatus(String status) { return switch (status) { case "PENDING" -> "Chờ xử lý"; case "APPROVED" -> "Đã gia hạn"; case "REJECTED" -> "Bị từ chối"; default -> "Đang cập nhật"; }; }
    private String trialStatus(String status) { return switch (status) { case "PENDING" -> "Chờ gia sư"; case "ACCEPTED" -> "Đã chấp nhận"; case "REJECTED" -> "Bị từ chối"; case "CANCELLED" -> "Đã hủy"; default -> "Đang cập nhật"; }; }
    private String dateLabel(Instant value) { return value == null ? "Chưa xác định" : DATE_TIME.format(value); }
    private String dateOnlyLabel(Instant value) { return value == null ? "Chưa xác định" : DATE.format(value); }
    private String shortId(UUID id) { return id == null ? "—" : "#" + id.toString().substring(0, 8); }
    private String join(String... values) { return java.util.Arrays.stream(values).filter(value -> value != null && !value.isBlank()).reduce((a, b) -> a + " · " + b).orElse("Chưa có ghi chú"); }
    private String normalizeTab(String value) { return value == null ? "trial" : switch (value.trim().toLowerCase(Locale.ROOT)) { case "refund" -> "refund"; case "extension" -> "extension"; default -> "trial"; }; }
    private UUID parseId(String raw) { try { return UUID.fromString(raw); } catch (RuntimeException ex) { throw new ResponseStatusException(HttpStatus.NOT_FOUND); } }
    private String safeMessage(RuntimeException ex) { return ex instanceof BusinessException business ? business.getErrorCode().getDefaultMessage() : "Chưa gửi được yêu cầu. Kiểm tra thông tin rồi thử lại."; }
    private String ownedPackageQuery(String rawId) {
        if (rawId == null || rawId.isBlank()) return "";
        try { return "&packageId=" + UUID.fromString(rawId); }
        catch (IllegalArgumentException ignored) { return ""; }
    }
    private void logFailure(String kind, RuntimeException ex) {
        if (ex instanceof BusinessException) log.info("Student {} request was rejected", kind);
        else log.warn("Student {} request failed ({})", kind, ex.getClass().getSimpleName());
    }
    private void requireStudent(AuthenticatedUser user) { if (user == null || !"STUDENT".equals(user.role())) throw new AccessDeniedException("Student role required"); }

    public record Tab(String value, String label) {}
    public record RequestRow(String shortId, String title, String detail, String status, String dateLabel) {}
    public record PackageRow(StudentOwnedPackageView data, String expiryLabel) {}
}
