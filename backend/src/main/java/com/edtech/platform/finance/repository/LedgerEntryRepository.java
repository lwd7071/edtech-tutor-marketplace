package com.edtech.platform.finance.repository;
import com.edtech.platform.finance.domain.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
public interface LedgerEntryRepository {
    LedgerEntry append(LedgerEntry entry);
    boolean existsByIdempotencyKey(String key);
    Page<LedgerEntry> findByWalletId(UUID walletId, Pageable pageable);
}
