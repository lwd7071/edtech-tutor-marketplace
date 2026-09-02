package com.edtech.platform.architecture.paymentfixtures;
import com.edtech.platform.payment.domain.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface BadCrudInvoiceRepository extends JpaRepository<Invoice, UUID> { }
