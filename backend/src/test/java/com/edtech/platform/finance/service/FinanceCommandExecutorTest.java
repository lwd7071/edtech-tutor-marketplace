package com.edtech.platform.finance.service;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.finance.domain.FinanceCommandReceipt;
import com.edtech.platform.finance.repository.FinanceCommandReceiptRepository;
import com.edtech.platform.common.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinanceCommandExecutorTest {
    @Mock FinanceCommandReceiptRepository receipts;
    private FinanceCommandExecutor executor;
    private final UUID actor = UUID.randomUUID();
    private final UUID key = UUID.randomUUID();
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-11T00:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() { executor = new FinanceCommandExecutor(receipts, new ObjectMapper(), clock); }

    @Test
    void sameKeyAndPayload_replaysWithoutRunningCommand() {
        AtomicInteger calls = new AtomicInteger();
        AtomicInteger claims = new AtomicInteger();
        AtomicReference<String> fingerprint = new AtomicReference<>();
        when(receipts.tryClaim(any(), eq(actor), eq("PAYOUT_CREATE"), eq(key), any(), eq(String.class.getName()), any(), any(), any()))
                .thenAnswer(invocation -> { fingerprint.set(invocation.getArgument(4)); return claims.getAndIncrement() == 0 ? 1 : 0; });
        AtomicReference<JsonNode> responsePayload = new AtomicReference<>(new ObjectMapper().nullNode());
        FinanceCommandReceipt claimed = mock(FinanceCommandReceipt.class);
        when(receipts.findByActorIdAndOperationAndIdempotencyKey(actor, "PAYOUT_CREATE", key)).thenReturn(Optional.of(claimed));
        when(claimed.getRequestFingerprint()).thenAnswer(invocation -> fingerprint.get());
        when(claimed.getResponsePayload()).thenAnswer(invocation -> responsePayload.get());
        doAnswer(invocation -> { responsePayload.set(invocation.getArgument(0)); return null; }).when(claimed).complete(any());
        ApiResponse<String> first = executor.execute(actor, "PAYOUT_CREATE", key, "payload", String.class,
                () -> { calls.incrementAndGet(); return ApiResponse.ok("created"); });
        assertThat(first.data()).isEqualTo("created");
        assertThat(calls).hasValue(1);

        ApiResponse<String> replay = executor.execute(actor, "PAYOUT_CREATE", key, "payload", String.class,
                () -> { calls.incrementAndGet(); return ApiResponse.ok("must-not-run"); });
        assertThat(replay.data()).isEqualTo("created");
        assertThat(calls).hasValue(1);

        verify(receipts).save(claimed);
    }

    @Test
    void reusedKeyWithDifferentPayload_isRejected() {
        when(receipts.tryClaim(any(), eq(actor), eq("PAYOUT_CREATE"), eq(key), any(), eq(String.class.getName()), any(), any(), any())).thenReturn(0);
        FinanceCommandReceipt existing = mock(FinanceCommandReceipt.class);
        when(receipts.findByActorIdAndOperationAndIdempotencyKey(actor, "PAYOUT_CREATE", key)).thenReturn(Optional.of(existing));
        when(existing.getRequestFingerprint()).thenReturn("different");
        assertThatThrownBy(() -> executor.execute(actor, "PAYOUT_CREATE", key, "payload", String.class,
                () -> ApiResponse.ok("must-not-run")))
                .isInstanceOf(BusinessException.class);
    }
}
