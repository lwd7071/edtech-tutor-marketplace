package com.edtech.platform.catalog.service;

import com.edtech.platform.catalog.dto.TeacherSearchParams;
import com.edtech.platform.catalog.repository.TeacherSearchRepository;
import com.edtech.platform.common.config.RedisCacheConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeacherSearchCache {
    private final TeacherSearchRepository repository;

    @Cacheable(
            value = RedisCacheConfig.POPULAR_SEARCH,
            key = "T(com.edtech.platform.catalog.service.TeacherSearchCacheKey).from(#params)",
            condition = "T(com.edtech.platform.catalog.service.TeacherSearchCacheKey).isCacheable(#params)",
            sync = true
    )
    public CachedTeacherSearchPage search(TeacherSearchParams params) {
        int page = params.page() == null ? 0 : params.page();
        int size = params.size() == null ? 20 : params.size();
        return CachedTeacherSearchPage.from(repository.searchTeachers(params, PageRequest.of(page, size)));
    }
}
