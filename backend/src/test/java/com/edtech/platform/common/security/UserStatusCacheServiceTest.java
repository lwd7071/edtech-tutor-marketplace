package com.edtech.platform.common.security;

import com.edtech.platform.auth.facade.IdentityFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.UUID;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserStatusCacheServiceTest {
    @Mock RedisTemplate<String, Object> redis;
    @Mock ValueOperations<String, Object> values;
    @Mock IdentityFacade identities;

    @Test
    void cacheHitDoesNotQueryDatabase() {
        UUID userId = UUID.randomUUID();
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("auth:user-status:" + userId)).thenReturn("ACTIVE");

        assertThat(new UserStatusCacheService(redis, identities).resolve(userId)).contains("ACTIVE");

        verify(identities, never()).getStatus(userId);
    }

    @Test
    void redisFailureFallsBackToDatabase() {
        UUID userId = UUID.randomUUID();
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("auth:user-status:" + userId)).thenThrow(new IllegalStateException("redis down"));
        when(identities.getStatus(userId)).thenReturn(Optional.of("LOCKED"));

        assertThat(new UserStatusCacheService(redis, identities).resolve(userId)).contains("LOCKED");
    }
}
