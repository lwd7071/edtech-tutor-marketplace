package com.edtech.platform.catalog.service;

import com.edtech.platform.common.config.RedisCacheConfig;
import com.edtech.platform.common.config.RedisCacheConfigurationContributor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import java.time.Duration;
import java.util.Map;

@Configuration
public class TeacherSearchCacheConfiguration implements RedisCacheConfigurationContributor {

    @Override
    public Map<String, RedisCacheConfiguration> configurations(RedisCacheConfiguration defaultConfiguration) {
        return Map.of(
                RedisCacheConfig.POPULAR_SEARCH,
                defaultConfiguration.entryTtl(Duration.ofMinutes(5))
                        .disableKeyPrefix()
                        .serializeValuesWith(SerializationPair.fromSerializer(serializer())));
    }

    static Jackson2JsonRedisSerializer<CachedTeacherSearchPage> serializer() {
        return new Jackson2JsonRedisSerializer<>(CachedTeacherSearchPage.class);
    }
}
