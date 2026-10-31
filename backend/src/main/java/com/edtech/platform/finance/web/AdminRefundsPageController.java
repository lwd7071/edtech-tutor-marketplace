package com.edtech.platform.finance.web;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.storage.CredentialProofStorage;
import com.edtech.platform.finance.command.ApproveRefundCommand;
import com.edtech.platform.finance.command.CompleteTransferCommand;
import com.edtech.platform.finance.command.RejectFinanceCommand;
import com.edtech.platform.finance.dto.response.RefundRequestView;
import com.edtech.platform.finance.service.FinanceCommandExecutor;
import com.edtech.platform.finance.service.FinanceProofStorage;
import com.edtech.platform.finance.service.RefundService;
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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class AdminRefundsPageController {
    private static final Logger log = LoggerFactory.getLogger(AdminRefundsPageController.class);
    private static final int PAGE_SIZE = 20;
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm", Locale.forLanguageTag("vi-VN")).withZone(VIETNAM_ZONE);
    private static final DateTimeFormatter LOCAL_INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final List<String> FILTERS = List.of("ALL", "PENDING", "APPROVED", "PROCESSING", "REFUNDED", "REJECTED", "FAILED");

    private final RefundService refunds;
    private final FinanceProofStorage proofStorage;
    private final CredentialProofStorage proofReader;
    private final FinanceCommandExecutor commands;

    @ModelAttribute
    void preventPrivatePageCaching(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, max-age=0");
    }

    @GetMapping("/admin/refunds")
    public String list(@AuthenticationPrincipal AuthenticatedUser user,
                       @RequestParam(defaultValue = "ALL") String status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        requireAdmin(user);
        String selected = normalizeStatus(status);
        int safePage = Math.max(0, Math.min(page, 100_000));
        model.addAttribute("activeWorkspaceNav", "refunds");
        model.addAttribute("selectedStatus", selected);
        model.addAttribute("statusOptions", FILTERS.stream().map(value -> new StatusOption(value, statusLabel(value))).toList());
        model.addAttribute("pageNumber", safePage);
        model.addAttribute("pageSize", PAGE_SIZE);
        model.addAttribute("refundsUnavailable", false);
        model.addAttribute("refundRows", List.of());
        model.addAttribute("totalElements", 0L);
        model.addAttribute("totalPages", 0);
        model.addAttribute("hasPrevious", false);
        model.addAttribute("hasNext", false);
        try {
            Page<RefundRequestView> result = refunds.findAdminRefunds(
                    "ALL".equals(selected) ? null : selected,
                    PageRequest.of(safePage, PAGE_SIZE, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
            if (safePage > 0 && result.getTotalElements() > 0 && result.isEmpty()) {
                return "redirect:/admin/refunds?status=" + selected + "&page=" + Math.max(0, result.getTotalPages() - 1);
            }
            List<RefundRow> rows = result.getContent().stream().map(value -> row(value)).toList();
            model.addAttribute("refundRows", rows);
            model.addAttribute("totalElements", result.getTotalElements());
            model.addAttribute("totalPages", result.getTotalPages());
            model.addAttribute("hasPrevious", result.hasPrevious());
            model.addAttribute("hasNext", result.hasNext());
            model.addAttribute("idempotencyKeys", rows.stream().collect(Collectors.toMap(
                    row -> row.data().id().toString(), row -> UUID.randomUUID().toString())));
        } catch (RuntimeException exception) {
            log.warn("Admin refund queue is unavailable");
            model.addAttribute("refundRows", List.of());
            model.addAttribute("refundsUnavailable", true);
        }
        return "workspace/admin-refunds";
    }

    @PostMapping("/admin/refunds/{id}/approve")
    public String approve(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                          @RequestParam int approvedSessions, @RequestParam(required = false, defaultValue = "") String adminNote,
                          @RequestParam long version, @RequestParam String idempotencyKey,
                          @RequestParam(defaultValue = "ALL") String status, @RequestParam(defaultValue = "0") int page,
                          RedirectAttributes redirect) {
        requireAdmin(user);
        String destination = location(status, page);
        String cleanNote = adminNote.trim();
        if (approvedSessions < 1) {
            redirect.addFlashAttribute("actionError", "Chọn số buổi duyệt hợp lệ.");
            redirect.addFlashAttribute("openApproveId", id.toString());
            redirect.addFlashAttribute("approveSessions", approvedSessions);
            redirect.addFlashAttribute("approveNote", adminNote);
            return "redirect:" + destination;
        }
        try {
            UUID key = UUID.fromString(idempotencyKey);
            ApproveRefundCommand command = new ApproveRefundCommand(approvedSessions, blankToNull(cleanNote), version);
            commands.execute(user.id(), "ADMIN_REFUND_APPROVE", key, new RefundActionFingerprint(id, command), RefundRequestView.class,
                    () -> ApiResponse.ok(refunds.approveRefund(user.id(), id, command)));
            redirect.addFlashAttribute("actionSuccess", "Đã duyệt số buổi hoàn tiền.");
        } catch (RuntimeException exception) {
            logFailure("approval", exception);
            redirect.addFlashAttribute("actionError", safeMessage(exception));
            redirect.addFlashAttribute("openApproveId", id.toString());
            redirect.addFlashAttribute("approveSessions", approvedSessions);
            redirect.addFlashAttribute("approveNote", adminNote);
        }
        return "redirect:" + destination;
    }

    @PostMapping("/admin/refunds/{id}/reject")
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
            UUID key = UUID.fromString(idempotencyKey);
            RejectFinanceCommand command = new RejectFinanceCommand(cleanReason, version);
            commands.execute(user.id(), "ADMIN_REFUND_REJECT", key, new RefundActionFingerprint(id, command), RefundRequestView.class,
                    () -> ApiResponse.ok(refunds.rejectRefund(user.id(), id, command)));
            redirect.addFlashAttribute("actionSuccess", "Đã từ chối yêu cầu hoàn tiền.");
        } catch (RuntimeException exception) {
            logFailure("rejection", exception);
            redirect.addFlashAttribute("actionError", safeMessage(exception));
            redirect.addFlashAttribute("openRejectId", id.toString());
            redirect.addFlashAttribute("rejectReason", reason);
        }
        return "redirect:" + destination;
    }

    @PostMapping("/admin/refunds/{id}/complete")
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
        if (proof == null || proof.isEmpty() || proof.getSize() > 10L * 1024 * 1024) {
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
            commands.execute(user.id(), "ADMIN_REFUND_COMPLETE", key, fingerprint, RefundRequestView.class, () -> {
                var uploaded = proofStorage.upload(proof, "refund", id);
                CompleteTransferCommand command = new CompleteTransferCommand(reference, transferInstant,
                        uploaded.publicId(), uploaded.secureUrl(), version);
                return ApiResponse.ok(refunds.completeRefund(user.id(), id, command));
            });
            redirect.addFlashAttribute("actionSuccess", "Đã ghi nhận hoàn tiền và lưu chứng từ.");
        } catch (RuntimeException | IOException exception) {
            logFailure("completion", exception);
            redirect.addFlashAttribute("actionError", safeMessage(exception));
            redirect.addFlashAttribute("openCompleteId", id.toString());
            redirect.addFlashAttribute("transferReference", bankReference);
            redirect.addFlashAttribute("transferTime", transferredAt);
        }
        return "redirect:" + destination;
    }

    @GetMapping("/admin/refunds/{id}/proof")
    public ResponseEntity<byte[]> proof(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        requireAdmin(user);
        try {
            String publicId = refunds.adminProofPublicId(id);
            CredentialProofStorage.DownloadedFile file = proofReader.downloadLegacy(publicId);
            MediaType mediaType = switch (file.mimeType() == null ? "" : file.mimeType().toLowerCase(Locale.ROOT)) {
                case "application/pdf" -> MediaType.APPLICATION_PDF;
                case "image/jpeg" -> MediaType.IMAGE_JPEG;
                case "image/png" -> MediaType.IMAGE_PNG;
                default -> MediaType.APPLICATION_OCTET_STREAM;
            };
            return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store, max-age=0")
                    .header("X-Content-Type-Options", "nosniff")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=refund-proof")
                    .contentType(mediaType).body(file.bytes());
        } catch (BusinessException exception) {
            log.info("Admin refund proof was unavailable");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).header(HttpHeaders.CACHE_CONTROL, "no-store, max-age=0")
                    .header("X-Content-Type-Options", "nosniff").build();
        } catch (IOException exception) {
            log.warn("Admin refund proof storage is unavailable");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).header(HttpHeaders.CACHE_CONTROL, "no-store, max-age=0")
                    .header("X-Content-Type-Options", "nosniff").build();
        }
    }

    private RefundRow row(RefundRequestView value) {
        return new RefundRow(value, statusLabel(value.status().name()), dateLabel(value.createdAt()),
                money(value.refundAmountVnd()), value.proofUrl() != null && !value.proofUrl().isBlank());
    }

    private String normalizeStatus(String value) {
        String normalized = value == null ? "ALL" : value.trim().toUpperCase(Locale.ROOT);
        return FILTERS.contains(normalized) ? normalized : "ALL";
    }

    private String location(String status, int page) {
        return "/admin/refunds?status=" + normalizeStatus(status) + "&page=" + Math.max(0, Math.min(page, 100_000));
    }

    private String statusLabel(String value) {
        return switch (value) {
            case "ALL" -> "Tất cả trạng thái"; case "PENDING" -> "Chờ duyệt"; case "APPROVED" -> "Đã duyệt";
            case "PROCESSING" -> "Đang xử lý"; case "REFUNDED" -> "Đã hoàn tiền"; case "REJECTED" -> "Đã từ chối";
            case "FAILED" -> "Thất bại"; default -> "Đang cập nhật";
        };
    }

    private String dateLabel(Instant value) { return value == null ? "Chưa xác định" : DATE_TIME.format(value); }
    private String money(Long amount) { return amount == null ? "Chưa xác định" : String.format(Locale.forLanguageTag("vi-VN"), "%,d ₫", amount); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value; }
    private String safeMessage(Exception exception) {
        return exception instanceof BusinessException business ? business.getErrorCode().getDefaultMessage()
                : "Không thể cập nhật yêu cầu. Hãy tải lại danh sách để kiểm tra trạng thái mới nhất.";
    }
    private String sha256(byte[] bytes) throws IOException {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (Exception exception) { throw new IOException("proof fingerprint unavailable", exception); }
    }
    private void logFailure(String action, Exception exception) {
        if (exception instanceof BusinessException) log.info("Admin refund {} was rejected", action);
        else log.warn("Admin refund {} failed ({})", action, exception.getClass().getSimpleName());
    }
    private void requireAdmin(AuthenticatedUser user) {
        if (user == null || !"ADMIN".equals(user.role())) throw new AccessDeniedException("Admin workspace only");
    }

    public record StatusOption(String value, String label) {}
    public record RefundRow(RefundRequestView data, String statusLabel, String dateLabel, String amountLabel, boolean proofRecorded) {}
    private record RefundActionFingerprint(UUID refundId, Object action) {}
    private record CompletionFingerprint(UUID refundId, String bankReference, Instant transferredAt, long version,
                                         String filename, long size, String sha256) {}
}
