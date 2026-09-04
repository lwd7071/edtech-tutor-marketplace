package com.edtech.platform.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Custom Micrometer Business Metrics for platform transactions and operations.
 */
@Component
public class PlatformBusinessMetrics {

    private final Counter invoiceCreatedCounter;
    private final Counter invoicePaidCounter;
    private final Counter bookingCreatedCounter;
    private final Counter bookingCompletedCounter;
    private final Counter payoutRequestedCounter;
    private final Counter payoutCompletedCounter;
    private final DistributionSummary settledAmountSummary;

    public PlatformBusinessMetrics(MeterRegistry registry) {
        this.invoiceCreatedCounter = Counter.builder("business.invoices.created")
                .description("Total number of invoices created")
                .register(registry);

        this.invoicePaidCounter = Counter.builder("business.invoices.paid")
                .description("Total number of invoices marked as paid")
                .register(registry);

        this.bookingCreatedCounter = Counter.builder("business.bookings.created")
                .description("Total number of bookings scheduled")
                .register(registry);

        this.bookingCompletedCounter = Counter.builder("business.bookings.completed")
                .description("Total number of bookings completed")
                .register(registry);

        this.payoutRequestedCounter = Counter.builder("business.payouts.requested")
                .description("Total number of payout requests created")
                .register(registry);

        this.payoutCompletedCounter = Counter.builder("business.payouts.completed")
                .description("Total number of payout requests completed")
                .register(registry);

        this.settledAmountSummary = DistributionSummary.builder("business.wallet.settled_amount_vnd")
                .description("Summary of settled gross revenue in VND transferred to available balance")
                .baseUnit("VND")
                .register(registry);
    }

    public void recordInvoiceCreated() {
        invoiceCreatedCounter.increment();
    }

    public void recordInvoicePaid(long amountVnd) {
        invoicePaidCounter.increment();
    }

    public void recordBookingCreated() {
        bookingCreatedCounter.increment();
    }

    public void recordBookingCompleted() {
        bookingCompletedCounter.increment();
    }

    public void recordPayoutRequested(long amountVnd) {
        payoutRequestedCounter.increment();
    }

    public void recordPayoutCompleted(long amountVnd) {
        payoutCompletedCounter.increment();
    }

    public void recordWalletSettled(long amountVnd) {
        settledAmountSummary.record(amountVnd);
    }
}
