package com.edtech.platform.booking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

class BookingSettlementTest {

    @ParameterizedTest
    @CsvSource({
            "1000000, 5.0, 10",
            "1000000, 5.0, 3",
            "750000, 7.5, 7",
            "500000, 10.0, 1",
            "1234567, 3.25, 6"
    })
    void cumulativeSettlement_sumOfAllSessionsMustEqualTeacherNetTotal(long purchasePrice, double commissionPercent, int totalSessions) {
        BigDecimal commRate = BigDecimal.valueOf(commissionPercent);
        BigDecimal feeRate = commRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        long commissionTotal = new BigDecimal(purchasePrice).multiply(feeRate).setScale(0, RoundingMode.HALF_UP).longValue();
        long teacherNetTotal = purchasePrice - commissionTotal;

        long sumAllocated = 0;
        for (int resolvedBefore = 0; resolvedBefore < totalSessions; resolvedBefore++) {
            long sessionNet = (long) Math.floor((double) (resolvedBefore + 1) * teacherNetTotal / totalSessions)
                    - (long) Math.floor((double) resolvedBefore * teacherNetTotal / totalSessions);
            assertThat(sessionNet).isGreaterThanOrEqualTo(0);
            sumAllocated += sessionNet;
        }

        assertThat(sumAllocated).isEqualTo(teacherNetTotal);
    }
}