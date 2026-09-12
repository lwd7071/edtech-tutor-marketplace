package com.edtech.platform.payment.controller;

import com.edtech.platform.payment.domain.Invoice;
import com.edtech.platform.payment.domain.InvoiceStatus;
import com.edtech.platform.payment.dto.CreateInvoiceRequest;
import com.edtech.platform.payment.dto.InvoiceDetail;
import com.edtech.platform.payment.service.InvoiceService;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;

import java.util.UUID;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StudentInvoiceControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private com.edtech.platform.common.security.RateLimiterService rateLimiterService;

    @InjectMocks
    private StudentInvoiceController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createInvoice_shouldReturn201_whenValid() throws Exception {
        UUID idempotencyKey = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        CreateInvoiceRequest request = new CreateInvoiceRequest(packageId, null, null);

        Invoice mockInvoice = mock(Invoice.class);
        when(mockInvoice.getInvoiceNumber()).thenReturn("INV-123");
        when(mockInvoice.getId()).thenReturn(UUID.randomUUID());
        when(mockInvoice.getPricingPackageId()).thenReturn(packageId);
        when(mockInvoice.getStatus()).thenReturn(InvoiceStatus.PENDING);
        when(mockInvoice.getCheckoutUrl()).thenReturn("https://payos.vn/pay/123");
        when(mockInvoice.getQrCode()).thenReturn("qr");
        when(mockInvoice.getPaymentExpiredAt()).thenReturn(Instant.parse("2026-09-12T01:00:00Z"));
        when(mockInvoice.getPaidAt()).thenReturn(null);
        when(mockInvoice.getAmountVnd()).thenReturn(500000L);

        InvoiceDetail mockInvoiceDetail = InvoiceDetail.from(mockInvoice);
        when(invoiceService.createInvoiceAndPaymentLink(
                any(UUID.class), any(UUID.class), any(UUID.class), any(), any()))
                .thenReturn(mockInvoiceDetail);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new AuthenticatedUser(studentId, "student@test", "STUDENT"), null));

        mockMvc.perform(post("/api/student/invoices")
                .header("Idempotency-Key", idempotencyKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.*").value(org.hamcrest.Matchers.hasSize(5)))
                .andExpect(jsonPath("$.data.invoiceNumber").value("INV-123"))
                .andExpect(jsonPath("$.data.pricingPackageId").value(packageId.toString()))
                .andExpect(jsonPath("$.data.checkoutUrl").value("https://payos.vn/pay/123"));
    }
}
