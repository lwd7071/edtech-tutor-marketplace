package com.edtech.platform.common.cache;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PublicCacheRevalidationClientTest {

    @Test
    void revalidateTeacherPublicData_doesNotThrowWhenEndpointUnreachable() {
        // Port 59999 has no listener
        PublicCacheRevalidationClient client = new PublicCacheRevalidationClient(
                true,
                "http://127.0.0.1:59999/api/internal/revalidate-public",
                "secret-123",
                200
        );

        // When called without transaction, does not throw
        assertDoesNotThrow(() -> client.revalidateTeacherPublicData(UUID.randomUUID(), "TEACHER_CREDENTIAL_APPROVED"));
    }

    @Test
    void revalidateTeacherPublicData_disabledClientDoesNothing() {
        PublicCacheRevalidationClient client = new PublicCacheRevalidationClient(
                false,
                "http://127.0.0.1:59999/api/internal/revalidate-public",
                "secret-123",
                200
        );

        assertDoesNotThrow(() -> client.revalidateTeacherPublicData(UUID.randomUUID(), "TEACHER_RESIDENCE_UPDATED"));
    }

    @Test
    void revalidateTeacherPublicData_registersSynchronizationWhenTransactionActive() {
        PublicCacheRevalidationClient client = new PublicCacheRevalidationClient(
                true,
                "http://127.0.0.1:59999/api/internal/revalidate-public",
                "secret-123",
                200
        );

        TransactionSynchronizationManager.initSynchronization();
        try {
            client.revalidateTeacherPublicData(UUID.randomUUID(), "TEACHER_RESIDENCE_UPDATED");
            // Verify synchronizations registered
            var synchronizations = TransactionSynchronizationManager.getSynchronizations();
            org.junit.jupiter.api.Assertions.assertEquals(1, synchronizations.size());

            // Trigger afterCommit directly to verify safe execution
            TransactionSynchronization sync = synchronizations.get(0);
            assertDoesNotThrow(sync::afterCommit);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
