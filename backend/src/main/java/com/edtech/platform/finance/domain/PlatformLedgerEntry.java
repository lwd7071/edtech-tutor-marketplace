package com.edtech.platform.finance.domain;
import com.edtech.platform.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
@Entity @Table(name="platform_ledger_entries") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class PlatformLedgerEntry extends BaseEntity {
 @Column(name="booking_id",nullable=false) private UUID bookingId;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private PlatformLedgerBucket bucket;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private LedgerDirection direction;
 @Column(name="amount_vnd",nullable=false) private long amountVnd;
 @Column(name="idempotency_key",nullable=false,unique=true) private String idempotencyKey;
 private String description;
 public PlatformLedgerEntry(UUID bookingId,PlatformLedgerBucket bucket,LedgerDirection direction,long amount,String key,String description){this.bookingId=bookingId;this.bucket=bucket;this.direction=direction;this.amountVnd=amount;this.idempotencyKey=key;this.description=description;}
}
