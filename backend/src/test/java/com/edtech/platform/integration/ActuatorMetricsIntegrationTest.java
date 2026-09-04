package com.edtech.platform.integration;

import com.edtech.platform.common.AbstractIntegrationTest;
import com.edtech.platform.common.metrics.PlatformBusinessMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Task 9.3 - Actuator & Observability Integration Test")
@AutoConfigureMockMvc
class ActuatorMetricsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PlatformBusinessMetrics businessMetrics;

    @Test
    @DisplayName("Health endpoint should return UP status")
    void healthEndpoint_shouldReturnUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Prometheus metrics endpoint should expose business metrics")
    void prometheusEndpoint_shouldExposeBusinessMetrics() throws Exception {
        // Record test business metrics
        businessMetrics.recordInvoiceCreated();
        businessMetrics.recordInvoicePaid(500000L);
        businessMetrics.recordBookingCreated();
        businessMetrics.recordBookingCompleted();
        businessMetrics.recordPayoutRequested(200000L);
        businessMetrics.recordPayoutCompleted(200000L);
        businessMetrics.recordWalletSettled(475000L);

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("business_invoices_created_total")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("business_invoices_paid_total")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("business_bookings_created_total")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("business_bookings_completed_total")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("business_payouts_requested_total")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("business_payouts_completed_total")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("business_wallet_settled_amount_vnd")));
    }
}


