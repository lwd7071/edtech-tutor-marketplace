package com.edtech.platform.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StartupProfileConfigurationContractTest {
    private static final Path MAIN_RESOURCES = Path.of("src/main/resources");
    private static final Path TEST_RESOURCES = Path.of("src/test/resources");

    @Test
    void cloudIsTheDefaultProfileAndLoadsThePrivateEnvironmentFile() throws Exception {
        assertThat(Files.readString(MAIN_RESOURCES.resolve("application.yml")))
                .contains("default: cloud")
                .doesNotContain("default: local");
        assertThat(Files.readString(MAIN_RESOURCES.resolve("application-cloud.yml")))
                .contains("optional:file:.env.cloud[.properties]")
                .contains("optional:file:../.env.cloud[.properties]");
    }

    @Test
    void localProfileOwnsAllProviderAndEncryptionDefaults() throws Exception {
        String local = Files.readString(MAIN_RESOURCES.resolve("application-local.yml"));

        assertThat(local)
                .contains("GOOGLE_CLIENT_ID:local-google-client-disabled")
                .contains("GOOGLE_CLIENT_SECRET:local-google-secret-disabled")
                .contains("CLOUDINARY_URL:cloudinary://local:local@local")
                .contains("EDTECH_ACCOUNT_ENCRYPTION_KEY:local-dev-account-key-32-chars!!");
    }

    @Test
    void cloudProfileRequiresSplitCloudinaryCredentialsAndEncryptionKey() throws Exception {
        String cloud = Files.readString(MAIN_RESOURCES.resolve("application-cloud.yml"));

        assertThat(cloud)
                .contains("${CLOUDINARY_CLOUD_NAME}")
                .contains("${CLOUDINARY_API_KEY}")
                .contains("${CLOUDINARY_API_SECRET}")
                .contains("${EDTECH_ACCOUNT_ENCRYPTION_KEY}")
                .doesNotContain("cloudinary://local")
                .doesNotContain("local-dev-account-key");
    }

    @Test
    void testProfileHasOneCanonicalConfigurationFile() {
        assertThat(MAIN_RESOURCES.resolve("application-test.yml")).exists();
        assertThat(TEST_RESOURCES.resolve("application-test.yml")).doesNotExist();
    }
}
