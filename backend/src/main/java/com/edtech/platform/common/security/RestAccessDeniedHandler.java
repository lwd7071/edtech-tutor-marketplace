package com.edtech.platform.common.security;

import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.response.ApiErrorDetail;
import com.edtech.platform.common.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        ErrorCode errorCode = ErrorCode.FORBIDDEN_RESOURCE;

        ApiResponse<Void> apiErrorResponse = ApiResponse.error(
                errorCode.getDefaultMessage(),
                List.of(new ApiErrorDetail(errorCode.name(), null, errorCode.getDefaultMessage())));

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(errorCode.getStatus().value());
        
        objectMapper.writeValue(response.getOutputStream(), apiErrorResponse);
    }
}
