package com.edtech.platform.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisCacheConfigTest {

    @Test
    void cacheErrorsAreSwallowedSoDatabaseCanServeTheRequest() {
        Cache cache = mock(Cache.class);
        when(cache.getName()).thenReturn("POPULAR_SEARCH");

        assertThatCode(() -> new RedisCacheConfig().errorHandler()
                .handleCacheGetError(new IllegalStateException("redis unavailable"), cache, "key"))
                .doesNotThrowAnyException();
    }
}
