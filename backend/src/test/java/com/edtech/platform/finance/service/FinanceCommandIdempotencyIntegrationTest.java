package com.edtech.platform.finance.service;

import com.edtech.platform.common.AbstractIntegrationTest;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.auth.domain.Role;
import com.edtech.platform.auth.domain.User;
import com.edtech.platform.auth.domain.UserStatus;
import com.edtech.platform.auth.repository.UserRepository;
import com.edtech.platform.finance.repository.FinanceCommandReceiptRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FinanceCommandIdempotencyIntegrationTest extends AbstractIntegrationTest {
    @Autowired FinanceCommandExecutor executor;
    @Autowired FinanceCommandReceiptRepository receipts;
    @Autowired UserRepository users;

    @Test
    void concurrentSameKey_runsSideEffectOnce_andReplaysCompletePayload() throws Exception {
        UUID actor = createActor();
        UUID key = UUID.randomUUID();
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Supplier<ApiResponse<String>> action = () -> {
                if (calls.incrementAndGet() == 1) {
                    firstEntered.countDown();
                    await(releaseFirst);
                }
                return ApiResponse.ok("completed");
            };
            Future<ApiResponse<String>> first = pool.submit(() -> executor.execute(actor, "TEST_CONCURRENT", key, "same", String.class, action));
            assertThat(firstEntered.await(10, TimeUnit.SECONDS)).isTrue();
            Future<ApiResponse<String>> second = pool.submit(() -> executor.execute(actor, "TEST_CONCURRENT", key, "same", String.class, action));
            releaseFirst.countDown();
            assertThat(first.get(20, TimeUnit.SECONDS).data()).isEqualTo("completed");
            assertThat(second.get(20, TimeUnit.SECONDS).data()).isEqualTo("completed");
            assertThat(calls).hasValue(1);
            assertThat(receipts.findByActorIdAndOperationAndIdempotencyKey(actor, "TEST_CONCURRENT", key)).isPresent();
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void failedCommand_rollsBackReceipt_andRetryCanClaim() {
        UUID actor = createActor();
        UUID key = UUID.randomUUID();
        AtomicBoolean fail = new AtomicBoolean(true);
        assertThatThrownBy(() -> executor.execute(actor, "TEST_RETRY", key, "same", String.class, () -> {
            if (fail.getAndSet(false)) throw new IllegalStateException("boom");
            return ApiResponse.ok("retry-ok");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(executor.execute(actor, "TEST_RETRY", key, "same", String.class,
                () -> ApiResponse.ok("retry-ok")).data()).isEqualTo("retry-ok");
        assertThat(receipts.findByActorIdAndOperationAndIdempotencyKey(actor, "TEST_RETRY", key)).isPresent();
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(20, TimeUnit.SECONDS)) throw new AssertionError("timed out waiting for release");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError(ex);
        }
    }

    private UUID createActor() {
        return users.save(User.builder()
                .email("finance-idempotency-" + UUID.randomUUID() + "@example.test")
                .passwordHash("hash")
                .fullName("Finance Test Actor")
                .role(Role.TEACHER)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .notifyParent(false)
                .build()).getId();
    }
}
