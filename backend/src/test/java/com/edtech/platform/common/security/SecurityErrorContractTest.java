package com.edtech.platform.common.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityErrorContractTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void authenticationEntryPointUsesSharedEnvelope() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new RestAuthenticationEntryPoint(objectMapper).commence(
                new MockHttpServletRequest(), response, new BadCredentialsException("do not expose"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertEnvelope(response);
    }

    @Test
    void accessDeniedHandlerUsesSharedEnvelope() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new RestAccessDeniedHandler(objectMapper).handle(
                new MockHttpServletRequest(), response, new AccessDeniedException("do not expose"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertEnvelope(response);
    }

    private void assertEnvelope(MockHttpServletResponse response) throws Exception {
        JsonNode json = objectMapper.readTree(response.getContentAsByteArray());
        Set<String> names = new HashSet<>();
        json.fieldNames().forEachRemaining(names::add);
        assertThat(names).containsExactlyInAnyOrder("success", "message", "data", "errors", "meta");
        assertThat(json.get("success").asBoolean()).isFalse();
        assertThat(json.get("data").isNull()).isTrue();
    }
}
