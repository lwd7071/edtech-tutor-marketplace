package com.edtech.platform.finance.web;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.storage.CredentialProofStorage;
import com.edtech.platform.finance.command.CompleteTransferCommand;
import com.edtech.platform.finance.command.ProcessPayoutCommand;
import com.edtech.platform.finance.command.RejectFinanceCommand;
import com.edtech.platform.finance.dto.response.PayoutRequestView;
import com.edtech.platform.finance.service.FinanceCommandExecutor;
import com.edtech.platform.finance.service.FinanceProofStorage;
import com.edtech.platform.finance.service.PayoutService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class AdminPayoutsPageController {
    private static final Logger log = LoggerFactory.getLogger(AdminPayoutsPageController.class);
    private static final int PAGE_SIZE = 20;
    private static final long MAX_PROOF_SIZE = 10L * 1024 * 1024;
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm", Locale.forLanguageTag("vi-VN")).withZone(VIETNAM_ZONE);
    private static final DateTimeFormatter LOCAL_INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final DateTimeFormatter LOCAL_VALUE = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm").withZone(VIETNAM_ZONE);
    private static final List<String> FILTERS = List.of("ALL", "PENDING", "PROCESSING", "SUCCEEDED", "REJECTED", "FAILED");

    private final PayoutService payouts;
    private final FinanceProofStorage proofStorage;
    private final CredentialProofStorage proofReader;
    private final FinanceCommandExecutor commands;

    @ModelAttribute
    void preventPrivatePageCaching(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store, max-age=0");
    }

    @GetMapping("/admin/payouts")
    public String list(@AuthenticationPrincipal AuthenticatedUser user,
                       @RequestParam(defaultValue = "ALL") String status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        requireAdmin(user);
        String selected = normalizeStatus(status);
        int safePage = Math.max(0, Math.min(page, 100_000));
        model.addAttribute("activeWorkspaceNav", "payouts");
        model.addAttribute("selectedStatus", selected);
        model.addAttribute("statusOptions", FILTERS.stream().map(value -> new StatusOption(value, statusLabel(value))).toList());
        model.addAttribute("pageNumber", safePage);
        model.addAttribute("payoutsUnavailable", false);
        model.addAttribute("payoutRows", List.of());
        model.addAttribute("totalElements", 0L);
        model.addAttribute("totalPages", 0);
        model.addAttribute("hasPrevious", false);
        model.addAttribute("hasNext", false);
        try {
            Page<PayoutService.AdminPayoutWorkspaceRow> result = payouts.findAdminPayoutWorkspace(
                    "ALL".equals(selected) ? null : selected,
                    PageRequest.of(safePage, PAGE_SIZE, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
            if (safePage > 0 && result.getTotalElements() > 0 && result.isEmpty()) {
                return "redirect:/admin/payouts?status=" + selected + "&page=" + Math.max(0, result.getTotalPages() - 1);
            }
            List<PayoutRow> rows = result.getContent().stream().map(this::row).toList();
            model.addAttribute("payoutRows", rows);
            model.addAttribute("totalElements", result.getTotalElements());
            model.addAttribute("totalPages", result.getTotalPages());
            model.addAttribute("hasPrevious", result.hasPrevious());
            model.addAttribute("hasNext", result.hasNext());
            model.addAttribute("idempotencyKeys", rows.stream().collect(Collectors.toMap(
                    row -> row.data().id().toString(), row -> UUID.randomUUID().toString())));
        } catch (RuntimeException exception) {
            log.warn("Admin payout queue is unavailable");
            model.addAttribute("payoutRows", List.of());
            model.addAttribute("payoutsUnavailable", true);
        }
        return "workspace/admin-payouts";
    }

    @PostMapping("/admin/payouts/{id}/process")
    public String process(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                          @RequestParam long version, @RequestParam String idempotencyKey,
                          @RequestParam(defaultValue = "ALL") String status, @RequestParam(defaultValue = "0") int page,
                          RedirectAttributes redirect) {
        requireAdmin(user);
        String destination = location(status, page);
        try {
            ProcessPayoutCommand command = new ProcessPayoutCommand(version);
            commands.execute(user.id(), "ADMIN_PAYOUT_PROCESS", UUID.fromString(idempotencyKey),
                    new PayoutActionFingerprint(id, command), PayoutRequestView.class,
                    () -> ApiResponse.ok(payouts.processPayout(user.id(), id, command)));
            redirect.addFlashAttribute("actionSuccess", "Đã chuyển yêu cầu sang trạng thái đang xử lý.");
        } catch (RuntimeException exception) {
            logFailure("processing", exception);
            redirect.addFlashAttribute("actionError", safeMessage(exception));
        }
        return "redirect:" + destination;
    }

    @PostMapping("/admin/payouts/{id}/reject")
    public String reject(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                         @RequestParam(required = false, defaultValue = "") String reason,
                         @RequestParam long version, @RequestParam String idempotencyKey,
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
            RejectFinanceCommand command = new RejectFinanceCommand(cleanReason, version);
            commands.execute(user.id(), "ADMIN_PAYOUT_REJECT", UUID.fromString(idempotencyKey),
                    new PayoutActionFingerprint(id, command), PayoutRequestView.class,
                    () -> ApiResponse.ok(payouts.rejectPayout(user.id(), id, command)));
            redirect.addFlashAttribute("actionSuccess", "Đã từ chối yêu cầu rút tiền và hoàn số dư về ví gia sư.");
        } catch (RuntimeException exception) {
            logFailure("rejection", exception);
            redirect.addFlashAttribute("actionError", safeMessage(exception));
            redirect.addFlashAttribute("openRejectId", id.toString());
            redirect.addFlashAttribute("rejectReason", reason);
        }
        return "redirect:" + destination;
    }

    @PostMapping("/admin/payouts/{id}/complete")
    public String complete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                           @RequestParam(required = false, defaultValue = "") String bankReference,
                           @RequestParam(required = false, defaultValue = "") String transferredAt,
                           @RequestParam long version, @RequestParam String idempotencyKey,
                           @RequestParam MultipartFile proof,
                           @RequestParam(defaultValue = "ALL") String status, @RequestParam(defaultValue = "0") int page,
                           RedirectAttributes redirect) {
        requireAdmin(user);
        String destination = location(status, page);
        String reference = bankReference.trim();
        LocalDateTime localTransferTime;
        try { localTransferTime = LocalDateTime.parse(transferredAt, LOCAL_INPUT); }
        catch (RuntimeException exception) { localTransferTime = null; }
        if (reference.isBlank() || reference.length() > 255 || localTransferTime == null) {
            redirect.addFlashAttribute("actionError", "Nhập mã giao dịch và thời gian chuyển khoản hợp lệ.");
            redirect.addFlashAttribute("openCompleteId", id.toString());
            redirect.addFlashAttribute("transferReference", bankReference);
            redirect.addFlashAttribute("transferTime", transferredAt);
            return "redirect:" + destination;
        }
        if (proof == null || proof.isEmpty() || proof.getSize() > MAX_PROOF_SIZE) {
            redirect.addFlashAttribute("actionError", proof == null || proof.isEmpty()
                    ? "Chọn chứng từ chuyển khoản." : "Chứng từ không được vượt quá 10 MiB.");
            redirect.addFlashAttribute("openCompleteId", id.toString());
            redirect.addFlashAttribute("transferReference", bankReference);
            redirect.addFlashAttribute("transferTime", transferredAt);
            return "redirect:" + destination;
        }
        try {
            UUID key = UUID.fromString(idempotencyKey);
            Instant transferInstant = localTransferTime.atZone(VIETNAM_ZONE).toInstant();
            String proofHash = sha256(proof.getBytes());
            CompletionFingerprint fingerprint = new CompletionFingerprint(id, reference, transferInstant, version,
                    proof.getOriginalFilename(), proof.getSize(), proofHash);
            commands.execute(user.id(), "ADMIN_PAYOUT_COMPLETE", key, fingerprint, PayoutRequestView.class, () -> {
                var uploaded = proofStorage.upload(proof, "payout", id);
                CompleteTransferCommand command = new CompleteTransferCommand(reference, transferInstant,
                        uploaded.publicId(), uploaded.secureUrl(), version);
                return ApiResponse.ok(payouts.completePayout(user.id(), id, command));
            });
            redirect.addFlashAttribute("actionSuccess", "Đã ghi nhận chuyển khoản và lưu chứng từ.");
        } catch (RuntimeException | IOException exception) {
            logFailure("completion", exception);
            redirect.addFlashAttribute("actionError", safeMessage(exception));
            redirect.addFlashAttribute("openCompleteId", id.toString());
            redirect.addFlashAttribute("transferReference", bankReference);
            redirect.addFlashAttribute("transferTime", transferredAt);
        }
        return "redirect:" + destination;
    }

    @GetMapping("/admin/payouts/{id}/proof")
    public ResponseEntity<byte[]> proof(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        requireAdmin(user);
        try {
            String publicId = payouts.adminProofPublicId(id);
            CredentialProofStorage.DownloadedFile file = proofReader.downloadLegacy(publicId);
            MediaType mediaType = switch (file.mimeType() == null ? "" : file.mimeType().toLowerCase(Locale.ROOT)) {
                case "application/pdf" -> MediaType.APPLICATION_PDF;
                case "image/jpeg" -> MediaType.IMAGE_JPEG;
                case "image/png" -> MediaType.IMAGE_PNG;
                default -> MediaType.APPLICATION_OCTET_STREAM;
            };
            return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store, max-age=0")
                    .header("X-Content-Type-Options", "nosniff")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=payout-proof")
                    .contentType(mediaType).body(file.bytes());
        } catch (BusinessException exception) {
            log.info("Admin payout proof was unavailable");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).header(HttpHeaders.CACHE_CONTROL, "no-store, max-age=0")
                    .header("X-Content-Type-Options", "nosniff").build();
        } catch (IOException exception) {
            log.warn("Admin payout proof storage is unavailable");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).header(HttpHeaders.CACHE_CONTROL, "no-store, max-age=0")
                    .header("X-Content-Type-Options", "nosniff").build();
        }
    }

    private PayoutRow row(PayoutService.AdminPayoutWorkspaceRow value) {
        PayoutRequestView payout = value.payout();
        return new PayoutRow(payout, statusLabel(payout.status().name()), dateLabel(payout.createdAt()),
                money(payout.amountVnd()), payout.transferredAt() == null ? null : dateLabel(payout.transferredAt()),
                payout.transferredAt() == null ? "" : LOCAL_VALUE.format(payout.transferredAt()),
                payout.proofUrl() != null && !payout.proofUrl().isBlank(), value.bankName(),
                value.accountHolderName(), value.accountNumber(), value.bankDetailsAvailable());
    }
    private String normalizeStatus(String value) {
        String normalized = value == null ? "ALL" : value.trim().toUpperCase(Locale.ROOT);
        return FILTERS.contains(normalized) ? normalized : "ALL";
    }
    private String location(String status, int page) {
        return "/admin/payouts?status=" + normalizeStatus(status) + "&page=" + Math.max(0, Math.min(page, 100_000));
    }
    private String statusLabel(String value) {
        return switch (value) { case "ALL" -> "Tất cả trạng thái"; case "PENDING" -> "Chờ xử lý";
            case "PROCESSING" -> "Đang chuyển khoản"; case "SUCCEEDED" -> "Đã chuyển";
            case "REJECTED" -> "Đã từ chối"; case "FAILED" -> "Thất bại"; default -> "Đang cập nhật"; };
    }
    private String dateLabel(Instant value) { return value == null ? "Chưa xác định" : DATE_TIME.format(value); }
    private String money(long amount) { return String.format(Locale.forLanguageTag("vi-VN"), "%,d ₫", amount); }
    private String safeMessage(Exception exception) {
        return exception instanceof BusinessException business ? business.getErrorCode().getDefaultMessage()
                : "Không thể cập nhật yêu cầu. Hãy tải lại danh sách để kiểm tra trạng thái mới nhất.";
    }
    private String sha256(byte[] bytes) throws IOException {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (Exception exception) { throw new IOException("proof fingerprint unavailable", exception); }
    }
    private void logFailure(String action, Exception exception) {
        if (exception instanceof BusinessException) log.info("Admin payout {} was rejected", action);
        else log.warn("Admin payout {} failed ({})", action, exception.getClass().getSimpleName());
    }
    private void requireAdmin(AuthenticatedUser user) {
        if (user == null || !"ADMIN".equals(user.role())) throw new AccessDeniedException("Admin workspace only");
    }

    public record StatusOption(String value, String label) {}
    public record PayoutRow(PayoutRequestView data, String statusLabel, String createdLabel, String amountLabel,
                            String transferredLabel, String transferredInput, boolean proofRecorded,
                            String bankName, String accountHolderName, String accountNumber,
                            boolean bankDetailsAvailable) {}
    private record PayoutActionFingerprint(UUID payoutId, Object action) {}
    private record CompletionFingerprint(UUID payoutId, String bankReference, Instant transferredAt, long version,
                                         String filename, long size, String sha256) {}
}
