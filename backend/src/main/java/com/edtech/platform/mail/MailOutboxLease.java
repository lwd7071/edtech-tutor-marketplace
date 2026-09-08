package com.edtech.platform.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class MailOutboxLease {
    private final MailOutboxRepository outbox;

    @Transactional
    public List<OutboundMail> claimBatch() {
        return outbox.claimBatch();
    }

    @Transactional
    public void markSent(UUID id) {
        outbox.markSent(id);
    }

    @Transactional
    public void markFailed(UUID id) {
        outbox.markFailed(id);
    }
}
