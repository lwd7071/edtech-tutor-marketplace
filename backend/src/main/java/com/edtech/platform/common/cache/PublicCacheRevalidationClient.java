package com.edtech.platform.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@Slf4j
public class PublicCacheRevalidationClient {

    private final boolean enabled;
    private final String url;
    private final String secret;
    private final int timeoutMs;

    public PublicCacheRevalidationClient(
            @Value("${app.cache.revalidation.enabled:true}") boolean enabled,
            @Value("${app.cache.revalidation.url:${app.email.frontend-url:http://localhost:3000}/api/internal/revalidate-public}") String url,
            @Value("${app.cache.revalidation.secret:local-internal-revalidate-secret}") String secret,
            @Value("${app.cache.revalidation.timeout-ms:2000}") int timeoutMs) {
        this.enabled = enabled;
        this.url = url;
        this.secret = secret;
        this.timeoutMs = timeoutMs;
    }

    /**
     * Revalidates Next.js Data Cache tags after the current transaction successfully commits.
     * If no transaction is active, the notification is sent immediately.
     */
    public void revalidateTeacherPublicData(UUID teacherId, String eventType) {
        if (!enabled || teacherId == null || eventType == null || eventType.isBlank()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendNotification(teacherId, eventType);
                }
            });
        } else {
            sendNotification(teacherId, eventType);
        }
    }

    private void sendNotification(UUID teacherId, String eventType) {
        if (url == null || url.isBlank()) {
            return;
        }
        try {
            String payload = String.format("{\"teacherId\":\"%s\",\"eventType\":\"%s\"}", teacherId, eventType);
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("x-internal-secret", secret);
            conn.setConnectTimeout(1000);
            conn.setReadTimeout(timeoutMs);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            if (status >= 200 && status < 300) {
                log.debug("Public cache revalidation sent successfully for teacher {}, event {}", teacherId, eventType);
            } else {
                log.warn("Public cache revalidation returned non-2xx status {} for teacher {}, event {}", status, teacherId, eventType);
            }
            conn.disconnect();
        } catch (Exception e) {
            log.warn("Public cache revalidation failed for teacher {}, event {}: {}", teacherId, eventType, e.getMessage());
        }
    }
}
