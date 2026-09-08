package com.edtech.platform.mail;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

class MailDeliveryJobTest {
    @Test
    void successfulDeliveryAcknowledgesOutboxOnlyAfterSending() {
        var outbox = mock(MailOutboxLease.class);
        var transport = mock(MailTransport.class);
        var mail = new OutboundMail(UUID.randomUUID(), "student@example.test", "Verify", "Token link", 0);
        when(outbox.claimBatch()).thenReturn(List.of(mail));

        new MailDeliveryJob(outbox, transport).deliver();

        var order = inOrder(transport, outbox);
        order.verify(transport).send(mail);
        order.verify(outbox).markSent(mail.id());
        verify(outbox, never()).markFailed(any());
    }

    @Test
    void transportFailureQueuesRetryAndContinuesBatch() {
        var outbox = mock(MailOutboxLease.class);
        var transport = mock(MailTransport.class);
        var first = new OutboundMail(UUID.randomUUID(), "first@example.test", "Verify", "Link", 0);
        var second = new OutboundMail(UUID.randomUUID(), "second@example.test", "Verify", "Link", 0);
        when(outbox.claimBatch()).thenReturn(List.of(first, second));
        doThrow(new IllegalStateException("Unavailable")).doNothing().when(transport).send(any());

        new MailDeliveryJob(outbox, transport).deliver();

        verify(outbox).markFailed(first.id());
        verify(outbox, never()).markSent(first.id());
        verify(outbox).markSent(second.id());
    }
}
