package com.edtech.platform.finance.service;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.finance.domain.FinanceCommandReceipt;
import com.edtech.platform.finance.repository.FinanceCommandReceiptRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class FinanceCommandExecutor {
    private final FinanceCommandReceiptRepository receipts;
    private final ObjectMapper mapper;
    private final Clock clock;

    @Transactional
    public <T> ApiResponse<T> execute(UUID actorId, String operation, UUID key, Object command,
                                      Class<T> responseType, Supplier<ApiResponse<T>> action) {
        if (key == null) throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        String fingerprint = fingerprint(actorId, operation, command);
        Instant now = clock.instant();
        JsonNode empty = mapper.nullNode();
        int claimed = receipts.tryClaim(UUID.randomUUID(), actorId, operation, key, fingerprint,
                responseType.getName(), empty.toString(), now, now.plus(30, ChronoUnit.DAYS));
        FinanceCommandReceipt receipt = receipts.findByActorIdAndOperationAndIdempotencyKey(actorId, operation, key)
                .orElseThrow(() -> new IllegalStateException("idempotency receipt claim disappeared"));
        if (claimed == 0) {
            if (!receipt.getRequestFingerprint().equals(fingerprint)) {
                throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REUSED);
            }
            return ApiResponse.ok(mapper.convertValue(receipt.getResponsePayload(), responseType));
        }
        ApiResponse<T> response = action.get();
        JsonNode payload = mapper.valueToTree(response.data());
        receipt.complete(payload);
        receipts.save(receipt);
        return response;
    }

    private String fingerprint(UUID actorId, String operation, Object command) {
        JsonNode node = mapper.valueToTree(command);
        sanitize(node);
        String canonical = actorId + "|" + operation + "|" + node.toString();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("cannot fingerprint finance command", e);
        }
    }

    private void sanitize(JsonNode node) {
        if (node instanceof ObjectNode object) {
            object.fieldNames().forEachRemaining(name -> {
                JsonNode value = object.get(name);
                if (name.toLowerCase().contains("accountnumber")) {
                    object.put(name, sha256(value == null ? "" : value.asText()));
                } else {
                    sanitize(value);
                }
            });
        } else if (node != null && node.isArray()) {
            node.forEach(this::sanitize);
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
