package com.edtech.platform.payment.job;

import com.edtech.platform.payment.domain.Invoice;
import com.edtech.platform.payment.domain.InvoiceStatus;
import com.edtech.platform.payment.repository.InvoiceCommandRepository;
import com.edtech.platform.payment.repository.InvoiceQueryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InvoiceExpiryJobTest {
 @Test void expiresOnlyLockedPendingExpiredInvoice() {
  InvoiceQueryRepository query = mock(InvoiceQueryRepository.class);
  InvoiceCommandRepository command = mock(InvoiceCommandRepository.class);
  TransactionTemplate tx = mock(TransactionTemplate.class);
  Invoice invoice = mock(Invoice.class);
  UUID id = UUID.randomUUID();
  when(invoice.getId()).thenReturn(id); when(invoice.getStatus()).thenReturn(InvoiceStatus.PENDING);
  when(invoice.getPaymentExpiredAt()).thenReturn(Instant.now().minusSeconds(60));
  when(query.findPendingExpiredBefore(any(), any(PageRequest.class))).thenReturn(List.of(invoice));
  when(command.findByIdForUpdate(id)).thenReturn(Optional.of(invoice));
  doAnswer(inv -> { ((java.util.function.Consumer<org.springframework.transaction.TransactionStatus>) inv.getArgument(0)).accept(null); return null; }).when(tx).executeWithoutResult(any());
  new InvoiceExpiryJob(query, command, tx).expirePendingInvoices();
  verify(invoice).expire();
 }
}
