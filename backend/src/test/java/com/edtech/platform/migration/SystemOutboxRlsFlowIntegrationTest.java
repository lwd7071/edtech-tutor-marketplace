package com.edtech.platform.migration;

import com.edtech.platform.common.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class SystemOutboxRlsFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("V37 Cross-Table Flow: PlatformSettings -> AuditLog -> EmailOutbox Queue -> Worker Dispatch & Sent")
    void verifySystemOutboxFlowWithRlsEnabled() {
        UUID adminUserId = UUID.randomUUID();
        UUID auditId = UUID.randomUUID();
        UUID emailId = UUID.randomUUID();

        String adminEmail = "flow_adm_" + adminUserId.toString().substring(0, 8) + "@test.com";
        UUID settingId = null;
        BigDecimal originalRate = null;

        try {
            // 1. Setup Admin User
            jdbcTemplate.update("INSERT INTO public.users (id, email, password_hash, full_name, role, status) VALUES (?, ?, 'hash', 'Flow Admin', 'ADMIN', 'ACTIVE')",
                    adminUserId, adminEmail);

            // 2. Fetch or initialize Platform Settings (singleton)
            List<UUID> existingSettingIds = jdbcTemplate.queryForList("SELECT id FROM public.platform_settings LIMIT 1", UUID.class);
            if (existingSettingIds.isEmpty()) {
                settingId = UUID.randomUUID();
                jdbcTemplate.update("INSERT INTO public.platform_settings (id, commission_rate, bayesian_minimum_reviews, booking_reminder_hours, booking_expiration_hours) " +
                                "VALUES (?, 10.00, 10, 24, 48)",
                        settingId);
                originalRate = new BigDecimal("10.00");
            } else {
                settingId = existingSettingIds.get(0);
                originalRate = jdbcTemplate.queryForObject("SELECT commission_rate FROM public.platform_settings WHERE id = ?", BigDecimal.class, settingId);
            }

            // 3. Admin modifies platform commission rate
            jdbcTemplate.update("UPDATE public.platform_settings SET commission_rate = 15.00 WHERE id = ?", settingId);

            // 4. Audit Log is recorded for the admin action
            jdbcTemplate.update("INSERT INTO public.audit_logs (id, actor_id, action, target_type, target_id, after_data) " +
                            "VALUES (?, ?, 'UPDATE_COMMISSION_RATE', 'PLATFORM_SETTINGS', ?, cast('{\"new_rate\": 15.00}' as jsonb))",
                    auditId, adminUserId, settingId);

            // 5. System queues an alert email to system administrators in email_outbox
            jdbcTemplate.update("INSERT INTO public.email_outbox (id, recipient, subject, body, status, attempts, next_attempt_at) " +
                            "VALUES (?, 'admin-alerts@edtech.com', 'Alert: Commission rate changed', 'Rate changed to 15.00%', 'PENDING', 0, now())",
                    emailId);

            // 6. Verify pending status
            Map<String, Object> pendingEmail = jdbcTemplate.queryForMap(
                    "SELECT id, recipient, status, attempts FROM public.email_outbox WHERE id = ?", emailId);
            assertThat(pendingEmail.get("status")).isEqualTo("PENDING");
            assertThat(pendingEmail.get("attempts")).isEqualTo(0);

            // 7. Background worker polls pending emails and marks them SENT
            int dispatched = jdbcTemplate.update(
                    "UPDATE public.email_outbox SET status = 'SENT', sent_at = now(), attempts = attempts + 1 WHERE id = ? AND status = 'PENDING'",
                    emailId);
            assertThat(dispatched).isEqualTo(1);

            // 8. Verify final states
            Map<String, Object> updatedSettings = jdbcTemplate.queryForMap(
                    "SELECT commission_rate FROM public.platform_settings WHERE id = ?", settingId);
            assertThat(new BigDecimal(updatedSettings.get("commission_rate").toString())).isEqualByComparingTo("15.00");

            Map<String, Object> auditRecord = jdbcTemplate.queryForMap(
                    "SELECT action, target_type FROM public.audit_logs WHERE id = ?", auditId);
            assertThat(auditRecord.get("action")).isEqualTo("UPDATE_COMMISSION_RATE");
            assertThat(auditRecord.get("target_type")).isEqualTo("PLATFORM_SETTINGS");

            Map<String, Object> sentEmail = jdbcTemplate.queryForMap(
                    "SELECT status, sent_at, attempts FROM public.email_outbox WHERE id = ?", emailId);
            assertThat(sentEmail.get("status")).isEqualTo("SENT");
            assertThat(sentEmail.get("sent_at")).isNotNull();
            assertThat(sentEmail.get("attempts")).isEqualTo(1);

        } finally {
            // Clean up test data (revert settings to original, delete transient rows)
            if (settingId != null && originalRate != null) {
                jdbcTemplate.update("UPDATE public.platform_settings SET commission_rate = ? WHERE id = ?", originalRate, settingId);
            }
            jdbcTemplate.update("DELETE FROM public.email_outbox WHERE id = ?", emailId);
            jdbcTemplate.update("DELETE FROM public.audit_logs WHERE id = ?", auditId);
            jdbcTemplate.update("DELETE FROM public.users WHERE id = ?", adminUserId);
        }
    }
}