package com.edtech.platform.payment.service;

import com.edtech.platform.admin.facade.PlatformSettingsFacade;
import com.edtech.platform.catalog.facade.PricingPackageFacade;
import com.edtech.platform.catalog.facade.dto.PricingPackageSnapshot;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.enrollment.facade.EnrollmentFacade;
import com.edtech.platform.finance.facade.FinanceFacade;
import com.edtech.platform.payment.domain.Invoice;
import com.edtech.platform.payment.domain.InvoiceStatus;
import com.edtech.platform.payment.domain.PaymentTransaction;
import com.edtech.platform.payment.gateway.VerifiedPayment;
import com.edtech.platform.payment.repository.InvoiceCommandRepository;
import com.edtech.platform.payment.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentWebhookServiceImpl implements PaymentWebhookService {

    private final InvoiceCommandRepository invoiceCommandRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PricingPackageFacade pricingPackageFacade;
    private final PlatformSettingsFacade platformSettingsFacade;
    private final EnrollmentFacade enrollmentFacade;
    private final FinanceFacade financeFacade;

    @Override
    @Transactional
    public void processWebhook(VerifiedPayment payment) {
        Objects.requireNonNull(payment, "payment is required");
        if (payment.providerReference() == null || payment.providerReference().isBlank()
                || payment.orderCode() <= 0 || payment.amountVnd() <= 0 || payment.paidAt() == null) {
            throw new BusinessException(ErrorCode.PAYMENT_PROVIDER_ERROR, "Verified payment identity is invalid");
        }
        log.info("Processing webhook for orderCode: {}, ref: {}", payment.orderCode(), payment.providerReference());

        // 1. Pessimistic Lock on Invoice
        Invoice invoice = invoiceCommandRepository.findByPayosOrderCodeForUpdate(payment.orderCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVOICE_NOT_FOUND));

        // 2. Idempotency Check: Already PAID or Transaction already processed
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            paymentTransactionRepository.findByProviderReference(payment.providerReference())
                    .filter(existing -> invoice.getId().equals(existing.getInvoiceId()))
                    .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED));
            log.info("Invoice {} is already marked PAID. Skipping duplicate processing.", invoice.getInvoiceNumber());
            return;
        }

        if (paymentTransactionRepository.existsByProviderReference(payment.providerReference())) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED,
                    "Provider reference is already associated with another payment");
        }

        // 3. Amount verification
        if (payment.amountVnd() != invoice.getAmountVnd()) {
            log.error("Amount mismatch for orderCode {}: expected {}, received {}",
                    payment.orderCode(), invoice.getAmountVnd(), payment.amountVnd());
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        // 4. Mark Invoice PAID
        if (invoice.getStatus() == com.edtech.platform.payment.domain.InvoiceStatus.EXPIRED) {
            invoice.markPaidFromVerifiedProvider(payment.paidAt());
        } else {
            invoice.markPaid(payment.paidAt());
        }

        // 5. Append PaymentTransaction
        PaymentTransaction tx = new PaymentTransaction(
                invoice.getId(),
                "PAYOS",
                payment.providerReference(),
                payment.orderCode(),
                payment.amountVnd(),
                payment.paidAt(),
                true,
                payment.sanitizedPayload()
        );
        paymentTransactionRepository.append(tx);

        // 6. Fetch Pricing Package Snapshot
        PricingPackageSnapshot pkg = pricingPackageFacade.getPurchasablePackage(invoice.getPricingPackageId());
        if (pkg == null) {
            log.error("Pricing package {} not found for invoice {}", invoice.getPricingPackageId(), invoice.getInvoiceNumber());
            throw new BusinessException(ErrorCode.PRICING_PACKAGE_NOT_FOUND);
        }

        UUID subjectId = pkg.subjectId();
        if (subjectId == null) {
            throw new BusinessException(ErrorCode.SUBJECT_NOT_FOUND, "Package does not have associated subject");
        }

        // 7. Get Commission Rate
        BigDecimal commissionRate = platformSettingsFacade.getCommissionRate();
        if (commissionRate == null) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Commission rate is not configured");
        }

        // 8. Create StudentPackage via EnrollmentFacade
        enrollmentFacade.activateStudentPackage(
                invoice.getStudentId(),
                invoice.getTeacherId(),
                subjectId,
                pkg.id(),
                invoice.getId(),
                pkg.name(),
                pkg.totalSessions(),
                pkg.durationDays(),
                invoice.getAmountVnd(),
                commissionRate,
                payment.paidAt()
        );

        // 9. Calculate net amount for teacher
        long amount = invoice.getAmountVnd();
        BigDecimal feeRate = commissionRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        long fee = new BigDecimal(amount).multiply(feeRate).setScale(0, RoundingMode.HALF_UP).longValue();
        long netAmount = amount - fee;

        // 10. Update Teacher Wallet via FinanceFacade
        financeFacade.creditTeacherPendingBalance(
                invoice.getTeacherId(),
                netAmount,
                invoice.getId(),
                invoice.getInvoiceNumber()
        );

        log.info("Successfully processed payment webhook for invoice {}", invoice.getInvoiceNumber());
    }
}
