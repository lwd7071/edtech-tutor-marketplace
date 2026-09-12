package com.edtech.platform.finance.controller;

import com.edtech.platform.admin.dto.request.CompleteTransferRequest;
import com.edtech.platform.admin.dto.request.ProcessPayoutRequest;
import com.edtech.platform.admin.dto.request.RejectRequest;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.finance.domain.PayoutStatus;
import com.edtech.platform.finance.dto.response.PayoutRequestView;
import com.edtech.platform.finance.service.PayoutService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminPayoutControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Mock private PayoutService payoutService;
    @InjectMocks private AdminPayoutController controller;

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
    void listAdminPayouts_shouldReturn200Paged() throws Exception {
        PayoutRequestView view = new PayoutRequestView(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                500000L, PayoutStatus.PENDING, "Rut tien", null, null, null, null, null, null, 0L, Instant.now()
        );
        when(payoutService.findAdminPayouts(nullable(String.class), any()))
                .thenReturn(new PageImpl<>(List.of(view)));

        mockMvc.perform(get("/api/admin/payout-requests")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].amountVnd").value(500000))
                .andExpect(jsonPath("$.meta.totalElements").value(1));
    }

    @Test
    void processPayout_shouldReturn200() throws Exception {
        UUID payoutId = UUID.randomUUID();
        ProcessPayoutRequest req = new ProcessPayoutRequest(0L);

        PayoutRequestView view = new PayoutRequestView(
                payoutId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                500000L, PayoutStatus.PROCESSING, "Rut tien", null, null, null, null, adminId, Instant.now(), 1L, Instant.now()
        );
        when(payoutService.processPayout(eq(adminId), eq(payoutId), any())).thenReturn(view);

        mockMvc.perform(post("/api/admin/payout-requests/" + payoutId + "/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PROCESSING"));
    }

    @Test
    void completePayout_shouldReturn200() throws Exception {
        UUID payoutId = UUID.randomUUID();
        CompleteTransferRequest req = new CompleteTransferRequest("VCB123", Instant.now(), "proof", "https://proof", 1L);

        PayoutRequestView view = new PayoutRequestView(
                payoutId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                500000L, PayoutStatus.SUCCEEDED, "Rut tien", null, "VCB123", "https://proof", Instant.now(), adminId, Instant.now(), 2L, Instant.now()
        );
        when(payoutService.completePayout(eq(adminId), eq(payoutId), any())).thenReturn(view);

        mockMvc.perform(post("/api/admin/payout-requests/" + payoutId + "/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"));
    }
}