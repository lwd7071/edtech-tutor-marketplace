package com.edtech.platform.payment.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity @Table(name = "payment_transactions") @Immutable @Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentTransaction {
    @Id private UUID id;
    @Column(name="invoice_id", nullable=false) private UUID invoiceId;
    @Column(nullable=false, length=50) private String provider;
    @Column(name="provider_reference", nullable=false, unique=true) private String providerReference;
    @Column(name="order_code", nullable=false) private long orderCode;
    @Column(name="amount_vnd", nullable=false) private long amountVnd;
    @Column(name="transaction_datetime", nullable=false) private Instant transactionDatetime;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name="raw_payload", columnDefinition="jsonb") private JsonNode rawPayload;
    @Column(name="signature_valid", nullable=false) private boolean signatureValid;
    @Column(name="processed_at") private Instant processedAt;
    @Column(name="created_at", nullable=false, updatable=false) private Instant createdAt;

    public PaymentTransaction(UUID invoiceId, String provider, String providerReference, long orderCode,
                              long amountVnd, Instant transactionDatetime, boolean signatureValid,
                              JsonNode sanitizedPayload) {
        if (orderCode <= 0 || amountVnd <= 0) throw new IllegalArgumentException("payment values must be positive");
        this.invoiceId = Objects.requireNonNull(invoiceId);
        this.provider = Objects.requireNonNull(provider);
        this.providerReference = Objects.requireNonNull(providerReference);
        this.orderCode = orderCode;
        this.amountVnd = amountVnd;
        this.transactionDatetime = Objects.requireNonNull(transactionDatetime);
        this.signatureValid = signatureValid;
        this.rawPayload = sanitizedPayload;
    }
    @PrePersist void prePersist() { if (id == null) id = UUID.randomUUID(); if (createdAt == null) createdAt = Instant.now(); }
}
