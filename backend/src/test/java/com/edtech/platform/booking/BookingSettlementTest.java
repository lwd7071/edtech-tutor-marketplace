package com.edtech.platform.booking;

import com.edtech.platform.finance.facade.PackageMoneyAllocator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BookingSettlementTest {

    private final PackageMoneyAllocator allocator = new PackageMoneyAllocator();

    @ParameterizedTest
    @CsvSource({
            "1000000, 5.0, 10",
            "1000000, 5.0, 3",
            "750000, 7.5, 7",
            "500000, 10.0, 1",
            "1234567, 3.25, 6"
    })
    void cumulativeSettlement_sumOfAllSessionsMustEqualTeacherNetTotal(long purchasePrice, String commissionPercent, int totalSessions) {
        BigDecimal commRate = new BigDecimal(commissionPercent);
        long teacherNetTotal = allocator.teacherNetTotal(purchasePrice, commRate);

        long sumAllocated = 0;
        for (int resolvedBefore = 0; resolvedBefore < totalSessions; resolvedBefore++) {
            long sessionNet = allocator.allocationForRange(teacherNetTotal, totalSessions, resolvedBefore, 1);
            assertThat(sessionNet).isGreaterThanOrEqualTo(0);
            sumAllocated += sessionNet;
        }

        assertThat(sumAllocated).isEqualTo(teacherNetTotal);
    }
}
