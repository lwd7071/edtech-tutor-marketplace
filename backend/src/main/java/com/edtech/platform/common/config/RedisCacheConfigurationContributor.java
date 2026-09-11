package com.edtech.platform.common.config;

import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.util.Map;

public interface RedisCacheConfigurationContributor {
    Map<String, RedisCacheConfiguration> configurations(RedisCacheConfiguration defaultConfiguration);
}
