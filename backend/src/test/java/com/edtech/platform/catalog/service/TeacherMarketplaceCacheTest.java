package com.edtech.platform.catalog.service;

import com.edtech.platform.auth.facade.IdentityFacade;
import com.edtech.platform.catalog.dto.TeacherSearchParams;
import com.edtech.platform.catalog.repository.TeacherSearchRepository;
import com.edtech.platform.ranking.facade.TeacherStatsFacade;
import com.edtech.platform.subject.facade.SubjectFacade;
import com.edtech.platform.teacher.facade.TeacherFacade;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeacherMarketplaceCacheTest {

    @Test
    void equivalentVietnameseSearchesShareCachedResult() {
        try (var context = new AnnotationConfigApplicationContext(CacheTestConfig.class)) {
            TeacherSearchRepository repository = context.getBean(TeacherSearchRepository.class);
            when(repository.searchTeachers(any(), any())).thenReturn(Page.empty());
            TeacherSearchCache service = context.getBean(TeacherSearchCache.class);

            service.search(params("Toán", 0, 20));
            service.search(params("  TOAN  ", 0, 20));

            verify(repository, times(1)).searchTeachers(any(), any());
        }
    }

    @Test
    void pagesOutsideCachePolicyAlwaysReachRepository() {
        try (var context = new AnnotationConfigApplicationContext(CacheTestConfig.class)) {
            TeacherSearchRepository repository = context.getBean(TeacherSearchRepository.class);
            when(repository.searchTeachers(any(), any())).thenReturn(Page.empty());
            TeacherSearchCache service = context.getBean(TeacherSearchCache.class);

            service.search(params("Toán", 3, 20));
            service.search(params("Toán", 3, 20));

            verify(repository, times(2)).searchTeachers(any(), any());
        }
    }

    private TeacherSearchParams params(String keyword, Integer page, Integer size) {
        return new TeacherSearchParams(
                keyword, null, null, null, null, null, null, null,
                null, "rating_desc", page, size);
    }

    @Configuration
    @EnableCaching
    static class CacheTestConfig {
        @Bean CacheManager cacheManager() { return new ConcurrentMapCacheManager(); }
        @Bean TeacherSearchRepository repository() { return mock(TeacherSearchRepository.class); }
        @Bean TeacherFacade teacherFacade() { return mock(TeacherFacade.class); }
        @Bean IdentityFacade identityFacade() { return mock(IdentityFacade.class); }
        @Bean TeacherStatsFacade teacherStatsFacade() { return mock(TeacherStatsFacade.class); }
        @Bean SubjectFacade subjectFacade() { return mock(SubjectFacade.class); }
        @Bean PricingPackageService pricingPackageService() { return mock(PricingPackageService.class); }

        @Bean
        TeacherSearchCache teacherSearchCache(TeacherSearchRepository repository) {
            return new TeacherSearchCache(repository);
        }

        @Bean
        TeacherMarketplaceService teacherMarketplaceService(
                TeacherFacade teacherFacade,
                IdentityFacade identityFacade,
                TeacherStatsFacade teacherStatsFacade,
                SubjectFacade subjectFacade,
                PricingPackageService pricingPackageService,
                TeacherSearchCache teacherSearchCache) {
            return new TeacherMarketplaceService(
                    teacherFacade,
                    identityFacade,
                    teacherStatsFacade,
                    subjectFacade,
                    pricingPackageService,
                    teacherSearchCache);
        }
    }
}
