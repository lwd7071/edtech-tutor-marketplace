package com.edtech.platform.ranking.service;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TrialConversionTest {
    @Test void noCompletedTrialsReturnsZero() {
        assertEquals(0, TrialConversion.calculate(Map.of(), Map.of(UUID.randomUUID(), Instant.now())));
    }
    @Test void onlyPurchasesAfterCompletedTrialCountOncePerStudent() {
        UUID converted = UUID.randomUUID(), before = UUID.randomUUID(), unpaid = UUID.randomUUID();
        Instant trial = Instant.parse("2026-01-01T00:00:00Z");
        assertEquals(1.0 / 3, TrialConversion.calculate(
                Map.of(converted, trial, before, trial, unpaid, trial),
                Map.of(converted, trial.plusSeconds(1), before, trial.minusSeconds(1), UUID.randomUUID(), trial.plusSeconds(2))));
    }
}
