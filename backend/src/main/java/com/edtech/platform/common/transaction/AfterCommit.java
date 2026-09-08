package com.edtech.platform.common.transaction;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Publish only committed state. Direct invocations without a transaction run immediately. */
public final class AfterCommit {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AfterCommit.class);
    private AfterCommit() {}
    public static void run(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                try { action.run(); }
                catch (RuntimeException failure) { log.error("Post-commit delivery failed; persisted transaction remains committed", failure); }
            }
        });
    }
}
