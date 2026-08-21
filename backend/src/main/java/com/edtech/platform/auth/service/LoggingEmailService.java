package com.edtech.platform.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LoggingEmailService implements EmailService {
    @Override
    public void sendVerificationEmail(String email, String token) {
        log.info("Đã gửi email tới {}", email);
    }

    @Override
    public void sendPasswordResetEmail(String email, String token) {
        log.info("Đã gửi email tới {}", email);
    }

    @Override
    public void sendNotificationEmail(String to, String subject, String content) {
        log.info("Đã gửi email thông báo tới {}: [{}] {}", to, subject, content);
    }
}
