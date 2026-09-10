package com.edtech.platform.finance.facade;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Shared package-money policy exposed at the finance module boundary.
 * Allocations use cumulative integer floors so every VND is assigned exactly once.
 */
@Component
public class PackageMoneyAllocator {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    public long teacherNetTotal(long purchasePriceVnd, BigDecimal commissionRatePercent) {
        if (purchasePriceVnd < 0) {
            throw new IllegalArgumentException("purchasePriceVnd must not be negative");
        }
        BigDecimal commissionRate = Objects.requireNonNull(
                commissionRatePercent, "commissionRatePercent is required");
        if (commissionRate.signum() < 0 || commissionRate.compareTo(ONE_HUNDRED) > 0) {
            throw new IllegalArgumentException("commissionRatePercent must be between 0 and 100");
        }

        long commissionTotal = BigDecimal.valueOf(purchasePriceVnd)
                .multiply(commissionRate)
                .divide(ONE_HUNDRED)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
        return Math.subtractExact(purchasePriceVnd, commissionTotal);
    }

    public long allocationForRange(
            long totalAmountVnd,
            int totalSessions,
            int resolvedBefore,
            int sessionsToResolve
    ) {
        if (totalAmountVnd < 0) {
            throw new IllegalArgumentException("totalAmountVnd must not be negative");
        }
        if (totalSessions <= 0) {
            throw new IllegalArgumentException("totalSessions must be positive");
        }
        if (resolvedBefore < 0 || sessionsToResolve < 0) {
            throw new IllegalArgumentException("session range must not be negative");
        }
        long resolvedAfter = (long) resolvedBefore + sessionsToResolve;
        if (resolvedAfter > totalSessions) {
            throw new IllegalArgumentException("session range exceeds totalSessions");
        }

        BigInteger total = BigInteger.valueOf(totalAmountVnd);
        BigInteger divisor = BigInteger.valueOf(totalSessions);
        BigInteger allocatedBefore = total.multiply(BigInteger.valueOf(resolvedBefore)).divide(divisor);
        BigInteger allocatedAfter = total.multiply(BigInteger.valueOf(resolvedAfter)).divide(divisor);
        return allocatedAfter.subtract(allocatedBefore).longValueExact();
    }
}
