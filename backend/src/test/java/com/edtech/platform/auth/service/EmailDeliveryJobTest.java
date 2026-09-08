package com.edtech.platform.auth.service;

import com.edtech.platform.auth.repository.EmailOutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import java.util.List;
import java.util.UUID;
import static org.mockito.Mockito.*;

class EmailDeliveryJobTest {
    @Test void successfulDeliveryAcknowledgesOutboxOnlyAfterSending() {
        var outbox = mock(EmailOutboxRepository.class);
        var sender = mock(JavaMailSender.class);
        UUID id = UUID.randomUUID();
        when(outbox.claimBatch()).thenReturn(List.of(new EmailOutboxRepository.PendingEmail(id, "student@example.test", "Verify", "Token link", 0)));
        new EmailDeliveryJob(outbox, sender, "test@example.test").deliver();
        var order = inOrder(sender, outbox);
        order.verify(sender).send(any(SimpleMailMessage.class));
        order.verify(outbox).sent(id);
        verify(outbox, never()).failed(any());
    }
    @Test void transportFailureQueuesRetryAndContinuesBatch() {
        var outbox = mock(EmailOutboxRepository.class);
        var sender = mock(JavaMailSender.class);
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        when(outbox.claimBatch()).thenReturn(List.of(
                new EmailOutboxRepository.PendingEmail(first, "first@example.test", "Verify", "Link", 0),
                new EmailOutboxRepository.PendingEmail(second, "second@example.test", "Verify", "Link", 0)));
        doThrow(new MailSendException("Unavailable")).doNothing().when(sender).send(any(SimpleMailMessage.class));
        new EmailDeliveryJob(outbox, sender, "test@example.test").deliver();
        verify(outbox).failed(first);
        verify(outbox, never()).sent(first);
        verify(outbox).sent(second);
    }
}
