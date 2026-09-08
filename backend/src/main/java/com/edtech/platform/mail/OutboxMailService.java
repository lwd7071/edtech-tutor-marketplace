package com.edtech.platform.mail;

import com.edtech.platform.common.config.properties.MailProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class OutboxMailService implements MailService {
    private final MailOutboxRepository outbox;
    private final MailProperties properties;

    @Override
    public void sendVerificationEmail(String email, String token) {
        sendNotificationEmail(email, "Xác minh tài khoản Tutor Match",
                "Mở liên kết để xác minh email: " + link("/auth/verify-email", token));
    }

    @Override
    public void sendPasswordResetEmail(String email, String token) {
        sendNotificationEmail(email, "Đặt lại mật khẩu Tutor Match",
                "Mở liên kết để đặt lại mật khẩu: " + link("/auth/reset-password", token));
    }

    @Override
    public void sendNotificationEmail(String to, String subject, String content) {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Email recipient is required");
        }
        outbox.enqueue(to, subject, content);
    }

    private String link(String path, String token) {
        return properties.normalizedFrontendUrl() + path + "?token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }
}
