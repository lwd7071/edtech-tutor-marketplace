package com.edtech.platform.finance.repository;

import com.edtech.platform.finance.domain.FinanceCommandReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface FinanceCommandReceiptRepository extends JpaRepository<FinanceCommandReceipt, UUID> {
    Optional<FinanceCommandReceipt> findByActorIdAndOperationAndIdempotencyKey(UUID actorId, String operation, UUID idempotencyKey);

    @Modifying
    @Query(value = "INSERT INTO finance_command_receipts " +
            "(id, actor_id, operation, idempotency_key, request_fingerprint, response_type, response_payload, created_at, expires_at) " +
            "VALUES (:id, :actor, :operation, :key, :fingerprint, :type, CAST(:payload AS jsonb), :created, :expires) " +
            "ON CONFLICT (actor_id, operation, idempotency_key) DO NOTHING", nativeQuery = true)
    int tryClaim(@Param("id") UUID id, @Param("actor") UUID actor, @Param("operation") String operation,
                 @Param("key") UUID key, @Param("fingerprint") String fingerprint,
                 @Param("type") String type, @Param("payload") String payload,
                 @Param("created") Instant created, @Param("expires") Instant expires);

    @Modifying
    @Query("delete from FinanceCommandReceipt r where r.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
