package com.edtech.platform.catalog.service;

import com.edtech.platform.catalog.dto.TeacherSearchParams;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TeacherSearchCacheKeyTest {

    @Test
    void equivalentVietnameseKeywordsShareOneCacheKey() {
        String key = TeacherSearchCacheKey.from(params("Toán", 0, 20));

        assertThat(key)
                .startsWith("teacher-search:v1:keyword=toan:")
                .isEqualTo(TeacherSearchCacheKey.from(params("  TOAN  ", 0, 20)));
    }

    @Test
    void cacheIsLimitedToFirstThreePagesAndFiftyItems() {
        assertThat(TeacherSearchCacheKey.isCacheable(params("toán", 2, 50))).isTrue();
        assertThat(TeacherSearchCacheKey.isCacheable(params("toán", 3, 50))).isFalse();
        assertThat(TeacherSearchCacheKey.isCacheable(params("toán", 0, 51))).isFalse();
    }

    private TeacherSearchParams params(String keyword, Integer page, Integer size) {
        return new TeacherSearchParams(
                keyword, null, null, null, null, null, null, null,
                null, "rating_desc", page, size);
    }
}
