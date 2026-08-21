package com.edtech.platform.common.security;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class RateLimiterService {
    
    private static final byte[] LUA_SCRIPT = (
            "local count = redis.call('INCR', KEYS[1])\n" +
            "if tonumber(count) == 1 then\n" +
            "    redis.call('EXPIRE', KEYS[1], ARGV[1])\n" +
            "end\n" +
            "return count"
    ).getBytes(StandardCharsets.UTF_8);

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private final RedisTemplate<String, Object> redisTemplate;

    public RateLimiterService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void checkRateLimit(String action, String identifier, int maxRequests, long windowSeconds) {
        String key = "rate_limit:" + action + ":" + identifier;
        byte[] rawKey = key.getBytes(StandardCharsets.UTF_8);
        byte[] rawWindow = String.valueOf(windowSeconds).getBytes(StandardCharsets.UTF_8);
        
        Long count = redisTemplate.execute((RedisConnection connection) -> {
            return connection.scriptingCommands().eval(
                    LUA_SCRIPT, 
                    ReturnType.INTEGER, 
                    1, 
                    rawKey, 
                    rawWindow
            );
        });

        if (count != null && count > maxRequests) {
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }
    }
}

