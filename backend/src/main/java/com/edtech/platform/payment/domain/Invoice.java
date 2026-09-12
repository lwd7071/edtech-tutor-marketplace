package com.edtech.platform.payment.domain;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Entity @Table(name = "invoices") @Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE invoices SET is_deleted = true WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
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
    @Column(name="subject_id_snapshot", nullable=false) private UUID subjectIdSnapshot;
    @Column(name="package_name_snapshot", nullable=false, length=150) private String packageNameSnapshot;
    @Column(name="total_sessions_snapshot", nullable=false) private int totalSessionsSnapshot;
    @Column(name="duration_days_snapshot", nullable=false) private int durationDaysSnapshot;
    @Column(name="session_duration_minutes_snapshot", nullable=false) private int sessionDurationMinutesSnapshot;
    @Column(name="commission_rate_snapshot", nullable=false, precision=5, scale=2) private BigDecimal commissionRateSnapshot;
    @Column(name="return_url", length=500) private String returnUrl;
    @Column(name="cancel_url", length=500) private String cancelUrl;
    @Column(name="fingerprint_version", nullable=false) private short fingerprintVersion = 2;
    @Version @Column(nullable = false, columnDefinition = "bigint default 0") private long version;
    public static Invoice pending(String invoiceNumber, long orderCode, UUID studentId, UUID teacherId,
                                  UUID pricingPackageId, long amountVnd, UUID idempotencyKey,
                                  String requestFingerprint) {
        return pending(invoiceNumber, orderCode, studentId, teacherId, pricingPackageId, amountVnd,
                idempotencyKey, requestFingerprint, pricingPackageId, invoiceNumber, 1, 1, 1,
                BigDecimal.ZERO, null, null);
    }

    public static Invoice pending(String invoiceNumber, long orderCode, UUID studentId, UUID teacherId,
                                  UUID pricingPackageId, long amountVnd, UUID idempotencyKey,
                                  String requestFingerprint, UUID subjectId, String packageName,
                                  int totalSessions, int durationDays, int sessionDurationMinutes,
                                  BigDecimal commissionRate, String returnUrl, String cancelUrl) {
        if (amountVnd <= 0 || orderCode <= 0) throw new IllegalArgumentException("amount and orderCode must be positive");
        if (requestFingerprint == null || !requestFingerprint.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("requestFingerprint must be lowercase SHA-256");
        if (subjectId == null || packageName == null || packageName.isBlank() || totalSessions <= 0
                || durationDays <= 0 || sessionDurationMinutes <= 0 || commissionRate == null
                || commissionRate.signum() < 0 || commissionRate.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("invoice purchase snapshot is invalid");
        }
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
        value.subjectIdSnapshot = subjectId;
        value.packageNameSnapshot = packageName;
        value.totalSessionsSnapshot = totalSessions;
        value.durationDaysSnapshot = durationDays;
        value.sessionDurationMinutesSnapshot = sessionDurationMinutes;
        value.commissionRateSnapshot = commissionRate;
        value.returnUrl = returnUrl;
        value.cancelUrl = cancelUrl;
        value.fingerprintVersion = 2;
        return value;
    }

    public void attachPaymentLink(String linkId, String url, String qrCode, Instant expiresAt) {
        requirePending();
        Instant safeExpiresAt = expiresAt != null ? expiresAt : Instant.now().plus(java.time.Duration.ofMinutes(15));
        if (payosPaymentLinkId != null) {
            if (Objects.equals(payosPaymentLinkId, linkId) && Objects.equals(checkoutUrl, url)
                    && Objects.equals(this.qrCode, qrCode)) return;
            throw invalidState();
        }
        this.payosPaymentLinkId = Objects.requireNonNull(linkId);
        this.checkoutUrl = Objects.requireNonNull(url);
        this.qrCode = qrCode;
        this.paymentExpiredAt = safeExpiresAt;
    }
    public void markPaid(Instant paidAt) {
        if (status != InvoiceStatus.PENDING) {
            throw invalidState();
        }
        this.status = InvoiceStatus.PAID;
        this.paidAt = Objects.requireNonNull(paidAt);
    }
    public void markPaidFromVerifiedProvider(Instant paidAt) {
        if (status != InvoiceStatus.PENDING && status != InvoiceStatus.EXPIRED) throw invalidState();
        this.status = InvoiceStatus.PAID;
        this.paidAt = Objects.requireNonNull(paidAt);
    }
    public void expire() { requirePending(); this.status = InvoiceStatus.EXPIRED; }
    public void cancel() { requirePending(); this.status = InvoiceStatus.CANCELLED; }
    private void requirePending() { if (status != InvoiceStatus.PENDING) throw invalidState(); }
    private BusinessException invalidState() { return new BusinessException(ErrorCode.INVOICE_INVALID_STATE); }
}
