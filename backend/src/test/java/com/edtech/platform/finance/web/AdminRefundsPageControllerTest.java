package com.edtech.platform.finance.web;

import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.common.storage.CredentialProofStorage;
import com.edtech.platform.finance.command.ApproveRefundCommand;
import com.edtech.platform.finance.command.CompleteTransferCommand;
import com.edtech.platform.finance.command.RejectFinanceCommand;
import com.edtech.platform.finance.domain.RefundStatus;
import com.edtech.platform.finance.dto.response.RefundRequestView;
import com.edtech.platform.finance.service.FinanceCommandExecutor;
import com.edtech.platform.finance.service.FinanceProofStorage;
import com.edtech.platform.finance.service.RefundService;
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

@WebMvcTest(AdminRefundsPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminRefundsPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private RefundService refunds;
    @MockBean private FinanceProofStorage proofStorage;
    @MockBean private CredentialProofStorage proofReader;
    @MockBean private FinanceCommandExecutor commands;
    @MockBean private JwtTokenProvider tokens;
    @MockBean private UserStatusCacheService statuses;
    @MockBean private SessionService sessions;
    @MockBean private Clock clock;

    private final UUID adminId = UUID.randomUUID();
    private final UUID refundId = UUID.randomUUID();
    private final UUID key = UUID.randomUUID();

    @BeforeEach
    void authenticateAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(adminId, "admin@example.test", "ADMIN"), null, List.of()));
    }

    @Test
    void rendersRefundQueueAndServerOnlyProofStateWithoutPublicUrl() throws Exception {
        when(refunds.findAdminRefunds(eq("PENDING"), any())).thenReturn(new PageImpl<>(List.of(refund())));
        mockMvc.perform(get("/admin/refunds").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(view().name("workspace/admin-refunds"))
                .andExpect(header().string("Cache-Control", "no-store, max-age=0"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Đối soát hoàn tiền")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tải chứng từ qua hệ thống")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret-proof.example"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script"))));
    }

    @Test
    void normalizesUnknownFilterAndUsesStableVietnameseSortedPage() throws Exception {
        when(refunds.findAdminRefunds(eq((String) null), any())).thenReturn(new PageImpl<>(List.of()));
        mockMvc.perform(get("/admin/refunds").param("status", "surprise").param("page", "-2"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Không có yêu cầu ở trạng thái này")));
        verify(refunds).findAdminRefunds(eq((String) null), argThat(page -> page.getPageNumber() == 0
                && page.getPageSize() == 20 && page.getSort().getOrderFor("createdAt").getDirection() == Sort.Direction.DESC
                && page.getSort().getOrderFor("id").getDirection() == Sort.Direction.DESC));
    }

    @Test
    void returnsSafeErrorStateWhenFinanceQueryFails() throws Exception {
        when(refunds.findAdminRefunds(anyString(), any())).thenThrow(new IllegalStateException("database-password-not-for-ui"));
        mockMvc.perform(get("/admin/refunds"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tải được danh sách yêu cầu hoàn tiền")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("database-password-not-for-ui"))));
    }

    @Test
    @SuppressWarnings("unchecked")
    void approveUsesCurrentVersionAndIdempotencyExecutor() throws Exception {
        RefundRequestView response = refund();
        String longAdminNote = "Đã kiểm tra. ".repeat(100);
        doAnswer(invocation -> ((Supplier<ApiResponse<RefundRequestView>>) invocation.getArgument(5)).get())
                .when(commands).execute(eq(adminId), eq("ADMIN_REFUND_APPROVE"), eq(key), any(),
                        eq(RefundRequestView.class), any());
        when(refunds.approveRefund(eq(adminId), eq(refundId), any())).thenReturn(response);
        mockMvc.perform(post("/admin/refunds/{id}/approve", refundId)
                        .param("approvedSessions", "2").param("adminNote", " " + longAdminNote + " ")
                        .param("version", "4").param("idempotencyKey", key.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/refunds?status=ALL&page=0"));
        verify(refunds).approveRefund(eq(adminId), eq(refundId), eq(new ApproveRefundCommand(2, longAdminNote.trim(), 4)));
    }

    @Test
    void validationFailureKeepsRejectReasonAndReopensForm() throws Exception {
        when(refunds.findAdminRefunds(eq((String) null), any())).thenReturn(new PageImpl<>(List.of(refund())));
        mockMvc.perform(post("/admin/refunds/{id}/reject", refundId)
                        .param("reason", "   ").param("version", "4").param("idempotencyKey", key.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/refunds?status=ALL&page=0"));
        verify(commands, org.mockito.Mockito.never()).execute(any(), anyString(), any(), any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void completeUploadsProofOnlyInsideIdempotentCommand() throws Exception {
        byte[] pdf = "%PDF-1.4\nmock".getBytes();
        MockMultipartFile proof = new MockMultipartFile("proof", "receipt.pdf", "application/pdf", pdf);
        when(proofStorage.upload(any(), eq("refund"), eq(refundId)))
                .thenReturn(new com.edtech.platform.common.storage.FileStoragePort.UploadResult("finance/refund/receipt", "https://secret-proof.example/file", "application/pdf", pdf.length));
        doAnswer(invocation -> ((Supplier<ApiResponse<RefundRequestView>>) invocation.getArgument(5)).get())
                .when(commands).execute(eq(adminId), eq("ADMIN_REFUND_COMPLETE"), eq(key), any(),
                        eq(RefundRequestView.class), any());
        when(refunds.completeRefund(eq(adminId), eq(refundId), any())).thenReturn(refund());
        mockMvc.perform(multipart("/admin/refunds/{id}/complete", refundId)
                        .file(proof).param("bankReference", "VCB-001").param("transferredAt", "2026-10-29T10:00")
                        .param("version", "4").param("idempotencyKey", key.toString()))
                .andExpect(status().is3xxRedirection());
        verify(refunds).completeRefund(eq(adminId), eq(refundId), eq(new CompleteTransferCommand("VCB-001",
                Instant.parse("2026-10-29T03:00:00Z"), "finance/refund/receipt", "https://secret-proof.example/file", 4L)));
    }

    @Test
    void proofDownloadUsesSameOriginNoStoreProxy() throws Exception {
        when(refunds.adminProofPublicId(refundId)).thenReturn("finance/refund/opaque");
        when(proofReader.downloadLegacy("finance/refund/opaque"))
                .thenReturn(new CredentialProofStorage.DownloadedFile(new byte[]{1, 2}, "application/pdf"));
        mockMvc.perform(get("/admin/refunds/{id}/proof", refundId))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store, max-age=0"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=refund-proof"))
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    private RefundRequestView refund() {
        return new RefundRequestView(refundId, UUID.randomUUID(), UUID.randomUUID(), "Không thể tiếp tục học",
                2, 0, null, RefundStatus.PENDING, null, "Vietcombank", "970436", "•••• 1234",
                "NGUYEN VAN A", null, "https://secret-proof.example/file", adminId,
                Instant.parse("2026-10-28T12:00:00Z"), 4, Instant.parse("2026-10-28T12:00:00Z"));
    }
}
