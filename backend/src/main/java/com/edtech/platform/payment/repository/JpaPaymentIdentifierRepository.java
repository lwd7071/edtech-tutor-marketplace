package com.edtech.platform.payment.repository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
@Repository @RequiredArgsConstructor
class JpaPaymentIdentifierRepository implements PaymentIdentifierRepository {
    private final EntityManager entityManager;
    public long nextInvoiceNumberSequence() { return ((Number) entityManager.createNativeQuery("SELECT nextval('invoice_number_seq')").getSingleResult()).longValue(); }
    public long nextPayosOrderCode() { return ((Number) entityManager.createNativeQuery("SELECT nextval('payos_order_code_seq')").getSingleResult()).longValue(); }
}
