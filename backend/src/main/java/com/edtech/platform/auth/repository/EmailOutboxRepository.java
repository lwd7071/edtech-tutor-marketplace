package com.edtech.platform.auth.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class EmailOutboxRepository {
    private final JdbcTemplate jdbc;
    public record PendingEmail(UUID id, String recipient, String subject, String body, int attempts) {}

    public void enqueue(String recipient, String subject, String body) {
        jdbc.update("insert into email_outbox(id, recipient, subject, body) values (?, ?, ?, ?)", UUID.randomUUID(), recipient, subject, body);
    }

    public List<PendingEmail> claimBatch() {
        return jdbc.query("select id, recipient, subject, body, attempts from email_outbox where status='PENDING' and next_attempt_at<=now() order by created_at limit 10 for update skip locked",
                (r, n) -> new PendingEmail(r.getObject("id", UUID.class), r.getString("recipient"), r.getString("subject"), r.getString("body"), r.getInt("attempts")));
    }

    public void sent(UUID id) {
        jdbc.update("update email_outbox set status='SENT', sent_at=now(), body='', attempts=attempts+1 where id=?", id);
    }

    public void failed(UUID id) {
        jdbc.update("update email_outbox set attempts=attempts+1, status=case when attempts>=4 then 'FAILED' else 'PENDING' end, next_attempt_at=now()+interval '1 minute', body=case when attempts>=4 then '' else body end where id=?", id);
    }
}
