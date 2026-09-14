package com.edtech.platform.payment.facade;
import com.edtech.platform.payment.facade.dto.InvoiceSnapshot;
import java.util.Optional;
import java.util.UUID;
public interface InvoiceReadFacade { Optional<InvoiceSnapshot> findOwned(UUID invoiceId, UUID studentId); }
