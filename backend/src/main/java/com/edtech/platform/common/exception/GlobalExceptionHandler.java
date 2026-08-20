package com.edtech.platform.common.exception;

import com.edtech.platform.common.response.ApiError;
import com.edtech.platform.common.response.ApiErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private String getRequestId() {
        String reqId = MDC.get("requestId");
        return reqId != null ? reqId : "unknown";
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(ErrorCode errorCode, String customMessage, List<ApiError.FieldErrorDetail> fieldErrors) {
        String message = customMessage != null ? customMessage : errorCode.getDefaultMessage();
        ApiError apiError = new ApiError(errorCode.name(), message, fieldErrors);
        ApiErrorResponse response = new ApiErrorResponse(false, apiError, Instant.now(), getRequestId());
        return new ResponseEntity<>(response, errorCode.getStatus());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException ex) {
        return buildResponse(ex.getErrorCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        List<ApiError.FieldErrorDetail> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> new ApiError.FieldErrorDetail(err.getField(), err.getDefaultMessage()))
                .toList();
        return buildResponse(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getDefaultMessage(), fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        return buildResponse(ErrorCode.MALFORMED_JSON, null, null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException ex) {
        return buildResponse(ErrorCode.UNSUPPORTED_MEDIA_TYPE, null, null);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleOptimisticLockingFailureException(OptimisticLockingFailureException ex) {
        return buildResponse(ErrorCode.CONCURRENT_MODIFICATION, null, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        return buildResponse(ErrorCode.DUPLICATE_RESOURCE, null, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        return buildResponse(ErrorCode.FORBIDDEN_RESOURCE, null, null);
    }

    @ExceptionHandler({AuthenticationException.class, InsufficientAuthenticationException.class})
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(RuntimeException ex) {
        return buildResponse(ErrorCode.AUTH_TOKEN_MISSING, null, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(Exception ex) {
        log.error("Unhandled exception: ", ex);
        return buildResponse(ErrorCode.INTERNAL_SERVER_ERROR, null, null);
    }
}
