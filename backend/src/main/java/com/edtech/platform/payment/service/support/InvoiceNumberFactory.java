package com.edtech.platform.payment.service.support;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
public final class InvoiceNumberFactory {
    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private final Clock clock;
    public InvoiceNumberFactory(Clock clock) { this.clock = clock; }
    public String format(long sequence) {
        if (sequence <= 0) throw new IllegalArgumentException("sequence must be positive");
        return "INV-" + DATE.format(LocalDate.now(clock.withZone(ZoneOffset.UTC))) + "-" + sequence;
    }
}
