package com.edtech.platform.payment.repository;
import com.edtech.platform.payment.domain.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface InvoiceQueryRepository {
    Optional<Invoice> findByIdAndStudentId(UUID id, UUID studentId);
    Optional<Invoice> findByStudentIdAndIdempotencyKey(UUID studentId, UUID idempotencyKey);
    Optional<Invoice> findByPayosOrderCode(long orderCode);
    Page<Invoice> findByStudentId(UUID studentId, Pageable pageable);
    List<Invoice> findPendingExpiredBefore(Instant cutoff, Pageable pageable);
}
