package com.edtech.platform.common.security;

import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.response.ApiErrorDetail;
import com.edtech.platform.common.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        ErrorCode errorCode = (ErrorCode) request.getAttribute("jwt_error");
        if (errorCode == null) {
            errorCode = ErrorCode.AUTH_TOKEN_MISSING;
        }

        ApiResponse<Void> apiErrorResponse = ApiResponse.error(
                errorCode.getDefaultMessage(),
                List.of(new ApiErrorDetail(errorCode.name(), null, errorCode.getDefaultMessage())));

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(errorCode.getStatus().value());
        
        objectMapper.writeValue(response.getOutputStream(), apiErrorResponse);
    }
}
