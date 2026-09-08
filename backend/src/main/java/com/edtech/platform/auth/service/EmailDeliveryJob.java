package com.edtech.platform.auth.service;

import com.edtech.platform.auth.repository.EmailOutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "smtp", matchIfMissing = true)
public class EmailDeliveryJob {
    private final EmailOutboxRepository outbox;
    private final JavaMailSender sender;
    private final String from;
    public EmailDeliveryJob(EmailOutboxRepository outbox, JavaMailSender sender, @Value("${app.email.from:noreply@tutormatch.local}") String from) {
        this.outbox = outbox;
        this.sender = sender;
        this.from = from;
    }
    @Scheduled(fixedDelayString = "${app.email.poll-delay-ms:10000}")
    @Transactional
    public void deliver() {
        for (var email : outbox.claimBatch()) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(email.recipient());
            message.setSubject(email.subject());
            message.setText(email.body());
            try {
                sender.send(message);
                outbox.sent(email.id());
            } catch (org.springframework.mail.MailException failure) {
                outbox.failed(email.id());
            }
        }
    }
}
