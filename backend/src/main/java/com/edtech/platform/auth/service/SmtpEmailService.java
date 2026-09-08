package com.edtech.platform.auth.service;

import com.edtech.platform.auth.repository.EmailOutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "smtp", matchIfMissing = true)
@Transactional
public class SmtpEmailService implements EmailService {
    private final EmailOutboxRepository outbox;
    private final String frontendUrl;

    public SmtpEmailService(EmailOutboxRepository outbox, @Value("${app.email.frontend-url:http://localhost:3000}") String frontendUrl) {
        this.outbox = outbox;
        URI uri = URI.create(frontendUrl);
        if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme())) || uri.getHost() == null || uri.getQuery() != null || uri.getFragment() != null)
            throw new IllegalArgumentException("app.email.frontend-url must be an HTTP(S) URL without query or fragment");
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    public void sendVerificationEmail(String email, String token) {
        sendNotificationEmail(email, "Xác minh tài khoản Tutor Match", "Mở liên kết để xác minh email: " + link("/auth/verify-email", token));
    }
    public void sendPasswordResetEmail(String email, String token) {
        sendNotificationEmail(email, "Đặt lại mật khẩu Tutor Match", "Mở liên kết để đặt lại mật khẩu: " + link("/auth/reset-password", token));
    }
    public void sendNotificationEmail(String to, String subject, String content) {
        if (to == null || to.isBlank()) throw new IllegalArgumentException("Email recipient is required");
        outbox.enqueue(to, subject, content);
    }
    private String link(String path, String token) {
        return frontendUrl + path + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }
}
