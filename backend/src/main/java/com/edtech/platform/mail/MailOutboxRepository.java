package com.edtech.platform.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class MailOutboxRepository {
    private final JdbcTemplate jdbc;

    void enqueue(String recipient, String subject, String body) {
        jdbc.update("insert into email_outbox(id, recipient, subject, body) values (?, ?, ?, ?)",
                UUID.randomUUID(), recipient, subject, body);
    }

    List<OutboundMail> claimBatch() {
        return jdbc.query("""
                        with claimed as (
                            select id from email_outbox
                            where status = 'PENDING' and next_attempt_at <= now()
                            order by created_at
                            limit 10
                            for update skip locked
                        )
                        update email_outbox email
                        set next_attempt_at = now() + interval '5 minutes'
                        from claimed
                        where email.id = claimed.id
                        returning email.id, email.recipient, email.subject, email.body, email.attempts
                        """,
                (row, index) -> new OutboundMail(
                        row.getObject("id", UUID.class),
                        row.getString("recipient"),
                        row.getString("subject"),
                        row.getString("body"),
                        row.getInt("attempts")));
    }

    void markSent(UUID id) {
        jdbc.update("update email_outbox set status='SENT', sent_at=now(), body='', attempts=attempts+1 where id=?", id);
    }

    void markFailed(UUID id) {
        jdbc.update("""
                update email_outbox
                set attempts=attempts+1,
                    status=case when attempts>=4 then 'FAILED' else 'PENDING' end,
                    next_attempt_at=now()+interval '1 minute',
                    body=case when attempts>=4 then '' else body end
                where id=?
                """, id);
    }
}
