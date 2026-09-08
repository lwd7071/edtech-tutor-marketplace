package com.edtech.platform.common.config;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class FlywayStrategyTest {
    @Test void startupMustNeverRepairMigrationHistory() {
        Flyway flyway = mock(Flyway.class);
        new FlywayConfig().flywayMigrationStrategy().migrate(flyway);
        verify(flyway).migrate();
        verify(flyway, never()).repair();
    }
}
