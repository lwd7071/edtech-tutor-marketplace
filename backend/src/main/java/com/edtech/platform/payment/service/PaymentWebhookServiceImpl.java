package com.edtech.platform.payment.service;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentWebhookServiceImpl implements PaymentWebhookService {

    private final InvoiceCommandRepository invoiceCommandRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final EnrollmentFacade enrollmentFacade;
    private final FinanceFacade financeFacade;
    private final PackageMoneyAllocator packageMoneyAllocator;
    private final org.springframework.context.ApplicationEventPublisher events;

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

        UUID subjectId = invoice.getSubjectIdSnapshot();
        if (subjectId == null) {
            throw new BusinessException(ErrorCode.SUBJECT_NOT_FOUND, "Package does not have associated subject");
        }

        // 7. Get Commission Rate
        BigDecimal commissionRate = invoice.getCommissionRateSnapshot();
        if (commissionRate == null) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Commission rate is not configured");
        }

        // 8. Create StudentPackage via EnrollmentFacade
        enrollmentFacade.activateStudentPackage(
                invoice.getStudentId(),
                invoice.getTeacherId(),
                subjectId,
                invoice.getPricingPackageId(),
                invoice.getId(),
                invoice.getPackageNameSnapshot(),
                invoice.getTotalSessionsSnapshot(),
                invoice.getDurationDaysSnapshot(),
                invoice.getAmountVnd(),
                commissionRate,
                payment.paidAt()
        );

        // 9. Calculate net amount for teacher
        long netAmount = packageMoneyAllocator.teacherNetTotal(invoice.getAmountVnd(), commissionRate);

        // 10. Update Teacher Wallet via FinanceFacade
        if (netAmount > 0) {
            financeFacade.creditTeacherPendingBalance(
                    invoice.getTeacherId(),
                    netAmount,
                    invoice.getId(),
                    invoice.getInvoiceNumber()
            );
        }

        events.publishEvent(new com.edtech.platform.common.event.payment.PaymentSucceededEvent(
                invoice.getStudentId(), invoice.getTeacherId(), invoice.getId(), invoice.getPricingPackageId(),
                invoice.getPackageNameSnapshot(), invoice.getAmountVnd()));

        log.info("Successfully processed payment webhook for invoice {}", invoice.getInvoiceNumber());
    }
}
