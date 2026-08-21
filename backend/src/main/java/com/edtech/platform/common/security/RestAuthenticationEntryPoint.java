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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        ErrorCode errorCode = (ErrorCode) request.getAttribute("jwt_error");
        if (errorCode == null) {
            errorCode = ErrorCode.AUTH_TOKEN_MISSING;
        }

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
