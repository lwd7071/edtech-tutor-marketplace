package com.edtech.platform.finance.web;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.RateLimiterService;
import com.edtech.platform.finance.domain.PayoutStatus;
import com.edtech.platform.finance.dto.request.CreatePayoutRequest;
import com.edtech.platform.finance.dto.request.UpsertBankAccountRequest;
import com.edtech.platform.finance.dto.response.BankAccountView;
import com.edtech.platform.finance.dto.response.LedgerEntryView;
import com.edtech.platform.finance.dto.response.PayoutRequestView;
import com.edtech.platform.finance.dto.response.WalletView;
import com.edtech.platform.finance.service.BankAccountService;
import com.edtech.platform.finance.service.FinanceCommandExecutor;
import com.edtech.platform.finance.service.PayoutService;
import com.edtech.platform.finance.service.WalletQueryService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Controller
@Validated
@RequiredArgsConstructor
public class TeacherFinancePageController {
    private static final int PAGE_SIZE = 20;
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm", Locale.forLanguageTag("vi-VN")).withZone(VIETNAM_ZONE);

    private final WalletQueryService walletQueries;
    private final BankAccountService bankAccounts;
    private final PayoutService payouts;
    private final FinanceCommandExecutor financeCommands;
    private final RateLimiterService rateLimiter;
    @ModelAttribute
    void preventPrivatePageCaching(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, max-age=0");
    }

    @GetMapping("/teacher/wallet")
    public String wallet(@AuthenticationPrincipal AuthenticatedUser user,
                         @RequestParam(defaultValue = "0") int page, Model model) {
        if (page < 0) return "redirect:/teacher/wallet";
        model.addAttribute("activeWorkspaceNav", "wallet");
        model.addAttribute("walletUnavailable", false);
        model.addAttribute("ledgerUnavailable", false);
        model.addAttribute("pageSize", PAGE_SIZE);
        model.addAttribute("wallet", null);
        model.addAttribute("ledgerRows", List.of());
        try {
            WalletView wallet = walletQueries.getOrCreateWallet(user.id());
            model.addAttribute("wallet", wallet);
        } catch (RuntimeException ex) {
            model.addAttribute("walletUnavailable", true);
        }
        try {
            Page<LedgerEntryView> result = walletQueries.getLedgerEntries(user.id(), PageRequest.of(Math.max(0, page), PAGE_SIZE,
                    Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
            if (page > 0 && result.getTotalElements() > 0 && result.isEmpty()) {
                return "redirect:/teacher/wallet?page=" + Math.max(0, result.getTotalPages() - 1);
            }
            model.addAttribute("ledgerRows", result.getContent().stream().map(row -> new LedgerRow(row,
                    money(row.amountVnd()), DATE_TIME.format(row.createdAt()), row.direction().name().equals("CREDIT") ? "Cộng" : "Trừ")).toList());
            model.addAttribute("currentPage", result.getNumber());
            model.addAttribute("totalPages", result.getTotalPages());
            model.addAttribute("totalElements", result.getTotalElements());
            model.addAttribute("hasPrevious", result.hasPrevious());
            model.addAttribute("hasNext", result.hasNext());
        } catch (RuntimeException ex) {
            model.addAttribute("ledgerUnavailable", true);
            model.addAttribute("ledgerRows", List.of());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalElements", 0L);
            model.addAttribute("hasPrevious", false);
            model.addAttribute("hasNext", false);
        }
        return "workspace/teacher-wallet";
    }

    @GetMapping("/teacher/bank-accounts")
    public String bankAccounts(@AuthenticationPrincipal AuthenticatedUser user, Model model) {
        model.addAttribute("activeWorkspaceNav", "bank-accounts");
        model.addAttribute("bankAccountsUnavailable", false);
        if (!model.containsAttribute("createIdempotencyKey")) {
            model.addAttribute("createIdempotencyKey", UUID.randomUUID());
        }
        try {
            model.addAttribute("bankAccountRows", bankAccounts.findTeacherAccounts(user.id()));
        } catch (RuntimeException ex) {
            model.addAttribute("bankAccountsUnavailable", true);
            model.addAttribute("bankAccountRows", List.of());
        }
        return "workspace/teacher-bank-accounts";
    }

    @PostMapping("/teacher/bank-accounts")
    public String createBankAccount(@AuthenticationPrincipal AuthenticatedUser user,
                                    @RequestParam String bankBin, @RequestParam String bankName,
                                    @RequestParam String accountNumber, @RequestParam String accountHolderName,
                                    @RequestParam(required = false, defaultValue = "false") boolean isDefault,
                                    @RequestParam String idempotencyKey, RedirectAttributes redirect) {
        try {
            UpsertBankAccountRequest request = new UpsertBankAccountRequest(bankBin, bankName, accountNumber,
                    accountHolderName, isDefault, 0L);
            financeCommands.execute(user.id(), "TEACHER_BANK_ACCOUNT_CREATE", UUID.fromString(idempotencyKey),
                    request, BankAccountView.class,
                    () -> ApiResponse.created(bankAccounts.createAccount(user.id(), request)));
            redirect.addFlashAttribute("financeNotice", "Đã thêm tài khoản nhận tiền.");
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("financeError", publicMessage(ex));
            redirect.addFlashAttribute("createIdempotencyKey", UUID.randomUUID());
            redirect.addFlashAttribute("bankForm", new BankForm(bankBin, bankName, accountHolderName, isDefault));
            // Never put the full account number in a redirect, flash attribute, log, or HTML response.
        }
        return "redirect:/teacher/bank-accounts#bank-action-message";
    }

    @PostMapping("/teacher/bank-accounts/{id}/update")
    public String updateBankAccount(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user,
                                    @RequestParam String bankBin, @RequestParam String bankName,
                                    @RequestParam String accountNumber, @RequestParam String accountHolderName,
                                    @RequestParam(required = false, defaultValue = "false") boolean isDefault,
                                    @RequestParam long version, RedirectAttributes redirect) {
        try {
            bankAccounts.updateAccount(user.id(), id, new UpsertBankAccountRequest(bankBin, bankName,
                    accountNumber, accountHolderName, isDefault, version));
            redirect.addFlashAttribute("financeNotice", "Đã cập nhật tài khoản nhận tiền.");
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("financeError", publicMessage(ex));
        }
        return "redirect:/teacher/bank-accounts#bank-action-message";
    }

    @PostMapping("/teacher/bank-accounts/{id}/delete")
    public String deleteBankAccount(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user,
                                    @RequestParam long version, RedirectAttributes redirect) {
        try {
            bankAccounts.deleteAccount(user.id(), id, "\"" + version + "\"");
            redirect.addFlashAttribute("financeNotice", "Đã xóa tài khoản nhận tiền.");
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("financeError", publicMessage(ex));
        }
        return "redirect:/teacher/bank-accounts#bank-action-message";
    }

    @GetMapping("/teacher/payouts")
    public String payoutList(@AuthenticationPrincipal AuthenticatedUser user,
                             @RequestParam(required = false) String status,
                             @RequestParam(defaultValue = "0") @Min(0) int page, Model model) {
        model.addAttribute("activeWorkspaceNav", "payouts");
        int safePage = Math.max(0, page);
        PayoutStatus selected = parseStatus(status);
        model.addAttribute("selectedStatus", selected == null ? "ALL" : selected.name());
        model.addAttribute("statusInvalid", status != null && !status.isBlank() && selected == null);
        model.addAttribute("statusOptions", List.of(new StatusOption("ALL", "Tất cả"),
                new StatusOption("PENDING", "Chờ duyệt"), new StatusOption("PROCESSING", "Đang xử lý"),
                new StatusOption("SUCCEEDED", "Đã chuyển"), new StatusOption("REJECTED", "Bị từ chối"),
                new StatusOption("FAILED", "Thất bại")));
        model.addAttribute("payoutsUnavailable", false);
        model.addAttribute("walletUnavailable", false);
        model.addAttribute("bankAccountsUnavailable", false);
        if (!model.containsAttribute("createIdempotencyKey")) {
            model.addAttribute("createIdempotencyKey", UUID.randomUUID());
        }
        WalletView wallet = null;
        List<BankAccountView> accounts = List.of();
        try {
            wallet = walletQueries.getOrCreateWallet(user.id());
            model.addAttribute("wallet", wallet);
        } catch (RuntimeException ex) {
            model.addAttribute("walletUnavailable", true);
            model.addAttribute("wallet", null);
        }
        try {
            accounts = bankAccounts.findTeacherAccounts(user.id());
            model.addAttribute("bankAccountRows", accounts);
            model.addAttribute("bankAccountsUnavailable", false);
        } catch (RuntimeException ex) {
            model.addAttribute("bankAccountsUnavailable", true);
            model.addAttribute("bankAccountRows", List.of());
        }
        try {
            Page<PayoutRequestView> result = payouts.findTeacherPayouts(user.id(), selected,
                    PageRequest.of(safePage, PAGE_SIZE, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
            if (safePage > 0 && result.getTotalElements() > 0 && result.isEmpty()) {
                return "redirect:/teacher/payouts?status=" + (selected == null ? "ALL" : selected.name()) + "&page=" + Math.max(0, result.getTotalPages() - 1);
            }
            model.addAttribute("payoutRows", result.getContent().stream().map(row -> new PayoutRow(row,
                    money(row.amountVnd()), DATE_TIME.format(row.createdAt()), payoutStatus(row.status()), safeHttpUrl(row.proofUrl()))).toList());
            model.addAttribute("currentPage", result.getNumber());
            model.addAttribute("totalPages", result.getTotalPages());
            model.addAttribute("totalElements", result.getTotalElements());
            model.addAttribute("hasPrevious", result.hasPrevious());
            model.addAttribute("hasNext", result.hasNext());
            boolean activePayout = payouts.findTeacherPayouts(user.id(), PayoutStatus.PENDING, PageRequest.of(0, 1)).hasContent()
                    || payouts.findTeacherPayouts(user.id(), PayoutStatus.PROCESSING, PageRequest.of(0, 1)).hasContent();
            model.addAttribute("activePayout", activePayout);
            model.addAttribute("canRequestPayout", wallet != null && !activePayout && !accounts.isEmpty()
                    && wallet.availableBalanceVnd() >= 1000);
            model.addAttribute("payoutsUnavailable", false);
        } catch (RuntimeException ex) {
            model.addAttribute("payoutsUnavailable", true);
            model.addAttribute("payoutRows", List.of());
            model.addAttribute("canRequestPayout", false);
            model.addAttribute("activePayout", false);
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalElements", 0L);
            model.addAttribute("hasPrevious", false);
            model.addAttribute("hasNext", false);
        }
        return "workspace/teacher-payouts";
    }

    @PostMapping("/teacher/payouts")
    public String createPayout(@AuthenticationPrincipal AuthenticatedUser user,
                               @RequestParam UUID bankAccountId, @RequestParam long amountVnd,
                               @RequestParam(required = false) String teacherNote,
                               @RequestParam long walletVersion, @RequestParam String idempotencyKey,
                               RedirectAttributes redirect) {
        try {
            if (amountVnd < 1000) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Số tiền tối thiểu là 1.000 ₫.");
            rateLimiter.checkRateLimit("create_payout", user.id().toString(), 3, 3600);
            CreatePayoutRequest request = new CreatePayoutRequest(bankAccountId, amountVnd, teacherNote, walletVersion);
            financeCommands.execute(user.id(), "TEACHER_PAYOUT_CREATE", UUID.fromString(idempotencyKey),
                    request, PayoutRequestView.class,
                    () -> ApiResponse.created(payouts.createPayout(user.id(), request)));
            redirect.addFlashAttribute("financeNotice", "Đã gửi yêu cầu rút tiền. Số dư sẽ nằm trong mục đang rút khi chờ xử lý.");
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("financeError", publicMessage(ex));
            redirect.addFlashAttribute("payoutForm", new PayoutForm(bankAccountId, amountVnd, teacherNote));
            redirect.addFlashAttribute("createIdempotencyKey", UUID.randomUUID());
        }
        return "redirect:/teacher/payouts#payout-action-message";
    }

    private PayoutStatus parseStatus(String value) {
        if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value)) return null;
        try { return PayoutStatus.valueOf(value.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { return null; }
    }

    private String payoutStatus(PayoutStatus status) {
        return switch (status) {
            case PENDING -> "Chờ duyệt";
            case PROCESSING -> "Đang xử lý";
            case SUCCEEDED -> "Đã chuyển";
            case REJECTED -> "Bị từ chối";
            case FAILED -> "Thất bại";
        };
    }

    private String money(long amount) { return NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(amount) + " ₫"; }

    private String safeHttpUrl(String value) {
        if (value == null) return null;
        String url = value.trim();
        return url.startsWith("https://") || url.startsWith("http://") ? url : null;
    }

    private String publicMessage(RuntimeException ex) {
        if (ex instanceof BusinessException business) return switch (business.getErrorCode()) {
            case CONCURRENT_MODIFICATION -> "Số dư hoặc thông tin tài khoản đã thay đổi. Tải lại trang rồi thử lại.";
            case BANK_ACCOUNT_IN_USE -> "Tài khoản đang được dùng trong yêu cầu rút tiền nên chưa thể xóa.";
            case BANK_ACCOUNT_NOT_FOUND -> "Không tìm thấy tài khoản nhận tiền của bạn.";
            case WALLET_NOT_FOUND -> "Chưa có ví để thực hiện yêu cầu.";
            case PAYOUT_INSUFFICIENT_BALANCE -> "Số tiền vượt quá số dư khả dụng.";
            case PAYOUT_ALREADY_PENDING -> "Bạn đã có yêu cầu rút tiền đang xử lý.";
            case PAYOUT_INVALID_AMOUNT, VALIDATION_ERROR -> business.getMessage() == null ? "Kiểm tra lại thông tin đã nhập." : business.getMessage();
            case RATE_LIMIT_EXCEEDED -> "Bạn đã gửi nhiều yêu cầu trong thời gian ngắn. Thử lại sau.";
            case IDEMPOTENCY_KEY_REQUIRED, IDEMPOTENCY_KEY_REUSED -> "Yêu cầu chưa được xác nhận. Tải lại trang rồi thử lại.";
            case FILE_TOO_LARGE, FILE_TYPE_NOT_ALLOWED -> "Tệp hoặc dữ liệu không hợp lệ.";
            default -> "Chưa thực hiện được thao tác. Tải lại trang rồi thử lại.";
        };
        return "Chưa thực hiện được thao tác. Tải lại trang rồi thử lại.";
    }

    public record LedgerRow(LedgerEntryView data, String amountLabel, String dateLabel, String directionLabel) {}
    public record PayoutRow(PayoutRequestView data, String amountLabel, String dateLabel, String statusLabel, String proofUrl) {}
    public record StatusOption(String value, String label) {}
    public record BankForm(String bankBin, String bankName, String accountHolderName, boolean isDefault) {}
    public record PayoutForm(UUID bankAccountId, long amountVnd, String teacherNote) {}
}
