package com.edtech.platform.payment.repository;
import com.edtech.platform.payment.domain.Invoice;
import java.util.Optional;
import java.util.UUID;
public interface InvoiceCommandRepository {
    Invoice insert(Invoice invoice);
    Optional<Invoice> findByIdForUpdate(UUID id);
    Optional<Invoice> findByPayosOrderCodeForUpdate(long orderCode);
}
