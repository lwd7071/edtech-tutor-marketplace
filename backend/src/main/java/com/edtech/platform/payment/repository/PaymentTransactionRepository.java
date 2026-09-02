package com.edtech.platform.payment.repository;
import com.edtech.platform.payment.domain.PaymentTransaction;
import java.util.Optional;
public interface PaymentTransactionRepository {
    PaymentTransaction append(PaymentTransaction transaction);
    boolean existsByProviderReference(String providerReference);
    Optional<PaymentTransaction> findByProviderReference(String providerReference);
}
