package com.edtech.platform.payment.web;

import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.payment.facade.InvoiceReadFacade;
import com.edtech.platform.payment.facade.dto.InvoiceSnapshot;
import com.edtech.platform.payment.service.support.QrCodeSvgService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(StudentPaymentPageController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@TestPropertySource(properties = "app.payment.provider=fake")
class StudentPaymentPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private InvoiceReadFacade invoices;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private UserStatusCacheService userStatusCacheService;
    @MockBean private SessionService sessionService;
    @MockBean private Clock clock;
    @MockBean private QrCodeSvgService qrCodeSvgService;

    private final UUID studentId = UUID.randomUUID();
    private final UUID invoiceId = UUID.randomUUID();

    @BeforeEach
    void authenticateStudent() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(studentId, "student@example.com", "STUDENT"), null, List.of()));
        when(qrCodeSvgService.generateSvgDataUri(anyString(), anyInt()))
                .thenReturn("data:image/svg+xml;base64,PHN2Zy8+");
    }

    @Test
    void rendersPendingCheckoutAndUsesOwnedInvoiceSnapshot() throws Exception {
        when(invoices.findOwned(eq(invoiceId), eq(studentId))).thenReturn(Optional.of(invoice("PENDING")));

        mockMvc.perform(get("/student/checkout/{id}", invoiceId).param("status", "PAID"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/checkout"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mở trang thanh toán</a>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Kiểm tra trạng thái hóa đơn")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("student/payment-result/")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data:image/svg+xml;base64,PHN2Zy8+")));
    }

    @Test
    void providerReturnRedirectsToOwnedInvoiceResultWithoutUsingProviderStatus() throws Exception {
        mockMvc.perform(get("/student/payments/callback").param("invoiceId", invoiceId.toString()).param("status", "PAID"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/payment-result/" + invoiceId));
    }

    @Test
    void providerReturnWithMalformedInvoiceIdFallsBackToPackages() throws Exception {
        mockMvc.perform(get("/student/payments/callback").param("invoiceId", "https://evil.example"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/packages"));
    }

    @Test
    void paidCheckoutRedirectsToServerVerifiedResult() throws Exception {
        when(invoices.findOwned(eq(invoiceId), eq(studentId))).thenReturn(Optional.of(invoice("PAID")));

        mockMvc.perform(get("/student/checkout/{id}", invoiceId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/payment-result/" + invoiceId));
    }

    @Test
    void checkoutDoesNotRenderNonPayOsOrInsecureUrls() throws Exception {
        when(invoices.findOwned(eq(invoiceId), eq(studentId))).thenReturn(Optional.of(invoice("PENDING", "http://evil.example/pay")));

        mockMvc.perform(get("/student/checkout/{id}", invoiceId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("evil.example"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Hóa đơn chưa có liên kết thanh toán")));
    }

    @Test
    void testProfileCanRenderTheReservedFakeProviderHost() throws Exception {
        when(invoices.findOwned(eq(invoiceId), eq(studentId))).thenReturn(Optional.of(invoice("PENDING", "https://pay.test/checkout/42")));

        mockMvc.perform(get("/student/checkout/{id}", invoiceId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("https://pay.test/checkout/42")));
    }

    @Test
    void missingOrNotOwnedInvoiceIsNotFound() throws Exception {
        when(invoices.findOwned(eq(invoiceId), eq(studentId))).thenReturn(Optional.empty());

        mockMvc.perform(get("/student/payment-result/{id}", invoiceId))
                .andExpect(status().isNotFound());
    }

    @Test
    void paymentResultShowsSuccessOnlyFromOwnedPaidSnapshot() throws Exception {
        when(invoices.findOwned(eq(invoiceId), eq(studentId))).thenReturn(Optional.of(invoice("PAID")));

        mockMvc.perform(get("/student/payment-result/{id}", invoiceId).param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/payment-result"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Thanh toán thành công")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Xem gói học đã mua")));
    }

    @Test
    void readFailureRendersRetryStateWithoutClaimingSuccess() throws Exception {
        when(invoices.findOwned(eq(invoiceId), eq(studentId))).thenThrow(new IllegalStateException("read failed"));

        mockMvc.perform(get("/student/payment-result/{id}", invoiceId))
                .andExpect(status().isOk())
                .andExpect(view().name("student/payment-result"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model().attribute("paymentUnavailable", true))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa xác minh được kết quả")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Thanh toán thành công"))));
    }

    private InvoiceSnapshot invoice(String status) {
        return invoice(status, "https://pay.payos.vn/web/123");
    }

    private InvoiceSnapshot invoice(String status, String checkoutUrl) {
        return new InvoiceSnapshot(invoiceId, "TM-2026-123", UUID.randomUUID(), 450_000, status,
                checkoutUrl, "bank-qr-payload", Instant.parse("2026-10-28T12:00:00Z"),
                "PAID".equals(status) ? Instant.parse("2026-10-28T11:00:00Z") : null);
    }
}
