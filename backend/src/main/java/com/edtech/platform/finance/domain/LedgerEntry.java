package com.edtech.platform.finance.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity @Table(name="ledger_entries") @Immutable @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class LedgerEntry {
    @Id private UUID id;
    @Column(name="wallet_id",nullable=false) private UUID walletId;
    @Enumerated(EnumType.STRING) @Column(name="entry_type",nullable=false,length=30) private LedgerEntryType entryType;
    @Column(name="amount_vnd",nullable=false) private long amountVnd;
    @Enumerated(EnumType.STRING) @Column(name="balance_bucket",nullable=false,length=20) private BalanceBucket balanceBucket;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=10) private LedgerDirection direction;
    @Column(name="reference_type",nullable=false,length=30) private String referenceType;
    @Column(name="reference_id",nullable=false) private UUID referenceId;
    @Column(name="idempotency_key",nullable=false,unique=true) private String idempotencyKey;
    @Column(columnDefinition="text") private String description;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    public LedgerEntry(UUID walletId,LedgerEntryType entryType,long amountVnd,BalanceBucket bucket,
                       LedgerDirection direction,String referenceType,UUID referenceId,String idempotencyKey,String description){
        if(amountVnd<=0) throw new IllegalArgumentException("ledger amount must be positive");
        this.walletId=Objects.requireNonNull(walletId); this.entryType=Objects.requireNonNull(entryType);
        this.amountVnd=amountVnd; this.balanceBucket=Objects.requireNonNull(bucket); this.direction=Objects.requireNonNull(direction);
        this.referenceType=requireText(referenceType,"referenceType"); this.referenceId=Objects.requireNonNull(referenceId);
        this.idempotencyKey=requireText(idempotencyKey,"idempotencyKey"); this.description=description;
    }
    private String requireText(String value,String name){if(value==null||value.isBlank())throw new IllegalArgumentException(name+" is required");return value;}
    @PrePersist void prePersist(){if(id==null)id=UUID.randomUUID();if(createdAt==null)createdAt=Instant.now();}
}
