package com.edtech.platform.integration;

import com.edtech.platform.booking.domain.DeliveryMode;
import com.edtech.platform.booking.dto.request.CreateBookingRequest;
import com.edtech.platform.booking.dto.response.BookingDetail;
import com.edtech.platform.booking.service.BookingService;
import com.edtech.platform.common.AbstractIntegrationTest;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.payment.domain.Invoice;
import com.edtech.platform.payment.service.InvoiceService;
import com.edtech.platform.payment.gateway.PaymentGateway;
import com.edtech.platform.payment.gateway.PaymentLinkCommand;
import com.edtech.platform.payment.gateway.PaymentLinkResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@DisplayName("Task 9.4 - Concurrency & Performance Benchmarks Integration Test")
class ConcurrentStressIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private PaymentGateway paymentGateway;

    @BeforeEach
    void setUp() {
        when(paymentGateway.findPaymentLink(anyLong())).thenReturn(Optional.empty());
        when(paymentGateway.createPaymentLink(any(PaymentLinkCommand.class))).thenAnswer(inv -> {
            PaymentLinkCommand cmd = inv.getArgument(0);
            return new PaymentLinkResult(
                    cmd.orderCode(),
                    "link-" + cmd.orderCode(),
                    "https://checkout.payos.vn/" + cmd.orderCode(),
                    "qr-data",
                    Instant.now().plusSeconds(900)
            );
        });
    }

    @Test
    @DisplayName("Concurrency: 20 concurrent threads booking the exact same slot -> Exactly 1 winner")
    void concurrentBookingOverlap_shouldAllowOnlyOneWinner() throws Exception {
        UUID teacherUserId = UUID.fromString("b0000000-0000-0000-0000-000000000001");
        UUID studentPackageId = UUID.fromString("40000000-0000-0000-0000-000000000001");

        // Fixed slot in future (2026-11-20T14:00:00Z to 2026-11-20T15:00:00Z)
        Instant startTime = Instant.parse("2026-11-20T14:00:00Z");
        Instant endTime = Instant.parse("2026-11-20T15:00:00Z");

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
        List<BookingDetail> successfulBookings = Collections.synchronizedList(new ArrayList<>());

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    CreateBookingRequest request = new CreateBookingRequest(
                            studentPackageId,
                            startTime,
                            endTime,
                            DeliveryMode.ONLINE,
                            "https://meet.google.com/test-concurrency",
                            null
                    );
                    BookingDetail detail = bookingService.create(teacherUserId, request);
                    successCount.incrementAndGet();
                    successfulBookings.add(detail);
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    failures.add(e);
                }
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        for (Future<?> f : futures) {
            f.get(15, TimeUnit.SECONDS);
        }
        executor.shutdown();

        // Verify exactly 1 succeeded and 19 failed
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(19);
        assertThat(failures).hasSize(19);
        assertThat(failures).allMatch(this::isBookingConflict);

        // Verify DB contains only 1 booking for this slot
        Integer countInDb = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM bookings WHERE start_time = ?::timestamptz AND end_time = ?::timestamptz AND status = 'SCHEDULED' AND is_deleted = false",
                Integer.class, startTime.toString(), endTime.toString()
        );
        assertThat(countInDb).isEqualTo(1);
    }

    @Test
    @DisplayName("Concurrency: 20 concurrent threads creating invoice with same Idempotency-Key -> Exactly 1 invoice")
    void concurrentInvoiceCreation_sameIdempotencyKey_shouldReturnSameInvoice() throws Exception {
        UUID studentId = UUID.fromString("10000000-0000-0000-0000-000000000001");
        UUID pricingPackageId = UUID.fromString("f0000000-0000-0000-0000-000000000001");
        UUID idempotencyKey = UUID.randomUUID();

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Invoice> results = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger successCount = new AtomicInteger(0);
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    Invoice invoice = invoiceService.createInvoiceAndPaymentLink(
                            studentId,
                            pricingPackageId,
                            idempotencyKey,
                            "http://localhost:3000/payment/success",
                            "http://localhost:3000/payment/cancel"
                    );
                    successCount.incrementAndGet();
                    results.add(invoice);
                } catch (Exception e) {
                    failures.add(e);
                }
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        for (Future<?> f : futures) {
            f.get(15, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(failures).as("all concurrent idempotent callers must succeed").isEmpty();
        assertThat(results).hasSize(threadCount);

        // All returned invoices must share the EXACT SAME invoice number and ID
        String firstInvoiceNumber = results.get(0).getInvoiceNumber();
        UUID firstInvoiceId = results.get(0).getId();

        for (Invoice inv : results) {
            assertThat(inv.getInvoiceNumber()).isEqualTo(firstInvoiceNumber);
            assertThat(inv.getId()).isEqualTo(firstInvoiceId);
        }

        // Verify DB only has 1 record for this idempotency key
        Integer countInDb = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM invoices WHERE idempotency_key = ?::uuid",
                Integer.class, idempotencyKey.toString()
        );
        assertThat(countInDb).isEqualTo(1);
    }

    private boolean isBookingConflict(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof BusinessException business
                    && business.getErrorCode() == ErrorCode.BOOKING_TIME_CONFLICT) {
                return true;
            }
            current = current.getCause();
        }
        // A concurrent insert can surface as a translated PostgreSQL exclusion
        // violation before the global exception mapper sees it.
        String message = failure.toString().toLowerCase();
        return message.contains("ex_booking_teacher_overlap")
                || message.contains("ex_booking_student_overlap")
                || message.contains("booking_time_conflict");
    }
}

