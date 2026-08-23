package com.edtech.platform.common.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class RemovedEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void fixDbEndpointShouldNotBeExposed() throws Exception {
        // Given that FixDbController should be removed,
        // When we call the endpoint,
        // Then it should return 404 Not Found
        mockMvc.perform(get("/api/public/fix-db"))
               .andExpect(status().isNotFound());
    }
}
