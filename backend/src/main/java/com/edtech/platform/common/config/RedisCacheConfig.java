package com.edtech.platform.common.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableCaching
@Slf4j
public class RedisCacheConfig implements CachingConfigurer {

    public static final String SUBJECT_ACTIVE_LIST = "SUBJECT_ACTIVE_LIST";
    public static final String TEACHER_PUBLIC_PROFILE = "TEACHER_PUBLIC_PROFILE";
    public static final String GLOBAL_RANKING = "GLOBAL_RANKING";
    public static final String POPULAR_SEARCH = "POPULAR_SEARCH";

    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            List<RedisCacheConfigurationContributor> contributors) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeValuesWith(SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // 24 hours
        cacheConfigurations.put(SUBJECT_ACTIVE_LIST, 
                defaultConfig.entryTtl(Duration.ofHours(24)));
        
        // Public marketplace profiles may change; keep bounded staleness for the pilot.
        cacheConfigurations.put(TEACHER_PUBLIC_PROFILE, 
                defaultConfig.entryTtl(Duration.ofMinutes(30)));
        
        // 24 hours (evicted manually by job)
        cacheConfigurations.put(GLOBAL_RANKING, 
                defaultConfig.entryTtl(Duration.ofHours(24)));
        
        contributors.forEach(contributor ->
                cacheConfigurations.putAll(contributor.configurations(defaultConfig)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                warn("read", exception, cache, key);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                warn("write", exception, cache, key);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                warn("evict", exception, cache, key);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                warn("clear", exception, cache, "all");
            }

            private void warn(String operation, RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache {} failed for cache={} key={}; continuing without cache: {}",
                        operation, cache.getName(), key, exception.toString());
            }
        };
    }
}
