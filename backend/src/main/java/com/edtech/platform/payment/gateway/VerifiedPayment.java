package com.edtech.platform.payment.gateway;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
public record VerifiedPayment(String providerReference, long orderCode, long amountVnd, Instant paidAt, JsonNode sanitizedPayload) { }
