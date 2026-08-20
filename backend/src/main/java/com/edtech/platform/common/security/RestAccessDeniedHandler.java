package com.edtech.platform.common.security;

import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.response.ApiError;
import com.edtech.platform.common.response.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        ErrorCode errorCode = ErrorCode.FORBIDDEN_RESOURCE;

        String requestId = MDC.get("requestId");
        if (requestId == null) {
            requestId = "unknown";
        }

        ApiError apiError = new ApiError(errorCode.name(), errorCode.getDefaultMessage(), null);
        ApiErrorResponse apiErrorResponse = new ApiErrorResponse(false, apiError, Instant.now(), requestId);

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(errorCode.getStatus().value());
        
        objectMapper.writeValue(response.getOutputStream(), apiErrorResponse);
    }
}
