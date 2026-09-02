package com.edtech.platform.payment.repository;
import com.edtech.platform.payment.domain.PaymentTransaction;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository @RequiredArgsConstructor
class JpaPaymentTransactionRepository implements PaymentTransactionRepository {
    private final EntityManager entityManager;
    public PaymentTransaction append(PaymentTransaction value) { entityManager.persist(value); return value; }
    public boolean existsByProviderReference(String ref) { return entityManager.createQuery("select count(t)>0 from PaymentTransaction t where t.providerReference=:ref", Boolean.class).setParameter("ref", ref).getSingleResult(); }
    public Optional<PaymentTransaction> findByProviderReference(String ref) { return entityManager.createQuery("select t from PaymentTransaction t where t.providerReference=:ref", PaymentTransaction.class).setParameter("ref", ref).getResultStream().findFirst(); }
}
