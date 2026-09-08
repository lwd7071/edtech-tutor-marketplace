package com.edtech.platform.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MailDeliveryJob {
    private final MailOutboxLease outbox;
    private final MailTransport transport;

    @Scheduled(fixedDelayString = "${app.email.poll-delay-ms:10000}")
    public void deliver() {
        for (OutboundMail mail : outbox.claimBatch()) {
            try {
                transport.send(mail);
                outbox.markSent(mail.id());
                log.info("Mail delivered emailId={} attempts={}", mail.id(), mail.attempts() + 1);
            } catch (RuntimeException failure) {
                outbox.markFailed(mail.id());
                log.warn("Mail delivery failed emailId={} attempts={} cause={}",
                        mail.id(), mail.attempts() + 1, failure.getClass().getSimpleName());
            }
        }
    }
}
