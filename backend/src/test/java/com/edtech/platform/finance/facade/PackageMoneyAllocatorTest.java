package com.edtech.platform.finance.facade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PackageMoneyAllocatorTest {

    private final PackageMoneyAllocator allocator = new PackageMoneyAllocator();

    @ParameterizedTest
    @CsvSource({
            "1000000, 0, 1000000",
            "1000000, 5, 950000",
            "1000000, 100, 0",
            "1, 50, 0",
            "3, 50, 1"
    })
    void teacherNetTotal_roundsCommissionOnce(
            long purchasePrice, String commissionPercent, long expectedNet) {
        assertThat(allocator.teacherNetTotal(purchasePrice, new BigDecimal(commissionPercent)))
                .isEqualTo(expectedNet);
    }

    @ParameterizedTest
    @CsvSource({
            "1, 10",
            "2, 3",
            "1000000, 3",
            "950000, 10",
            "1172838, 6"
    })
    void allocationsAcrossAllSessions_sumToExactTotal(long totalAmount, int totalSessions) {
        long allocated = 0;
        for (int resolvedBefore = 0; resolvedBefore < totalSessions; resolvedBefore++) {
            allocated += allocator.allocationForRange(totalAmount, totalSessions, resolvedBefore, 1);
        }
        assertThat(allocated).isEqualTo(totalAmount);
    }

    @Test
    void allocationForRange_supportsZeroAllocationsAndMultipleSessions() {
        assertThat(allocator.allocationForRange(1, 10, 0, 1)).isZero();
        assertThat(allocator.allocationForRange(1, 10, 9, 1)).isEqualTo(1);
        assertThat(allocator.allocationForRange(1_000_000, 3, 1, 2)).isEqualTo(666_667);
        assertThat(allocator.allocationForRange(2, 3, 0, 0)).isZero();
    }

    @Test
    void completedAndRefundedRanges_partitionTheTotalExactly() {
        long total = allocator.teacherNetTotal(1_000_000, new BigDecimal("5"));
        long completed = allocator.allocationForRange(total, 3, 0, 1);
        long refunded = allocator.allocationForRange(total, 3, 1, 2);
        assertThat(completed + refunded).isEqualTo(total);
    }

    @Test
    void rejectsInvalidFinancialInputsAndSessionRanges() {
        assertThatThrownBy(() -> allocator.teacherNetTotal(-1, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> allocator.teacherNetTotal(1, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> allocator.teacherNetTotal(1, new BigDecimal("-0.01")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> allocator.teacherNetTotal(1, new BigDecimal("100.01")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> allocator.allocationForRange(-1, 1, 0, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> allocator.allocationForRange(1, 0, 0, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> allocator.allocationForRange(1, 1, -1, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> allocator.allocationForRange(1, 1, 0, -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> allocator.allocationForRange(1, 2, 1, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void randomizedPartitions_preserveEveryVndAndRangeAdditivity() {
        Random random = new Random(20260910L);
        for (int sample = 0; sample < 1_000; sample++) {
            long totalAmount = random.nextLong(0, 1_000_000_000_001L);
            int totalSessions = random.nextInt(1, 101);
            int firstRange = random.nextInt(0, totalSessions + 1);
            int secondRange = random.nextInt(0, totalSessions - firstRange + 1);

            long sum = 0;
            for (int index = 0; index < totalSessions; index++) {
                sum += allocator.allocationForRange(totalAmount, totalSessions, index, 1);
            }
            assertThat(sum).isEqualTo(totalAmount);

            long combined = allocator.allocationForRange(
                    totalAmount, totalSessions, 0, firstRange + secondRange);
            long partitioned = allocator.allocationForRange(
                    totalAmount, totalSessions, 0, firstRange)
                    + allocator.allocationForRange(
                    totalAmount, totalSessions, firstRange, secondRange);
            assertThat(partitioned).isEqualTo(combined);
        }
    }
}
