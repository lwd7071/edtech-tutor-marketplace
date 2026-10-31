package com.edtech.platform.finance.web;

import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.RateLimiterService;
import com.edtech.platform.common.security.UserStatusCacheService;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(TeacherFinancePageController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeacherFinancePageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private WalletQueryService wallets;
    @MockBean private BankAccountService bankAccounts;
    @MockBean private PayoutService payouts;
    @MockBean private FinanceCommandExecutor financeCommands;
    @MockBean private RateLimiterService rateLimiter;
    @MockBean private JwtTokenProvider tokens;
    @MockBean private UserStatusCacheService statuses;
    @MockBean private SessionService sessions;
    @MockBean private Clock clock;

    private final UUID teacherId = UUID.randomUUID();
    private final UUID bankAccountId = UUID.randomUUID();

    @BeforeEach
    void authenticateTeacher() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(teacherId, "teacher@example.test", "TEACHER"), null, List.of()));
    }

    @Test
    void walletRendersBalancesAndKeepsWalletVisibleWhenLedgerFails() throws Exception {
        when(wallets.getOrCreateWallet(teacherId)).thenReturn(new WalletView(UUID.randomUUID(), teacherId,
                12_000, 80_000, 4_000, 7, 10_000));
        when(wallets.getLedgerEntries(eq(teacherId), any())).thenThrow(new IllegalStateException("private ledger error"));

        mockMvc.perform(get("/teacher/wallet"))
                .andExpect(status().isOk()).andExpect(view().name("workspace/teacher-wallet"))
                .andExpect(header().string("Cache-Control", "no-store, max-age=0"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("80.000 ₫")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Đang giữ theo lịch học")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tải được sổ cái")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private ledger error"))));
    }

    @Test
    void walletNegativePageRedirectsToFirstPage() throws Exception {
        mockMvc.perform(get("/teacher/wallet").param("page", "-1"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/teacher/wallet"));

        org.mockito.Mockito.verifyNoInteractions(wallets);
    }

    @Test
    void bankAccountsRenderOnlyMaskedAccountNumberAndNativeForms() throws Exception {
        when(bankAccounts.findTeacherAccounts(teacherId)).thenReturn(List.of(bankAccount("•••• 6789")));

        mockMvc.perform(get("/teacher/bank-accounts"))
                .andExpect(status().isOk()).andExpect(view().name("workspace/teacher-bank-accounts"))
                .andExpect(header().string("Cache-Control", "no-store, max-age=0"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("•••• 6789")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("method=\"post\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("123456789"))));
    }

    @Test
    void bankAccountCreateUsesIdempotencyExecutorAndNeverFlashesFullNumber() throws Exception {
        UUID key = UUID.randomUUID();
        BankAccountView saved = bankAccount("•••• 6789");
        doReturn(ApiResponse.created(saved)).when(financeCommands).execute(eq(teacherId), eq("TEACHER_BANK_ACCOUNT_CREATE"),
                eq(key), any(UpsertBankAccountRequest.class), eq(BankAccountView.class), any());

        mockMvc.perform(post("/teacher/bank-accounts").param("bankBin", "970436").param("bankName", "Vietcombank")
                        .param("accountNumber", "123456789").param("accountHolderName", "NGUYEN AN")
                        .param("idempotencyKey", key.toString()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/teacher/bank-accounts#bank-action-message"))
                .andExpect(flash().attribute("financeNotice", "Đã thêm tài khoản nhận tiền."))
                .andExpect(flash().attributeCount(1));
        verify(financeCommands).execute(eq(teacherId), eq("TEACHER_BANK_ACCOUNT_CREATE"), eq(key),
                any(UpsertBankAccountRequest.class), eq(BankAccountView.class), any());
    }

    @Test
    void bankAccountDeletePassesQuotedVersionToService() throws Exception {
        mockMvc.perform(post("/teacher/bank-accounts/{id}/delete", bankAccountId).param("version", "4"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/teacher/bank-accounts#bank-action-message"));
        verify(bankAccounts).deleteAccount(teacherId, bankAccountId, "\"4\"");
    }

    @Test
    void payoutPageRendersNativeFormWithCurrentWalletVersion() throws Exception {
        when(wallets.getOrCreateWallet(teacherId)).thenReturn(new WalletView(UUID.randomUUID(), teacherId,
                0, 60_000, 0, 12, 0));
        when(bankAccounts.findTeacherAccounts(teacherId)).thenReturn(List.of(bankAccount("•••• 6789")));
        when(payouts.findTeacherPayouts(eq(teacherId), any(), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/teacher/payouts"))
                .andExpect(status().isOk()).andExpect(view().name("workspace/teacher-payouts"))
                .andExpect(header().string("Cache-Control", "no-store, max-age=0"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"walletVersion\" value=\"12\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"idempotencyKey\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tối thiểu 1.000 ₫")));
        verify(payouts).findTeacherPayouts(teacherId, PayoutStatus.PENDING, PageRequest.of(0, 1));
        verify(payouts).findTeacherPayouts(teacherId, PayoutStatus.PROCESSING, PageRequest.of(0, 1));
    }

    @Test
    void payoutCommandAppliesRateLimitAndIdempotencyToServiceRequest() throws Exception {
        UUID idempotencyKey = UUID.randomUUID();
        doReturn(ApiResponse.created(new PayoutRequestView(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), bankAccountId,
                10_000, PayoutStatus.PENDING, null, null, null, null, null, null, null, 0, Instant.parse("2026-10-01T00:00:00Z"))))
                .when(financeCommands).execute(eq(teacherId), eq("TEACHER_PAYOUT_CREATE"), eq(idempotencyKey),
                        any(CreatePayoutRequest.class), eq(PayoutRequestView.class), any());

        mockMvc.perform(post("/teacher/payouts").param("bankAccountId", bankAccountId.toString())
                        .param("amountVnd", "10000").param("walletVersion", "5")
                        .param("idempotencyKey", idempotencyKey.toString()).param("teacherNote", "Tháng 10"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/teacher/payouts#payout-action-message"))
                .andExpect(flash().attribute("financeNotice", "Đã gửi yêu cầu rút tiền. Số dư sẽ nằm trong mục đang rút khi chờ xử lý."));

        verify(rateLimiter).checkRateLimit("create_payout", teacherId.toString(), 3, 3600);
        verify(financeCommands).execute(eq(teacherId), eq("TEACHER_PAYOUT_CREATE"), eq(idempotencyKey),
                any(CreatePayoutRequest.class), eq(PayoutRequestView.class), any());
    }

    @Test
    void payoutRejectsAmountsBelowApiMinimumBeforeCreatingCommand() throws Exception {
        mockMvc.perform(post("/teacher/payouts").param("bankAccountId", bankAccountId.toString())
                        .param("amountVnd", "999").param("walletVersion", "5").param("idempotencyKey", UUID.randomUUID().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("financeError", "Số tiền tối thiểu là 1.000 ₫."));
        org.mockito.Mockito.verifyNoInteractions(financeCommands, payouts, rateLimiter);
    }

    private BankAccountView bankAccount(String maskedNumber) {
        return new BankAccountView(bankAccountId, "970436", "Vietcombank", maskedNumber, "NGUYEN AN", true,
                true, Instant.parse("2026-10-01T00:00:00Z"), 3);
    }
}
