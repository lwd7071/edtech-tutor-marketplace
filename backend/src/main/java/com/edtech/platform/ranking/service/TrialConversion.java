package com.edtech.platform.ranking.service;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

final class TrialConversion {
    private TrialConversion() {}
    static double calculate(Map<UUID, Instant> trials, Map<UUID, Instant> purchases) {
        if (trials.isEmpty()) return 0;
        long converted = trials.entrySet().stream().filter(e -> purchases.containsKey(e.getKey()) && purchases.get(e.getKey()).isAfter(e.getValue())).count();
        return (double) converted / trials.size();
    }
}
