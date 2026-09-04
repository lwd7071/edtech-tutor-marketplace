package com.edtech.platform.payment.service;

import com.edtech.platform.catalog.facade.PricingPackageFacade;
import com.edtech.platform.catalog.facade.dto.PricingPackageSnapshot;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.payment.config.PaymentProviderProperties;
import com.edtech.platform.payment.domain.Invoice;
import com.edtech.platform.payment.domain.InvoiceStatus;
import com.edtech.platform.payment.gateway.*;
import com.edtech.platform.payment.repository.InvoiceCommandRepository;
import com.edtech.platform.payment.repository.InvoiceQueryRepository;
import com.edtech.platform.payment.repository.PaymentIdentifierRepository;
import com.edtech.platform.payment.service.support.InvoiceNumberFactory;
import com.edtech.platform.payment.service.support.InvoiceRequestFingerprint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URI;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceCommandRepository invoiceCommandRepository;
    private final InvoiceQueryRepository invoiceQueryRepository;
    private final PaymentGateway paymentGateway;
    private final PricingPackageFacade pricingPackageFacade;
    private final PaymentIdentifierRepository paymentIdentifierRepository;
    private final PaymentProviderProperties paymentProperties;
    private final TransactionTemplate transactionTemplate;

    private final InvoiceRequestFingerprint fingerprintHelper = new InvoiceRequestFingerprint();
    private final InvoiceNumberFactory invoiceNumberFactory = new InvoiceNumberFactory(Clock.systemUTC());

    @Override
    public Invoice createInvoiceAndPaymentLink(UUID studentId, UUID pricingPackageId, UUID idempotencyKey) {
        String defaultReturn = paymentProperties.getDefaultReturnUrl() != null ? paymentProperties.getDefaultReturnUrl() : "http://localhost:3000/payment/success";
        String defaultCancel = paymentProperties.getDefaultCancelUrl() != null ? paymentProperties.getDefaultCancelUrl() : "http://localhost:3000/payment/cancel";
        return createInvoiceAndPaymentLink(studentId, pricingPackageId, idempotencyKey, defaultReturn, defaultCancel);
    }

    public Invoice createInvoiceAndPaymentLink(UUID studentId, UUID pricingPackageId, UUID idempotencyKey, String returnUrl, String cancelUrl) {
        Objects.requireNonNull(studentId, "studentId is required");
        Objects.requireNonNull(pricingPackageId, "pricingPackageId is required");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey is required");
        returnUrl = returnUrl == null || returnUrl.isBlank() ? paymentProperties.getDefaultReturnUrl() : returnUrl;
        cancelUrl = cancelUrl == null || cancelUrl.isBlank() ? paymentProperties.getDefaultCancelUrl() : cancelUrl;
        if (returnUrl == null || cancelUrl == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Payment return/cancel URL is not configured");
        }

        // 1. Fetch package snapshot
        PricingPackageSnapshot pkg = pricingPackageFacade.getPurchasablePackage(pricingPackageId);
        if (pkg == null) {
            throw new BusinessException(ErrorCode.PRICING_PACKAGE_NOT_FOUND);
        }
        if (pkg.priceVnd() <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Package price must be positive");
        }

        // 2. Build canonical SHA-256 fingerprint
        String requestFingerprint = fingerprintHelper.sha256(
                studentId,
                pricingPackageId,
                pkg.priceVnd(),
                returnUrl,
                cancelUrl
        );

        // 3. Check idempotency
        Optional<Invoice> existingOpt = invoiceQueryRepository.findByStudentIdAndIdempotencyKey(studentId, idempotencyKey);
        if (existingOpt.isPresent()) {
            Invoice existing = existingOpt.get();
            if (!existing.getRequestFingerprint().equals(requestFingerprint)) {
                throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REUSED);
            }
            if (existing.getStatus() == InvoiceStatus.PAID) {
                return existing;
            }
            if (existing.getCheckoutUrl() != null) {
                return existing;
            }
        }

        // 4. Phase 1: Local DB Transaction 1 (Prepare & Insert Pending Invoice)
        Invoice pendingInvoice = null;
        if (existingOpt.isPresent() && existingOpt.get().getCheckoutUrl() == null) {
            pendingInvoice = existingOpt.get();
        } else {
            try {
                pendingInvoice = transactionTemplate.execute(status -> {
                    long orderCode = paymentIdentifierRepository.nextPayosOrderCode();
                    long seq = paymentIdentifierRepository.nextInvoiceNumberSequence();
                    String invoiceNumber = invoiceNumberFactory.format(seq);

                    Invoice invoice = Invoice.pending(
                            invoiceNumber,
                            orderCode,
                            studentId,
                            pkg.teacherId(),
                            pricingPackageId,
                            pkg.priceVnd(),
                            idempotencyKey,
                            requestFingerprint
                    );
                    return invoiceCommandRepository.insert(invoice);
                });
            } catch (DataIntegrityViolationException dive) {
                // Concurrent race condition handling
                Optional<Invoice> raceOpt = invoiceQueryRepository.findByStudentIdAndIdempotencyKey(studentId, idempotencyKey);
                if (raceOpt.isPresent()) {
                    Invoice raceInvoice = raceOpt.get();
                    if (!raceInvoice.getRequestFingerprint().equals(requestFingerprint)) {
                        throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REUSED);
                    }
                    if (raceInvoice.getCheckoutUrl() != null) {
                        return raceInvoice;
                    }
                    pendingInvoice = raceInvoice;
                } else {
                    throw dive;
                }
            }
        }

        // 5. Phase 2: Gateway Network Call (OUTSIDE DB Transaction)
        PaymentLinkResult linkResult = null;
        try {
            // Reconcile first before creating new link
            Optional<PaymentLinkStatus> existingGatewayLink = paymentGateway.findPaymentLink(pendingInvoice.getPayosOrderCode());
            if (existingGatewayLink.isPresent() && existingGatewayLink.get().checkoutUrl() != null) {
                PaymentLinkStatus st = existingGatewayLink.get();
                linkResult = new PaymentLinkResult(
                        st.orderCode(),
                        st.paymentLinkId(),
                        st.checkoutUrl(),
                        st.qrCode(),
                        st.expiresAt()
                );
            } else {
                PaymentLinkCommand command = new PaymentLinkCommand(
                        pendingInvoice.getPayosOrderCode(),
                        pendingInvoice.getAmountVnd(),
                        "Thanh toan " + pendingInvoice.getInvoiceNumber(),
                        URI.create(returnUrl),
                        URI.create(cancelUrl)
                );
                linkResult = paymentGateway.createPaymentLink(command);
            }
        } catch (PaymentGatewayUnavailableException e) {
            log.error("Payment gateway unavailable for orderCode={}", pendingInvoice.getPayosOrderCode(), e);
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, e.getMessage());
        } catch (PaymentGatewayRejectedException e) {
            log.error("Payment gateway rejected orderCode={}", pendingInvoice.getPayosOrderCode(), e);
            throw new BusinessException(ErrorCode.PAYMENT_PROVIDER_ERROR, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to create payment link for orderCode={}", pendingInvoice.getPayosOrderCode(), e);
            throw new BusinessException(ErrorCode.PAYMENT_LINK_CREATION_FAILED, e.getMessage());
        }

        // 6. Phase 3: Local DB Transaction 2 (Attach Payment Link Info)
        final UUID invoiceId = pendingInvoice.getId();
        final PaymentLinkResult finalLinkResult = linkResult;

        return transactionTemplate.execute(status -> {
            Invoice inv = invoiceCommandRepository.findByIdForUpdate(invoiceId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVOICE_NOT_FOUND));

            inv.attachPaymentLink(
                    finalLinkResult.paymentLinkId(),
                    finalLinkResult.checkoutUrl(),
                    finalLinkResult.qrCode(),
                    finalLinkResult.expiresAt()
            );
            return inv;
        });
    }
}
