package com.edtech.platform.payment.controller;

import com.edtech.platform.payment.domain.Invoice;
import com.edtech.platform.payment.domain.InvoiceStatus;
import com.edtech.platform.payment.dto.InvoiceCreationRequest;
import com.edtech.platform.payment.service.InvoiceService;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
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
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void createInvoice_shouldReturn201_whenValid() throws Exception {
        UUID idempotencyKey = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        InvoiceCreationRequest request = new InvoiceCreationRequest(studentId, packageId, null, null);

        Invoice mockInvoice = mock(Invoice.class);
        when(mockInvoice.getInvoiceNumber()).thenReturn("INV-123");
        when(mockInvoice.getStatus()).thenReturn(InvoiceStatus.PENDING);
        when(mockInvoice.getCheckoutUrl()).thenReturn("https://payos.vn/pay/123");
        when(mockInvoice.getAmountVnd()).thenReturn(500000L);

        when(invoiceService.createInvoiceAndPaymentLink(eq(studentId), eq(packageId), eq(idempotencyKey), any(), any()))
                .thenReturn(mockInvoice);

        mockMvc.perform(post("/api/student/invoices")
                .header("Idempotency-Key", idempotencyKey.toString())
                .principal(() -> "student")
                .requestAttr("org.springframework.security.core.annotation.AuthenticationPrincipal", new AuthenticatedUser(studentId, "student@test", "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-123"))
                .andExpect(jsonPath("$.checkoutUrl").value("https://payos.vn/pay/123"));
    }
}
