package com.edtech.platform.payment.repository;
import com.edtech.platform.payment.domain.Invoice;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;
@Repository @RequiredArgsConstructor
class JpaInvoiceCommandRepository implements InvoiceCommandRepository {
    private final EntityManager entityManager;
    public Invoice insert(Invoice invoice) { entityManager.persist(invoice); return invoice; }
    public Optional<Invoice> findByIdForUpdate(UUID id) {
        return Optional.ofNullable(entityManager.find(Invoice.class, id, LockModeType.PESSIMISTIC_WRITE));
    }
    public Optional<Invoice> findByPayosOrderCodeForUpdate(long orderCode) {
        return entityManager.createQuery("select i from Invoice i where i.payosOrderCode=:code", Invoice.class)
                .setParameter("code", orderCode).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultStream().findFirst();
    }
}
