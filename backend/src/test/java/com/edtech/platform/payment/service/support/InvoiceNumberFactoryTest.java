package com.edtech.platform.payment.service.support;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import static org.assertj.core.api.Assertions.assertThat;
class InvoiceNumberFactoryTest {
    @Test void formatsUtcDateAndDatabaseSequence() {
        InvoiceNumberFactory factory = new InvoiceNumberFactory(Clock.fixed(Instant.parse("2026-08-27T23:30:00Z"), ZoneOffset.UTC));
        assertThat(factory.format(42)).isEqualTo("INV-20260827-42");
    }
}
