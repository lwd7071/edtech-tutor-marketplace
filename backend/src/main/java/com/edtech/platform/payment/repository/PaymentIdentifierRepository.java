package com.edtech.platform.payment.repository;
public interface PaymentIdentifierRepository {
    long nextInvoiceNumberSequence();
    long nextPayosOrderCode();
}
