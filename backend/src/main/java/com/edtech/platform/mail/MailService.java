package com.edtech.platform.mail;

public interface MailService {
    void sendVerificationEmail(String email, String token);
    void sendPasswordResetEmail(String email, String token);
    void sendNotificationEmail(String to, String subject, String content);
}
