package com.edtech.platform.payment.service;

import com.edtech.platform.payment.gateway.VerifiedPayment;

public interface PaymentWebhookService {
    
    /**
     * Processes a verified payment from webhook.
     * This method runs inside a single database transaction to ensure atomicity
     * across Invoice, PaymentTransaction, StudentPackage, Wallet, and Ledger updates.
     *
     * @param payment The verified payment payload
     */
    void processWebhook(VerifiedPayment payment);
}
