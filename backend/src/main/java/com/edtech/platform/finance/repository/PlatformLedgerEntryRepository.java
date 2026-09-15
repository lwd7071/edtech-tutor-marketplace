package com.edtech.platform.finance.repository;
import com.edtech.platform.finance.domain.PlatformLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;
import java.util.Optional;
public interface PlatformLedgerEntryRepository extends JpaRepository<PlatformLedgerEntry,UUID> {
    boolean existsByIdempotencyKey(String key);
    Optional<PlatformLedgerEntry> findByIdempotencyKey(String key);

    @Query(value = """
            select coalesce(sum(case when p.direction = 'CREDIT' then p.amount_vnd else -p.amount_vnd end), 0)::bigint
            from platform_ledger_entries p
            join bookings b on b.id = p.booking_id
            where b.teacher_id = :teacherId and p.bucket = 'ESCROW'
            """, nativeQuery = true)
    long heldBalanceForTeacher(@Param("teacherId") UUID teacherId);
}
