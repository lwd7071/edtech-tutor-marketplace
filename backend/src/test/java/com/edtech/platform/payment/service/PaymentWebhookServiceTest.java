package com.edtech.platform.payment.service;

import com.edtech.platform.admin.facade.PlatformSettingsFacade;
import com.edtech.platform.catalog.facade.PricingPackageFacade;
import com.edtech.platform.catalog.facade.dto.PricingPackageSnapshot;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.enrollment.facade.EnrollmentFacade;
import com.edtech.platform.finance.facade.FinanceFacade;
import com.edtech.platform.finance.facade.PackageMoneyAllocator;
import com.edtech.platform.payment.domain.Invoice;
import com.edtech.platform.payment.domain.InvoiceStatus;
import com.edtech.platform.payment.domain.PaymentTransaction;
import com.edtech.platform.payment.gateway.VerifiedPayment;
import com.edtech.platform.payment.repository.InvoiceCommandRepository;
import com.edtech.platform.payment.repository.PaymentTransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookServiceTest {

    @Mock private InvoiceCommandRepository invoiceCommandRepository;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;
    @Mock private PricingPackageFacade pricingPackageFacade;
    @Mock private PlatformSettingsFacade platformSettingsFacade;
    @Mock private EnrollmentFacade enrollmentFacade;
    @Mock private FinanceFacade financeFacade;
    @Mock private ApplicationEventPublisher eventPublisher;

    private PaymentWebhookService webhookService;

    @BeforeEach
    void setUp() {
        webhookService = new PaymentWebhookServiceImpl(
                invoiceCommandRepository,
                paymentTransactionRepository,
                enrollmentFacade,
                financeFacade,
                new PackageMoneyAllocator(),
                eventPublisher
        );
    }

    @Test
    void processWebhook_shouldMarkInvoicePaid_andCallFacades() {
        long orderCode = 123456789L;
        long amount = 1000000L;
        Instant now = Instant.now();

        VerifiedPayment verifiedPayment = new VerifiedPayment(
                "REF-123456",
                orderCode,
                amount,
                now,
                new ObjectMapper().createObjectNode()
        );

        UUID studentId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();

        // 1. Mock Invoice
        Invoice invoice = spy(Invoice.pending("INV-1", orderCode, studentId, teacherId, packageId, amount,
                UUID.randomUUID(), "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                subjectId, "Advanced Java", 20, 90, 60, new BigDecimal("5.00"),
                "http://localhost:3000/payment/success", "http://localhost:3000/payment/cancel"));
        when(invoice.getId()).thenReturn(invoiceId);
        when(invoiceCommandRepository.findByPayosOrderCodeForUpdate(orderCode)).thenReturn(Optional.of(invoice));
        when(paymentTransactionRepository.existsByProviderReference("REF-123456")).thenReturn(false);

        // Act
        webhookService.processWebhook(verifiedPayment);

        // Assert Invoice & Transaction
        verify(invoice).markPaid(now);
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        verify(paymentTransactionRepository).append(any(PaymentTransaction.class));

        // Assert Enrollment Facade called with exact domain values
        verify(enrollmentFacade).activateStudentPackage(
                eq(studentId), eq(teacherId), eq(subjectId), eq(packageId), eq(invoiceId),
                eq("Advanced Java"), eq(20), eq(90), eq(amount), eq(new BigDecimal("5.00")), eq(now)
        );

        // Assert Finance Facade called with netAmount = 950,000 (1,000,000 - 5%)
        verify(financeFacade).creditTeacherPendingBalance(
                eq(teacherId), eq(950000L), eq(invoiceId), eq("INV-1")
        );
        verify(pricingPackageFacade, never()).getPurchasablePackage(any());
    }

    @Test
    void processWebhook_shouldIgnore_whenInvoiceAlreadyPaid() {
        long orderCode = 123L;
        VerifiedPayment payment = new VerifiedPayment("REF-123", orderCode, 500000L, Instant.now(), new ObjectMapper().createObjectNode());

        Invoice invoice = mock(Invoice.class);
        UUID invoiceId = UUID.randomUUID();
        when(invoice.getId()).thenReturn(invoiceId);
        when(invoice.getStatus()).thenReturn(InvoiceStatus.PAID);
        when(invoiceCommandRepository.findByPayosOrderCodeForUpdate(orderCode)).thenReturn(Optional.of(invoice));
        PaymentTransaction existing = mock(PaymentTransaction.class);
        when(existing.getInvoiceId()).thenReturn(invoiceId);
        when(paymentTransactionRepository.findByProviderReference("REF-123")).thenReturn(Optional.of(existing));

        webhookService.processWebhook(payment);

        verify(invoice, never()).markPaid(any());
        verify(enrollmentFacade, never()).activateStudentPackage(any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), anyLong(), any(), any());
        verify(financeFacade, never()).creditTeacherPendingBalance(any(), anyLong(), any(), any());
    }

    @Test
    void processWebhook_shouldThrow_whenAmountMismatches() {
        long orderCode = 123L;
        VerifiedPayment payment = new VerifiedPayment("REF-123", orderCode, 999999L, Instant.now(), new ObjectMapper().createObjectNode());

        Invoice invoice = Invoice.pending("INV-1", orderCode, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 500000L, UUID.randomUUID(), "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        when(invoiceCommandRepository.findByPayosOrderCodeForUpdate(orderCode)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> webhookService.processWebhook(payment))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH));
    }

    @Test
    void processWebhook_shouldActivatePackage_withoutWalletEntry_whenCommissionIsOneHundredPercent() {
        long orderCode = 456L;
        long amount = 1L;
        Instant paidAt = Instant.now();
        UUID studentId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = spy(Invoice.pending(
                "INV-ZERO-NET", orderCode, studentId, teacherId, packageId, amount,
                UUID.randomUUID(), "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                subjectId, "Package", 10, 30, 60, new BigDecimal("100"),
                "http://localhost:3000/payment/success", "http://localhost:3000/payment/cancel"));
        when(invoice.getId()).thenReturn(invoiceId);
        when(invoiceCommandRepository.findByPayosOrderCodeForUpdate(orderCode)).thenReturn(Optional.of(invoice));
        webhookService.processWebhook(new VerifiedPayment(
                "REF-ZERO-NET", orderCode, amount, paidAt, new ObjectMapper().createObjectNode()));

        verify(enrollmentFacade).activateStudentPackage(
                eq(studentId), eq(teacherId), eq(subjectId), eq(packageId), eq(invoiceId),
                eq("Package"), eq(10), eq(30), eq(amount), eq(new BigDecimal("100")), eq(paidAt));
        verifyNoInteractions(financeFacade);
    }
}
