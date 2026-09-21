package com.edtech.platform.mail;

final class PermanentMailDeliveryException extends RuntimeException {
    PermanentMailDeliveryException(String message) {
        super(message);
    }
}
