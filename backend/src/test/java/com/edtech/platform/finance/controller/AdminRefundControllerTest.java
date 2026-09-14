package com.edtech.platform.finance.controller;

import com.edtech.platform.finance.dto.request.ApproveRefundRequest;
import com.edtech.platform.finance.dto.request.CompleteTransferMetadata;
import com.edtech.platform.admin.dto.request.RejectRequest;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.finance.domain.RefundStatus;
import com.edtech.platform.finance.dto.response.RefundRequestView;
import com.edtech.platform.finance.service.RefundService;
import com.edtech.platform.finance.service.FinanceProofStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminRefundControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Mock private RefundService refundService;
    @Mock private FinanceProofStorage proofStorage;
    @InjectMocks private AdminRefundController controller;

    private final UUID adminId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return new AuthenticatedUser(adminId, "admin@example.com", "ADMIN");
                    }
                })
                .build();
    }

    @Test
    void listAdminRefunds_shouldReturn200Paged() throws Exception {
        RefundRequestView view = new RefundRequestView(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Reason", 5, 0, null,
                RefundStatus.PENDING, null, "VCB", "970436", "******6789", "NGUYEN VAN A",
                null, null, null, null, 0L, Instant.now()
        );
        when(refundService.findAdminRefunds(nullable(String.class), any()))
                .thenReturn(new PageImpl<>(List.of(view)));

        mockMvc.perform(get("/api/admin/refund-requests")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.meta.totalElements").value(1));
    }

    @Test
    void approveRefund_shouldReturn200() throws Exception {
        UUID refundId = UUID.randomUUID();
        ApproveRefundRequest req = new ApproveRefundRequest(3, "Duyet 3 buoi", 0L);

        RefundRequestView view = new RefundRequestView(
                refundId, UUID.randomUUID(), UUID.randomUUID(), "Reason", 5, 3, 300000L,
                RefundStatus.APPROVED, "Duyet 3 buoi", "VCB", "970436", "******6789", "NGUYEN VAN A",
                null, null, adminId, Instant.now(), 1L, Instant.now()
        );
        when(refundService.approveRefund(eq(adminId), eq(refundId), any())).thenReturn(view);

        mockMvc.perform(post("/api/admin/refund-requests/" + refundId + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.approvedSessions").value(3))
                .andExpect(jsonPath("$.data.refundAmountVnd").value(300000));
    }

    @Test
    void completeRefund_shouldReturn200WithMultipartProof() throws Exception {
        UUID refundId = UUID.randomUUID();
        CompleteTransferMetadata req = new CompleteTransferMetadata("VCB-REF", Instant.now(), 1L);
        MockMultipartFile metadata = new MockMultipartFile("metadata", "metadata.json", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(req));
        MockMultipartFile proof = new MockMultipartFile("proof", "proof.pdf", "application/pdf", "%PDF-test".getBytes());
        when(proofStorage.upload(any(), eq("refund"), eq(refundId)))
                .thenReturn(new com.edtech.platform.common.storage.FileStoragePort.UploadResult("proof", "https://proof", "application/pdf", 9));
        RefundRequestView view = new RefundRequestView(
                refundId, UUID.randomUUID(), UUID.randomUUID(), "Reason", 5, 3, 300000L,
                RefundStatus.REFUNDED, null, "VCB", "970436", "******6789", "NGUYEN VAN A",
                "VCB-REF", "https://proof", adminId, Instant.now(), 2L, Instant.now());
        when(refundService.completeRefund(eq(adminId), eq(refundId), any())).thenReturn(view);

        mockMvc.perform(multipart("/api/admin/refund-requests/" + refundId + "/complete")
                        .file(metadata).file(proof))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("REFUNDED"));
    }
}
