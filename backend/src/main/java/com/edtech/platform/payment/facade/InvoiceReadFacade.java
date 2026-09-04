package com.edtech.platform.payment.facade;
import com.edtech.platform.payment.domain.Invoice;
import java.util.Optional;
import java.util.UUID;
public interface InvoiceReadFacade { Optional<Invoice> findOwned(UUID invoiceId, UUID studentId); }
