package com.edtech.platform.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class RenderDeploymentConfigurationContractTest {
    private static final Path REPOSITORY_ROOT = Path.of("..");

    @Test
    void renderUsesTheLightweightPublicHealthEndpoint() throws Exception {
        String blueprint = Files.readString(REPOSITORY_ROOT.resolve("render.yaml"));

        assertThat(blueprint)
                .contains("runtime: docker")
                .contains("rootDir: backend")
                .contains("healthCheckPath: /health");
    }

    @Test
    void containerFitsInsideTheRenderFreeTierMemoryLimit() throws Exception {
        String dockerfile = Files.readString(Path.of("Dockerfile"));

        assertThat(dockerfile)
                .contains("-Xms128m")
                .contains("-Xmx256m")
                .contains("-XX:MaxMetaspaceSize=128m")
                .contains("-XX:ReservedCodeCacheSize=64m")
                .contains("ENV DB_POOL_MAX_SIZE=5")
                .contains("ENV DB_POOL_MIN_IDLE=1")
                .contains("-XX:+ExitOnOutOfMemoryError");
    }

    @Test
    void serverListensOnThePortProvidedByRender() throws Exception {
        String application = Files.readString(Path.of("src/main/resources/application.yml"));

        assertThat(application)
                .contains("port: ${PORT:8080}")
                .contains("address: ${SERVER_ADDRESS:0.0.0.0}");

        String dockerfile = Files.readString(Path.of("Dockerfile"));
        assertThat(dockerfile)
                .contains("ENV PORT=10000")
                .contains("EXPOSE 10000");
    }
}
