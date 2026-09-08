package com.edtech.platform.mail;

public interface MailTransport {
    void send(OutboundMail mail);
}
