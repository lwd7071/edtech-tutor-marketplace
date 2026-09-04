package com.edtech.platform.admin.controller;

import com.edtech.platform.admin.dto.request.UpdatePlatformSettingsRequest;
import com.edtech.platform.admin.dto.response.PlatformSettingsView;
import com.edtech.platform.admin.service.AdminSettingsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminSettingsControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AdminSettingsService adminSettingsService;

    @InjectMocks
    private AdminSettingsController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getSettings_shouldReturn200() throws Exception {
        PlatformSettingsView view = new PlatformSettingsView(
                UUID.randomUUID(), new BigDecimal("5.00"), 10, 11, 12, Instant.now()
        );
        when(adminSettingsService.getSettings()).thenReturn(view);

        mockMvc.perform(get("/api/admin/settings")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.commissionRate").value(5.00))
                .andExpect(jsonPath("$.data.bayesianMinimumReviews").value(10));
    }

    @Test
    void updateSettings_shouldReturn200() throws Exception {
        UpdatePlatformSettingsRequest req = new UpdatePlatformSettingsRequest(
                new BigDecimal("7.50"), 15, 12, 24
        );
        PlatformSettingsView view = new PlatformSettingsView(
                UUID.randomUUID(), new BigDecimal("7.50"), 15, 12, 24, Instant.now()
        );
        when(adminSettingsService.updateSettings(any())).thenReturn(view);

        mockMvc.perform(put("/api/admin/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.commissionRate").value(7.50))
                .andExpect(jsonPath("$.data.bayesianMinimumReviews").value(15));
    }
}