package com.edtech.platform.finance.repository;
import com.edtech.platform.finance.domain.LedgerEntry;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
@Repository @RequiredArgsConstructor
class JpaLedgerEntryRepository implements LedgerEntryRepository {
    private final EntityManager entityManager;
    public LedgerEntry append(LedgerEntry entry){entityManager.persist(entry);return entry;}
    public boolean existsByIdempotencyKey(String key){return entityManager.createQuery("select count(e)>0 from LedgerEntry e where e.idempotencyKey=:key",Boolean.class).setParameter("key",key).getSingleResult();}
    public Page<LedgerEntry> findByWalletId(UUID walletId, Pageable p){
        List<LedgerEntry> rows=entityManager.createQuery("select e from LedgerEntry e where e.walletId=:id order by e.createdAt desc",LedgerEntry.class).setParameter("id",walletId).setFirstResult((int)p.getOffset()).setMaxResults(p.getPageSize()).getResultList();
        long count=entityManager.createQuery("select count(e) from LedgerEntry e where e.walletId=:id",Long.class).setParameter("id",walletId).getSingleResult();
        return new PageImpl<>(rows,p,count);
    }
}
