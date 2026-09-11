package com.edtech.platform.finance.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "finance_command_receipts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FinanceCommandReceipt {
    @Id
    private UUID id;
    @Column(nullable = false) private UUID actorId;
    @Column(nullable = false, length = 80) private String operation;
    @Column(nullable = false) private UUID idempotencyKey;
    @Column(nullable = false, length = 64) private String requestFingerprint;
    @Column(nullable = false, length = 255) private String responseType;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode responsePayload;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant expiresAt;

    public FinanceCommandReceipt(UUID actorId, String operation, UUID idempotencyKey,
                                String requestFingerprint, String responseType,
                                JsonNode responsePayload, Instant now, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.actorId = actorId;
        this.operation = operation;
        this.idempotencyKey = idempotencyKey;
        this.requestFingerprint = requestFingerprint;
        this.responseType = responseType;
        this.responsePayload = responsePayload;
        this.createdAt = now;
        this.expiresAt = expiresAt;
    }

    public void complete(JsonNode responsePayload) {
        this.responsePayload = responsePayload;
    }
}
