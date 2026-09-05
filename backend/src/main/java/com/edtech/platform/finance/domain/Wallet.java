package com.edtech.platform.finance.domain;

import com.edtech.platform.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.util.Objects;
import java.util.UUID;

@Entity @Table(name="wallets") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
@SQLDelete(sql="UPDATE wallets SET is_deleted = true WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
public class Wallet extends BaseEntity {
    @Column(name="teacher_id", nullable=false, unique=true) private UUID teacherId;
    @Column(name="pending_balance_vnd", nullable=false) private long pendingBalanceVnd;
    @Column(name="available_balance_vnd", nullable=false) private long availableBalanceVnd;
    @Column(name="reserved_balance_vnd", nullable=false) private long reservedBalanceVnd;
    @Version @Column(nullable=false) private long version;
    public static Wallet forTeacher(UUID teacherId) { Wallet w=new Wallet(); w.teacherId=Objects.requireNonNull(teacherId); return w; }
    public void creditPending(long amount) { pendingBalanceVnd = add(pendingBalanceVnd, amount); }
    public void debitPending(long amount) { pendingBalanceVnd = subtract(pendingBalanceVnd, amount); }
    public void creditAvailable(long amount) { availableBalanceVnd = add(availableBalanceVnd, amount); }
    public void debitAvailable(long amount) { availableBalanceVnd = subtract(availableBalanceVnd, amount); }
    public void reserveAvailable(long amount) { long available = subtract(availableBalanceVnd, amount); long reserved = add(reservedBalanceVnd, amount); availableBalanceVnd = available; reservedBalanceVnd = reserved; }
    public void releaseReserved(long amount) { long reserved = subtract(reservedBalanceVnd, amount); long available = add(availableBalanceVnd, amount); reservedBalanceVnd = reserved; availableBalanceVnd = available; }
    public void debitReserved(long amount) { reservedBalanceVnd = subtract(reservedBalanceVnd, amount); }
    private long add(long balance,long amount){ positive(amount); return Math.addExact(balance,amount); }
    private long subtract(long balance,long amount){ positive(amount); long result=Math.subtractExact(balance,amount); if(result<0) throw new IllegalStateException("wallet balance cannot be negative"); return result; }
    private void positive(long amount){ if(amount<=0) throw new IllegalArgumentException("amount must be positive"); }
}
