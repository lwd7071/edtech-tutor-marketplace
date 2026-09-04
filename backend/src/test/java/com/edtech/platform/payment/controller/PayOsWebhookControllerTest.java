package com.edtech.platform.payment.controller;

import com.edtech.platform.payment.gateway.InvalidPaymentSignatureException;
import com.edtech.platform.payment.gateway.PaymentGateway;
import com.edtech.platform.payment.gateway.VerifiedPayment;
import com.edtech.platform.payment.service.PaymentWebhookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PayOsWebhookControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private PaymentWebhookService paymentWebhookService;

    @Mock
    private com.edtech.platform.common.security.RateLimiterService rateLimiterService;

    @InjectMocks
    private PayOsWebhookController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void handleWebhook_shouldReturn200_whenValid() throws Exception {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("code", "00");

        VerifiedPayment verified = new VerifiedPayment("REF-1", 1001L, 500000L, Instant.now(), requestBody);
        when(paymentGateway.verifyWebhook(any())).thenReturn(verified);
        doNothing().when(paymentWebhookService).processWebhook(verified);

        mockMvc.perform(post("/api/webhooks/payos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void handleWebhook_shouldReturn400_whenInvalidSignature() throws Exception {
        ObjectNode requestBody = objectMapper.createObjectNode();
        when(paymentGateway.verifyWebhook(any())).thenThrow(new InvalidPaymentSignatureException("Bad signature"));

        mockMvc.perform(post("/api/webhooks/payos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
