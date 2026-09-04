package com.edtech.platform.payment.controller;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.payment.gateway.InvalidPaymentSignatureException;
import com.edtech.platform.payment.gateway.PaymentGateway;
import com.edtech.platform.payment.gateway.PaymentGatewayRejectedException;
import com.edtech.platform.payment.gateway.PaymentGatewayUnavailableException;
import com.edtech.platform.payment.gateway.VerifiedPayment;
import com.edtech.platform.payment.service.PaymentWebhookService;
import com.edtech.platform.common.security.RateLimiterService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/api/webhooks/payos")
@RequiredArgsConstructor
public class PayOsWebhookController {

    private final PaymentGateway paymentGateway;
    private final PaymentWebhookService paymentWebhookService;
    private final RateLimiterService rateLimiterService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> handleWebhook(@RequestBody JsonNode body, HttpServletRequest request) {
        log.info("Received PayOS webhook request");
        try {
            String source = request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
            rateLimiterService.checkRateLimit("payos_webhook", source, 120, 60);
            // 1. Verify signature and extract domain record
            VerifiedPayment verifiedPayment = paymentGateway.verifyWebhook(body);

            // 2. Orchestrate payment effects
            paymentWebhookService.processWebhook(verifiedPayment);

            return ResponseEntity.ok(Map.of("success", true, "message", "Webhook processed successfully"));
        } catch (InvalidPaymentSignatureException e) {
            log.error("Invalid signature in PayOS webhook: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "error", "PAYMENT_SIGNATURE_INVALID"));
        } catch (BusinessException e) {
            log.warn("Business exception processing PayOS webhook: {}", e.getMessage());
            return ResponseEntity.status(e.getErrorCode().getStatus())
                    .body(Map.of("success", false, "error", e.getErrorCode().name()));
        } catch (PaymentGatewayRejectedException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("success", false, "error", "PAYMENT_GATEWAY_REJECTED"));
        } catch (PaymentGatewayUnavailableException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("success", false, "error", "PAYMENT_GATEWAY_UNAVAILABLE"));
        } catch (Exception e) {
            log.error("Unhandled error processing PayOS webhook", e);
            // Return 500 so PayOS can retry transient system issues
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Internal server error"));
        }
    }
}
