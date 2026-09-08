package com.edtech.platform.auth.service;

import com.edtech.platform.auth.repository.EmailOutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SmtpTransportTest {
    @Test void deliversAnActualMessageToLoopbackMailbox() throws Exception {
        try (var server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
             var executor = Executors.newSingleThreadExecutor()) {
            server.setSoTimeout(10000);
            Future<String> received = executor.submit(() -> {
                try (var socket = server.accept()) {
                    socket.setSoTimeout(10000);
                    var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                    var writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII), true);
                    writer.print("220 localhost test mailbox\r\n"); writer.flush();
                    var data = new StringBuilder(); boolean collecting = false;
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (collecting && !line.equals(".")) { data.append(line).append('\n'); continue; }
                        if (collecting) { collecting = false; writer.print("250 accepted\r\n"); }
                        else if (line.equals("DATA")) { collecting = true; writer.print("354 send message\r\n"); }
                        else if (line.equals("QUIT")) { writer.print("221 bye\r\n"); writer.flush(); break; }
                        else { writer.print("250 localhost\r\n"); }
                        writer.flush();
                    }
                    return data.toString();
                }
            });
            var sender = new JavaMailSenderImpl();
            sender.setHost(server.getInetAddress().getHostAddress()); sender.setPort(server.getLocalPort());
            sender.getJavaMailProperties().setProperty("mail.smtp.timeout", "10000");
            sender.getJavaMailProperties().setProperty("mail.smtp.connectiontimeout", "10000");
            var outbox = mock(EmailOutboxRepository.class); UUID id = UUID.randomUUID();
            when(outbox.claimBatch()).thenReturn(List.of(new EmailOutboxRepository.PendingEmail(id,"student@example.test","Tutor Match verification","http://localhost:3000/auth/verify-email?token=test-only",0)));
            new EmailDeliveryJob(outbox, sender, "noreply@example.test").deliver();
            String message = received.get(15, TimeUnit.SECONDS);
            assertTrue(message.contains("To: student@example.test"));
            assertTrue(message.contains("/auth/verify-email?token=test-only"));
            verify(outbox).sent(id); verify(outbox,never()).failed(any());
        }
    }
}
