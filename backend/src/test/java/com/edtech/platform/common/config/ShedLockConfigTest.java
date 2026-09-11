package com.edtech.platform.common.config;

import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ShedLockConfigTest {

    @Configuration(proxyBeanMethods = false)
    static class TestConfiguration {
    }

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class, ShedLockConfig.class)
            .withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class));

    @Test
    void shouldKeepSchedulingEnabledByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ShedLockConfig.class);
            assertThat(context).hasSingleBean(LockProvider.class);
            assertThat(context).hasSingleBean(ScheduledAnnotationBeanPostProcessor.class);
        });
    }

    @Test
    void shouldKeepSchedulingEnabledWhenExplicitlyEnabled() {
        contextRunner.withPropertyValues("app.scheduling.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(ShedLockConfig.class);
                    assertThat(context).hasSingleBean(LockProvider.class);
                    assertThat(context).hasSingleBean(ScheduledAnnotationBeanPostProcessor.class);
                });
    }

    @Test
    void shouldDisableSchedulingAndLockProviderWhenExplicitlyDisabled() {
        contextRunner.withPropertyValues("app.scheduling.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ShedLockConfig.class);
                    assertThat(context).doesNotHaveBean(LockProvider.class);
                    assertThat(context).doesNotHaveBean(ScheduledAnnotationBeanPostProcessor.class);
                });
    }
}
