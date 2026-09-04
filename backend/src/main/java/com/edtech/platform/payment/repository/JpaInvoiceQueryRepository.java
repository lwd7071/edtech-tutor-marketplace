package com.edtech.platform.payment.repository;
import com.edtech.platform.payment.domain.Invoice;
import com.edtech.platform.payment.domain.InvoiceStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository @RequiredArgsConstructor
class JpaInvoiceQueryRepository implements InvoiceQueryRepository {
    private final EntityManager entityManager;
    public Optional<Invoice> findByIdAndStudentId(UUID id, UUID studentId) { return findOwned(id, studentId); }
    public Optional<Invoice> findByStudentIdAndIdempotencyKey(UUID studentId, UUID key) {
        return entityManager.createQuery("select i from Invoice i where i.studentId=:student and i.idempotencyKey=:key", Invoice.class)
                .setParameter("student", studentId).setParameter("key", key).getResultList().stream().findFirst();
    }
    public Optional<Invoice> findByPayosOrderCode(long code) {
        return entityManager.createQuery("select i from Invoice i where i.payosOrderCode=:code", Invoice.class)
                .setParameter("code", code).getResultList().stream().findFirst();
    }
    public Page<Invoice> findByStudentId(UUID studentId, Pageable p) {
        List<Invoice> rows = entityManager.createQuery("select i from Invoice i where i.studentId=:student order by i.createdAt desc", Invoice.class)
                .setParameter("student", studentId).setFirstResult((int)p.getOffset()).setMaxResults(p.getPageSize()).getResultList();
        long count = entityManager.createQuery("select count(i) from Invoice i where i.studentId=:student", Long.class).setParameter("student", studentId).getSingleResult();
        return new PageImpl<>(rows, p, count);
    }
    public List<Invoice> findPendingExpiredBefore(Instant cutoff, Pageable p) {
        return entityManager.createQuery("select i from Invoice i where i.status=:status and i.paymentExpiredAt<:cutoff order by i.paymentExpiredAt", Invoice.class)
                .setParameter("status", InvoiceStatus.PENDING).setParameter("cutoff", cutoff)
                .setFirstResult((int)p.getOffset()).setMaxResults(p.getPageSize()).getResultList();
    }
    private Optional<Invoice> findOwned(UUID id, UUID studentId) {
        return entityManager.createQuery("select i from Invoice i where i.id=:id and i.studentId=:student", Invoice.class)
                .setParameter("id", id).setParameter("student", studentId).getResultList().stream().findFirst();
    }
}
