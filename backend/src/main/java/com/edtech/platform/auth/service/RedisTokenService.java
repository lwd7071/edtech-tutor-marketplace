package com.edtech.platform.auth.service;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class RedisTokenService {

    public enum Purpose {
        EMAIL_VERIFY(86400),
        PASSWORD_RESET(900),
        OAUTH_REGISTRATION(900),
        OAUTH_LOGIN_EXCHANGE(60);

        private final long ttlSeconds;

        Purpose(long ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }

        public long getTtlSeconds() {
            return ttlSeconds;
        }
    }

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisTokenService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String issue(Purpose purpose, Object payload) {
        String token = UUID.randomUUID().toString();
        String key = "auth:token:" + purpose.name() + ":" + token;
        redisTemplate.opsForValue().set(key, payload, Duration.ofSeconds(purpose.getTtlSeconds()));
        return token;
    }

    public <T> T consume(Purpose purpose, String token, Class<T> clazz) {
        String key = "auth:token:" + purpose.name() + ":" + token;
        Object payload = redisTemplate.opsForValue().get(key);
        
        if (payload == null) {
            ErrorCode errorCode = getErrorCodeForPurpose(purpose);
            throw new BusinessException(errorCode);
        }
        
        redisTemplate.delete(key);
        
        try {
            return clazz.cast(payload);
        } catch (ClassCastException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid token payload type");
        }
    }

    private ErrorCode getErrorCodeForPurpose(Purpose purpose) {
        return switch (purpose) {
            case EMAIL_VERIFY -> ErrorCode.AUTH_VERIFY_TOKEN_INVALID;
            case PASSWORD_RESET -> ErrorCode.AUTH_PASSWORD_RESET_TOKEN_INVALID;
            case OAUTH_REGISTRATION, OAUTH_LOGIN_EXCHANGE -> ErrorCode.AUTH_VERIFY_TOKEN_INVALID;
        };
    }
}
