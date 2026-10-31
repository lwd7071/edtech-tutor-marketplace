package com.edtech.platform.finance.web;

import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.common.storage.CredentialProofStorage;
import com.edtech.platform.finance.command.CompleteTransferCommand;
import com.edtech.platform.finance.command.ProcessPayoutCommand;
import com.edtech.platform.finance.domain.PayoutStatus;
import com.edtech.platform.finance.dto.response.PayoutRequestView;
import com.edtech.platform.finance.service.FinanceCommandExecutor;
import com.edtech.platform.finance.service.FinanceProofStorage;
import com.edtech.platform.finance.service.PayoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminPayoutsPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminPayoutsPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private PayoutService payouts;
    @MockBean private FinanceProofStorage proofStorage;
    @MockBean private CredentialProofStorage proofReader;
    @MockBean private FinanceCommandExecutor commands;
    @MockBean private JwtTokenProvider tokens;
    @MockBean private UserStatusCacheService statuses;
    @MockBean private SessionService sessions;
    @MockBean private Clock clock;

    private final UUID adminId = UUID.randomUUID();
    private final UUID payoutId = UUID.randomUUID();
    private final UUID key = UUID.randomUUID();

    @BeforeEach
    void authenticateAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(adminId, "admin@example.test", "ADMIN"), null, List.of()));
    }

    @Test
    void rendersAdminQueueWithoutLeakingProviderUrlOrAccountOutsideDisclosure() throws Exception {
        PayoutRequestView payout = payout(PayoutStatus.PENDING);
        when(payouts.findAdminPayoutWorkspace(eq("PENDING"), any())).thenReturn(new PageImpl<>(List.of(
                new PayoutService.AdminPayoutWorkspaceRow(payout, "Vietcombank", "NGUYEN VAN A", "0123456789", true))));
        mockMvc.perform(get("/admin/payouts").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(view().name("workspace/admin-payouts"))
                .andExpect(header().string("Cache-Control", "no-store, max-age=0"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chi trả gia sư")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("https://secret-proof.example/file"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("0123456789")));
    }

    @Test
    void normalizesUnknownStatusAndUsesStableSortedPage() throws Exception {
        when(payouts.findAdminPayoutWorkspace(eq((String) null), any())).thenReturn(new PageImpl<>(List.of()));
        mockMvc.perform(get("/admin/payouts").param("status", "unknown").param("page", "-3"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Không có yêu cầu ở trạng thái này")));
        verify(payouts).findAdminPayoutWorkspace(eq((String) null), argThat(page -> page.getPageNumber() == 0
                && page.getPageSize() == 20 && page.getSort().getOrderFor("createdAt").getDirection() == Sort.Direction.DESC
                && page.getSort().getOrderFor("id").getDirection() == Sort.Direction.DESC));
    }

    @Test
    void rendersSafeUnavailableState() throws Exception {
        when(payouts.findAdminPayoutWorkspace(anyString(), any())).thenThrow(new IllegalStateException("private database detail"));
        mockMvc.perform(get("/admin/payouts"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tải được danh sách yêu cầu chi trả")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private database detail"))));
    }

    @Test
    @SuppressWarnings("unchecked")
    void processUsesVersionAndIdempotencyExecutor() throws Exception {
        doAnswer(invocation -> ((Supplier<ApiResponse<PayoutRequestView>>) invocation.getArgument(5)).get())
                .when(commands).execute(eq(adminId), eq("ADMIN_PAYOUT_PROCESS"), eq(key), any(),
                        eq(PayoutRequestView.class), any());
        when(payouts.processPayout(eq(adminId), eq(payoutId), eq(new ProcessPayoutCommand(7))))
                .thenReturn(payout(PayoutStatus.PROCESSING));
        mockMvc.perform(post("/admin/payouts/{id}/process", payoutId).param("version", "7")
                        .param("idempotencyKey", key.toString()).param("status", "PENDING").param("page", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/payouts?status=PENDING&page=2"));
    }

    @Test
    void invalidRejectionPreservesReasonWithoutRunningCommand() throws Exception {
        mockMvc.perform(post("/admin/payouts/{id}/reject", payoutId).param("reason", "  ")
                        .param("version", "4").param("idempotencyKey", key.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/payouts?status=ALL&page=0"));
        verify(commands, never()).execute(any(), anyString(), any(), any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void completeUploadsProofInsideIdempotentOperationAndConvertsVietnameseTime() throws Exception {
        byte[] pdf = "%PDF-1.4\nmock".getBytes();
        MockMultipartFile proof = new MockMultipartFile("proof", "receipt.pdf", "application/pdf", pdf);
        when(proofStorage.upload(any(), eq("payout"), eq(payoutId))).thenReturn(
                new com.edtech.platform.common.storage.FileStoragePort.UploadResult("finance/payout/opaque", "https://secret-proof.example/file", "application/pdf", pdf.length));
        doAnswer(invocation -> ((Supplier<ApiResponse<PayoutRequestView>>) invocation.getArgument(5)).get())
                .when(commands).execute(eq(adminId), eq("ADMIN_PAYOUT_COMPLETE"), eq(key), any(),
                        eq(PayoutRequestView.class), any());
        when(payouts.completePayout(eq(adminId), eq(payoutId), any())).thenReturn(payout(PayoutStatus.SUCCEEDED));
        mockMvc.perform(multipart("/admin/payouts/{id}/complete", payoutId).file(proof)
                        .param("bankReference", "VCB-001").param("transferredAt", "2026-10-29T10:00")
                        .param("version", "4").param("idempotencyKey", key.toString()))
                .andExpect(status().is3xxRedirection());
        verify(payouts).completePayout(eq(adminId), eq(payoutId), eq(new CompleteTransferCommand("VCB-001",
                Instant.parse("2026-10-29T03:00:00Z"), "finance/payout/opaque", "https://secret-proof.example/file", 4L)));
    }

    @Test
    void proofDownloadUsesPrivateSameOriginProxy() throws Exception {
        when(payouts.adminProofPublicId(payoutId)).thenReturn("finance/payout/opaque");
        when(proofReader.downloadLegacy("finance/payout/opaque"))
                .thenReturn(new CredentialProofStorage.DownloadedFile(new byte[]{1, 2}, "application/pdf"));
        mockMvc.perform(get("/admin/payouts/{id}/proof", payoutId))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store, max-age=0"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=payout-proof"))
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    private PayoutRequestView payout(PayoutStatus status) {
        return new PayoutRequestView(payoutId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 125_000,
                status, "Ghi chú gia sư", null, null, "https://secret-proof.example/file", null,
                adminId, Instant.parse("2026-10-28T12:00:00Z"), 4, Instant.parse("2026-10-28T12:00:00Z"));
    }
}
