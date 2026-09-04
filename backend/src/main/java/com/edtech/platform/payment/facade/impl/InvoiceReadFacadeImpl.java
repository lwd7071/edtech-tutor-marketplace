package com.edtech.platform.payment.facade.impl;
import com.edtech.platform.payment.domain.Invoice;
import com.edtech.platform.payment.facade.InvoiceReadFacade;
import com.edtech.platform.payment.repository.InvoiceQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.UUID;
@Service @RequiredArgsConstructor
public class InvoiceReadFacadeImpl implements InvoiceReadFacade {
 private final InvoiceQueryRepository repository;
 public Optional<Invoice> findOwned(UUID invoiceId, UUID studentId) { return repository.findByIdAndStudentId(invoiceId, studentId); }
}
