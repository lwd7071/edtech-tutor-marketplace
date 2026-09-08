package com.edtech.platform.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "logging")
public class LoggingMailTransport implements MailTransport {
    @Override
    public void send(OutboundMail mail) {
        log.info("Mail delivery simulated emailId={} attempts={} recipientDomain={}",
                mail.id(), mail.attempts() + 1, recipientDomain(mail.recipient()));
    }

    private String recipientDomain(String recipient) {
        int separator = recipient == null ? -1 : recipient.lastIndexOf('@');
        return separator < 0 ? "invalid" : recipient.substring(separator + 1);
    }
}
