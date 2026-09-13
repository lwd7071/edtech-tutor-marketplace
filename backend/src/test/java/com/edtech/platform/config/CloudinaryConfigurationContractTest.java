package com.edtech.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import com.edtech.platform.common.config.properties.CloudinaryProperties;
import com.edtech.platform.common.storage.StorageConfig;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CloudinaryConfigurationContractTest {
    private static final Path RESOURCES = Path.of("src/main/resources");

    @Configuration
    @EnableConfigurationProperties(CloudinaryProperties.class)
    static class PropertiesConfig {}

    @Test
    void baseAndCloudProfilesMustNotContainCredentialFallback() throws Exception {
        assertThat(Files.readString(RESOURCES.resolve("application.yml"))).doesNotContain("cloudinary://dummy");
        assertThat(Files.readString(RESOURCES.resolve("application-cloud.yml"))).doesNotContain("cloudinary://dummy");
    }

    @Test
    void localAndTestProfilesOwnTheirNonProductionOverride() throws Exception {
        assertThat(Files.readString(RESOURCES.resolve("application-local.yml"))).contains("app:", "cloudinary:");
        assertThat(Files.readString(RESOURCES.resolve("application-test.yml"))).contains("app:", "cloudinary:");
    }

    @Test
    void storageConfigurationFailsFastWithoutCloudinaryUrl() {
        new ApplicationContextRunner()
                .withUserConfiguration(PropertiesConfig.class, StorageConfig.class)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(
                            org.springframework.boot.context.properties.bind.validation.BindValidationException.class);
                });
    }

    @Test
    void storageConfigurationStartsWithExplicitCloudinaryUrl() {
        new ApplicationContextRunner()
                .withUserConfiguration(PropertiesConfig.class, StorageConfig.class)
                .withPropertyValues("app.cloudinary.url=cloudinary://test:test@test")
                .run(context -> assertThat(context).hasNotFailed());
    }
}
