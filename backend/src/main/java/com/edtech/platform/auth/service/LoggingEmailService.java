package com.edtech.platform.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.email.provider", havingValue = "logging")
public class LoggingEmailService implements EmailService {
    @Override
    public void sendVerificationEmail(String email, String token) {
        log.info("Email verification requested (logging provider; no email sent)");
    }

    @Override
    public void sendPasswordResetEmail(String email, String token) {
        log.info("Password reset requested (logging provider; no email sent)");
    }

    @Override
    public void sendNotificationEmail(String to, String subject, String content) {
        log.info("Notification email requested (logging provider; no email sent)");
    }
}
