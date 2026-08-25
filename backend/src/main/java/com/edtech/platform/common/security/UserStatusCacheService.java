package com.edtech.platform.common.security;

import com.edtech.platform.auth.facade.IdentityFacade;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserStatusCacheService {
    private static final Logger log = LoggerFactory.getLogger(UserStatusCacheService.class);
    private static final String PREFIX = "auth:user-status:";
    private static final Duration TTL = Duration.ofSeconds(30);

    private final RedisTemplate<String, Object> redis;
    private final IdentityFacade identities;

    public Optional<String> resolve(UUID userId) {
        String key = key(userId);
        try {
            Object cached = redis.opsForValue().get(key);
            if (cached != null) return Optional.of(cached.toString());
        } catch (RuntimeException ex) {
            log.warn("User status cache read failed for userId={}; falling back to database", userId);
            return identities.getStatus(userId);
        }
        Optional<String> status = identities.getStatus(userId);
        status.ifPresent(value -> putSafely(key, value));
        return status;
    }

    public void beforeStatusChange(UUID userId) {
        evictSafely(key(userId));
    }

    public void afterStatusChange(UUID userId, String status) {
        Runnable update = () -> {
            try {
                redis.opsForValue().set(key(userId), status, TTL);
            } catch (RuntimeException ex) {
                evictSafely(key(userId));
                log.warn("User status cache write-through failed for userId={}", userId);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { update.run(); }
            });
        } else {
            update.run();
        }
    }

    private void putSafely(String key, String value) {
        try { redis.opsForValue().set(key, value, TTL); }
        catch (RuntimeException ex) { log.warn("User status cache populate failed for key={}", key); }
    }

    private void evictSafely(String key) {
        try { redis.delete(key); }
        catch (RuntimeException ex) { log.warn("User status cache eviction failed for key={}", key); }
    }

    private String key(UUID userId) { return PREFIX + userId; }
}
