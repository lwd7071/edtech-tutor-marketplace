package com.edtech.platform.payment.domain;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity @Table(name = "invoices") @Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE invoices SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class Invoice extends BaseEntity {
    @Column(name="invoice_number", nullable=false, length=50, unique=true) private String invoiceNumber;
    @Column(name="student_id", nullable=false) private UUID studentId;
    @Column(name="teacher_id", nullable=false) private UUID teacherId;
    @Column(name="pricing_package_id", nullable=false) private UUID pricingPackageId;
    @Column(name="amount_vnd", nullable=false) private long amountVnd;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private InvoiceStatus status;
    @Column(name="payos_order_code", nullable=false, unique=true) private long payosOrderCode;
    @Column(name="payos_payment_link_id", unique=true) private String payosPaymentLinkId;
    @Column(name="checkout_url", length=500) private String checkoutUrl;
    @Column(name="qr_code", columnDefinition="text") private String qrCode;
    @Column(name="payment_expired_at") private Instant paymentExpiredAt;
    @Column(name="paid_at") private Instant paidAt;
    @Column(name="idempotency_key", nullable=false) private UUID idempotencyKey;
    @Column(name="request_fingerprint", nullable=false, length=64) private String requestFingerprint;

    public static Invoice pending(String invoiceNumber, long orderCode, UUID studentId, UUID teacherId,
                                  UUID pricingPackageId, long amountVnd, UUID idempotencyKey,
                                  String requestFingerprint) {
        if (amountVnd <= 0 || orderCode <= 0) throw new IllegalArgumentException("amount and orderCode must be positive");
        if (requestFingerprint == null || !requestFingerprint.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("requestFingerprint must be lowercase SHA-256");
        Invoice value = new Invoice();
        value.invoiceNumber = Objects.requireNonNull(invoiceNumber);
        value.payosOrderCode = orderCode;
        value.studentId = Objects.requireNonNull(studentId);
        value.teacherId = Objects.requireNonNull(teacherId);
        value.pricingPackageId = Objects.requireNonNull(pricingPackageId);
        value.amountVnd = amountVnd;
        value.idempotencyKey = Objects.requireNonNull(idempotencyKey);
        value.requestFingerprint = requestFingerprint;
        value.status = InvoiceStatus.PENDING;
        return value;
    }

    public void attachPaymentLink(String linkId, String url, String qrCode, Instant expiresAt) {
        requirePending();
        if (payosPaymentLinkId != null) {
            if (Objects.equals(payosPaymentLinkId, linkId) && Objects.equals(checkoutUrl, url)
                    && Objects.equals(this.qrCode, qrCode) && Objects.equals(paymentExpiredAt, expiresAt)) return;
            throw invalidState();
        }
        this.payosPaymentLinkId = Objects.requireNonNull(linkId);
        this.checkoutUrl = Objects.requireNonNull(url);
        this.qrCode = qrCode;
        this.paymentExpiredAt = Objects.requireNonNull(expiresAt);
    }
    public void markPaid(Instant paidAt) { requirePending(); this.status = InvoiceStatus.PAID; this.paidAt = Objects.requireNonNull(paidAt); }
    public void expire() { requirePending(); this.status = InvoiceStatus.EXPIRED; }
    public void cancel() { requirePending(); this.status = InvoiceStatus.CANCELLED; }
    private void requirePending() { if (status != InvoiceStatus.PENDING) throw invalidState(); }
    private BusinessException invalidState() { return new BusinessException(ErrorCode.INVOICE_INVALID_STATE); }
}
