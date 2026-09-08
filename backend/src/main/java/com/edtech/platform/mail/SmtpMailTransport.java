package com.edtech.platform.mail;

import com.edtech.platform.common.config.properties.MailProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.email.provider", havingValue = "smtp", matchIfMissing = true)
public class SmtpMailTransport implements MailTransport {
    private final JavaMailSender sender;
    private final MailProperties properties;

    @Override
    public void send(OutboundMail mail) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.from());
        message.setTo(mail.recipient());
        message.setSubject(mail.subject());
        message.setText(mail.body());
        sender.send(message);
    }
}
