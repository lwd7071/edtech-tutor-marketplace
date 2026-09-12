package com.edtech.platform.payment.service;

import com.edtech.platform.catalog.facade.PricingPackageFacade;
import com.edtech.platform.admin.facade.PlatformSettingsFacade;
import com.edtech.platform.catalog.facade.dto.PricingPackageSnapshot;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.payment.config.PaymentProviderProperties;
import com.edtech.platform.payment.domain.Invoice;
import com.edtech.platform.payment.dto.InvoiceDetail;
import com.edtech.platform.payment.gateway.PaymentGateway;
import com.edtech.platform.payment.gateway.PaymentLinkCommand;
import com.edtech.platform.payment.gateway.PaymentLinkResult;
import com.edtech.platform.payment.repository.InvoiceCommandRepository;
import com.edtech.platform.payment.repository.InvoiceQueryRepository;
import com.edtech.platform.payment.repository.PaymentIdentifierRepository;
import com.edtech.platform.payment.service.support.InvoiceRequestFingerprint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvoiceServiceTest {

    @Mock private InvoiceCommandRepository invoiceCommandRepository;
    @Mock private InvoiceQueryRepository invoiceQueryRepository;
    @Mock private PaymentGateway paymentGateway;
    @Mock private PricingPackageFacade pricingPackageFacade;
    @Mock private PaymentIdentifierRepository paymentIdentifierRepository;
    @Mock private PlatformSettingsFacade platformSettingsFacade;
    @Mock private TransactionTemplate transactionTemplate;

    private PaymentProviderProperties properties;
    private InvoiceService invoiceService;

    @BeforeEach
    void setUp() {
        properties = new PaymentProviderProperties();
        properties.setDefaultReturnUrl("http://localhost:3000/payment/success");
        properties.setDefaultCancelUrl("http://localhost:3000/payment/cancel");

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });

        invoiceService = new InvoiceServiceImpl(
                invoiceCommandRepository,
                invoiceQueryRepository,
                paymentGateway,
                pricingPackageFacade,
                paymentIdentifierRepository,
                properties,
                platformSettingsFacade,
                transactionTemplate
        );
        when(platformSettingsFacade.getCommissionRate()).thenReturn(new BigDecimal("5.00"));
    }

    @Test
    void createInvoice_shouldExecute3PhaseFlow_andReturnInvoiceWithPaymentLink() {
        UUID studentId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();

        PricingPackageSnapshot pkgSnapshot = new PricingPackageSnapshot(
                packageId, teacherId, subjectId, "Math 101", 10, 30, 500000L, 60, "ACTIVE"
        );
        when(pricingPackageFacade.getPurchasablePackage(packageId)).thenReturn(pkgSnapshot);
        when(invoiceQueryRepository.findByStudentIdAndIdempotencyKey(studentId, idempotencyKey)).thenReturn(Optional.empty());
        when(paymentIdentifierRepository.nextPayosOrderCode()).thenReturn(1001L);
        when(paymentIdentifierRepository.nextInvoiceNumberSequence()).thenReturn(1L);

        when(invoiceCommandRepository.insert(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        PaymentLinkResult linkResult = new PaymentLinkResult(1001L, "payos-link-id", "https://checkout.payos.vn/1001", "qr-data", Instant.now().plusSeconds(900));
        when(paymentGateway.findPaymentLink(1001L)).thenReturn(Optional.empty());
        when(paymentGateway.createPaymentLink(any(PaymentLinkCommand.class))).thenReturn(linkResult);

        Invoice createdPending = Invoice.pending("INV-20260904-1", 1001L, studentId, teacherId, packageId, 500000L, idempotencyKey,
                new InvoiceRequestFingerprint().sha256(studentId, packageId, 500000L, properties.getDefaultReturnUrl(), properties.getDefaultCancelUrl()));
        when(invoiceCommandRepository.findByIdForUpdate(any())).thenReturn(Optional.of(createdPending));

        InvoiceDetail result = invoiceService.createInvoiceAndPaymentLink(studentId, packageId, idempotencyKey);

        verify(invoiceCommandRepository).insert(any(Invoice.class));
        verify(paymentGateway).createPaymentLink(any(PaymentLinkCommand.class));
        verify(invoiceCommandRepository).findByIdForUpdate(any());

        assertThat(result.checkoutUrl()).isEqualTo("https://checkout.payos.vn/1001");
    }

    @Test
    void createInvoice_shouldReplayExisting_whenIdempotencyKeyReusedWithSamePayload() {
        UUID studentId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();

        PricingPackageSnapshot pkgSnapshot = new PricingPackageSnapshot(
                packageId, teacherId, subjectId, "Math 101", 10, 30, 500000L, 60, "ACTIVE"
        );
        when(pricingPackageFacade.getPurchasablePackage(packageId)).thenReturn(pkgSnapshot);

        String fingerprint = new InvoiceRequestFingerprint().sha256(studentId, packageId, 500000L, properties.getDefaultReturnUrl(), properties.getDefaultCancelUrl());
        Invoice existingInvoice = Invoice.pending("INV-20260904-1", 1001L, studentId, teacherId, packageId, 500000L, idempotencyKey, fingerprint);
        existingInvoice.attachPaymentLink("link-1", "https://checkout.payos.vn/1001", "qr", Instant.now().plusSeconds(900));

        when(invoiceQueryRepository.findByStudentIdAndIdempotencyKey(studentId, idempotencyKey)).thenReturn(Optional.of(existingInvoice));

        InvoiceDetail result = invoiceService.createInvoiceAndPaymentLink(studentId, packageId, idempotencyKey);

        verify(paymentGateway, never()).createPaymentLink(any());
        verify(invoiceCommandRepository, never()).insert(any());
        assertThat(result.checkoutUrl()).isEqualTo("https://checkout.payos.vn/1001");
    }

    @Test
    void createInvoice_shouldThrowConflict_whenIdempotencyKeyReusedWithDifferentPayload() {
        UUID studentId = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();

        PricingPackageSnapshot pkgSnapshot = new PricingPackageSnapshot(
                packageId, UUID.randomUUID(), UUID.randomUUID(), "Math 101", 10, 30, 500000L, 60, "ACTIVE"
        );
        when(pricingPackageFacade.getPurchasablePackage(packageId)).thenReturn(pkgSnapshot);

        Invoice existingWithDifferentFingerprint = Invoice.pending("INV-OLD", 999L, studentId, UUID.randomUUID(), packageId, 500000L, idempotencyKey, "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
        when(invoiceQueryRepository.findByStudentIdAndIdempotencyKey(studentId, idempotencyKey)).thenReturn(Optional.of(existingWithDifferentFingerprint));

        assertThatThrownBy(() -> invoiceService.createInvoiceAndPaymentLink(studentId, packageId, idempotencyKey))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REUSED));
    }
}
