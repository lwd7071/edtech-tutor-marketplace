package com.edtech.platform.common.config;

import org.springframework.cache.annotation.EnableCaching;
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

@Configuration
@EnableCaching
public class RedisCacheConfig {

    public static final String SUBJECT_ACTIVE_LIST = "SUBJECT_ACTIVE_LIST";
    public static final String TEACHER_PUBLIC_PROFILE = "TEACHER_PUBLIC_PROFILE";
    public static final String GLOBAL_RANKING = "GLOBAL_RANKING";
    public static final String POPULAR_SEARCH = "POPULAR_SEARCH";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeValuesWith(SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // 24 hours
        cacheConfigurations.put(SUBJECT_ACTIVE_LIST, 
                defaultConfig.entryTtl(Duration.ofHours(24)));
        
        // 12 hours
        cacheConfigurations.put(TEACHER_PUBLIC_PROFILE, 
                defaultConfig.entryTtl(Duration.ofHours(12)));
        
        // 24 hours (evicted manually by job)
        cacheConfigurations.put(GLOBAL_RANKING, 
                defaultConfig.entryTtl(Duration.ofHours(24)));
        
        // 30 minutes
        cacheConfigurations.put(POPULAR_SEARCH, 
                defaultConfig.entryTtl(Duration.ofMinutes(30)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
