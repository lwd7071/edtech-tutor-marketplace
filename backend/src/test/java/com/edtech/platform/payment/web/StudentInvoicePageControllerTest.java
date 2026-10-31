package com.edtech.platform.payment.web;

import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.RateLimiterService;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.payment.dto.InvoiceDetail;
import com.edtech.platform.payment.service.InvoiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentInvoicePageController.class)
@AutoConfigureMockMvc(addFilters = false)
class StudentInvoicePageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private InvoiceService invoices;
    @MockBean private RateLimiterService rateLimiter;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private UserStatusCacheService userStatusCacheService;
    @MockBean private SessionService sessionService;
    @MockBean private Clock clock;

    private final UUID studentId = UUID.randomUUID();
    private final UUID packageId = UUID.randomUUID();
    private final UUID idempotencyKey = UUID.randomUUID();
    private final UUID invoiceId = UUID.randomUUID();

    @BeforeEach
    void authenticateStudent() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(studentId, "student@example.com", "STUDENT"), null, List.of()));
    }

    @Test
    void createsInvoiceThroughApplicationServiceAndRedirectsToOwnedCheckout() throws Exception {
        when(invoices.createInvoiceAndPaymentLink(studentId, packageId, idempotencyKey))
                .thenReturn(new InvoiceDetail(invoiceId, "INV-TEST", packageId, 100_000,
                        "PENDING", "https://pay.test/checkout/42", "TEST-QR", null, null));

        mockMvc.perform(post("/student/invoices")
                        .param("pricingPackageId", packageId.toString())
                        .param("idempotencyKey", idempotencyKey.toString())
                        .param("returnTo", "/teachers/" + UUID.randomUUID()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/checkout/" + invoiceId));

        verify(rateLimiter).checkRateLimit("create_invoice", studentId.toString(), 10, 60);
    }

    @Test
    void failedPurchaseRedirectsToValidatedTeacherProfileWithGenericNotice() throws Exception {
        UUID teacherId = UUID.randomUUID();
        when(invoices.createInvoiceAndPaymentLink(eq(studentId), eq(packageId), eq(idempotencyKey)))
                .thenThrow(new BusinessException(ErrorCode.PRICING_PACKAGE_NOT_FOUND));

        mockMvc.perform(post("/student/invoices")
                        .param("pricingPackageId", packageId.toString())
                        .param("idempotencyKey", idempotencyKey.toString())
                        .param("returnTo", "/teachers/" + teacherId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teachers/" + teacherId + "#packages"))
                .andExpect(flash().attribute("purchaseError", org.hamcrest.Matchers.containsString("Chưa tạo được hóa đơn")));
    }

    @Test
    void rejectsExternalReturnTargetAndFallsBackToMarketplace() throws Exception {
        when(invoices.createInvoiceAndPaymentLink(any(UUID.class), any(UUID.class), any(UUID.class)))
                .thenThrow(new IllegalStateException("gateway unavailable"));

        mockMvc.perform(post("/student/invoices")
                        .param("pricingPackageId", packageId.toString())
                        .param("idempotencyKey", idempotencyKey.toString())
                        .param("returnTo", "//evil.example/collect"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teachers#packages"));
    }
}
