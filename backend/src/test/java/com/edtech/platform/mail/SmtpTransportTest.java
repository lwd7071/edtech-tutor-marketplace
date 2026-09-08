package com.edtech.platform.mail;

import com.edtech.platform.common.config.properties.MailProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SmtpTransportTest {
    @Test
    void deliversAnActualMessageToLoopbackMailbox() throws Exception {
        try (var server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
             var executor = Executors.newSingleThreadExecutor()) {
            server.setSoTimeout(10000);
            var received = executor.submit(() -> {
                try (var socket = server.accept()) {
                    socket.setSoTimeout(10000);
                    var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                    var writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII), true);
                    writer.print("220 localhost test mailbox\r\n");
                    writer.flush();
                    var data = new StringBuilder();
                    boolean collecting = false;
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (collecting && !line.equals(".")) {
                            data.append(line).append('\n');
                            continue;
                        }
                        if (collecting) {
                            collecting = false;
                            writer.print("250 accepted\r\n");
                        } else if (line.equals("DATA")) {
                            collecting = true;
                            writer.print("354 send message\r\n");
                        } else if (line.equals("QUIT")) {
                            writer.print("221 bye\r\n");
                            writer.flush();
                            break;
                        } else {
                            writer.print("250 localhost\r\n");
                        }
                        writer.flush();
                    }
                    return data.toString();
                }
            });
            var sender = new JavaMailSenderImpl();
            sender.setHost(server.getInetAddress().getHostAddress());
            sender.setPort(server.getLocalPort());
            sender.getJavaMailProperties().setProperty("mail.smtp.timeout", "10000");
            sender.getJavaMailProperties().setProperty("mail.smtp.connectiontimeout", "10000");
            var properties = new MailProperties("smtp", "noreply@example.test", URI.create("http://localhost:3000"), 10000);
            var transport = new SmtpMailTransport(sender, properties);

            transport.send(new OutboundMail(UUID.randomUUID(), "student@example.test", "Tutor Match verification",
                    "http://localhost:3000/auth/verify-email?token=test-only", 0));

            String message = received.get(15, TimeUnit.SECONDS);
            assertTrue(message.contains("To: student@example.test"));
            assertTrue(message.contains("/auth/verify-email?token=test-only"));
        }
    }
}
