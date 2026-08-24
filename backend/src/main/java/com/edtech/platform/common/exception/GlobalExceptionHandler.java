package com.edtech.platform.common.exception;

import com.edtech.platform.common.response.ApiErrorDetail;
import com.edtech.platform.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
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
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private String getRequestId() {
        String reqId = MDC.get("requestId");
        return reqId != null ? reqId : "unknown";
    }

    private ResponseEntity<ApiResponse<Void>> buildResponse(
            ErrorCode errorCode, String customMessage, List<ApiErrorDetail> details) {
        String message = customMessage != null ? customMessage : errorCode.getDefaultMessage();
        List<ApiErrorDetail> errors = details != null && !details.isEmpty()
                ? details
                : List.of(new ApiErrorDetail(errorCode.name(), null, message));
        ApiResponse<Void> response = ApiResponse.error(message, errors);
        return new ResponseEntity<>(response, errorCode.getStatus());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        return buildResponse(ex.getErrorCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        List<ApiErrorDetail> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> new ApiErrorDetail(ErrorCode.VALIDATION_ERROR.name(), err.getField(), err.getDefaultMessage()))
                .toList();
        return buildResponse(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getDefaultMessage(), fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        return buildResponse(ErrorCode.MALFORMED_JSON, null, null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException ex) {
        return buildResponse(ErrorCode.UNSUPPORTED_MEDIA_TYPE, null, null);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLockingFailureException(OptimisticLockingFailureException ex) {
        return buildResponse(ErrorCode.CONCURRENT_MODIFICATION, null, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        return buildResponse(ErrorCode.DUPLICATE_RESOURCE, null, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        return buildResponse(ErrorCode.FORBIDDEN_RESOURCE, null, null);
    }

    @ExceptionHandler({AuthenticationException.class, InsufficientAuthenticationException.class})
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(RuntimeException ex) {
        return buildResponse(ErrorCode.AUTH_TOKEN_MISSING, null, null);
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(org.springframework.web.server.ResponseStatusException ex) {
        ErrorCode errorCode = ex.getStatusCode() == org.springframework.http.HttpStatus.NOT_FOUND ? ErrorCode.RESOURCE_NOT_FOUND : ErrorCode.INTERNAL_SERVER_ERROR;
        return buildResponse(errorCode, null, null);
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        return buildResponse(ErrorCode.RESOURCE_NOT_FOUND, null, null);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException ex) {
        List<ApiErrorDetail> errors = ex.getConstraintViolations().stream()
                .map(violation -> new ApiErrorDetail(
                        ErrorCode.VALIDATION_ERROR.name(),
                        lastPathSegment(violation.getPropertyPath().toString()),
                        violation.getMessage()))
                .toList();
        return buildResponse(ErrorCode.VALIDATION_ERROR, null, errors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return buildResponse(ErrorCode.VALIDATION_ERROR, null,
                List.of(new ApiErrorDetail(ErrorCode.VALIDATION_ERROR.name(), ex.getName(), "Kiểu dữ liệu không hợp lệ")));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException ex) {
        return buildResponse(ErrorCode.VALIDATION_ERROR, null,
                List.of(new ApiErrorDetail(ErrorCode.VALIDATION_ERROR.name(), ex.getParameterName(), "Thiếu tham số bắt buộc")));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingPart(MissingServletRequestPartException ex) {
        return buildResponse(ErrorCode.VALIDATION_ERROR, null,
                List.of(new ApiErrorDetail(ErrorCode.VALIDATION_ERROR.name(), ex.getRequestPartName(), "Thiếu phần multipart bắt buộc")));
    }

    @ExceptionHandler({MultipartException.class, MaxUploadSizeExceededException.class})
    public ResponseEntity<ApiResponse<Void>> handleMultipartException(Exception ex) {
        return buildResponse(ErrorCode.VALIDATION_ERROR, "Dữ liệu tải lên không hợp lệ", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("Unhandled exception type={}, requestId={}", ex.getClass().getName(), getRequestId());
        return buildResponse(ErrorCode.INTERNAL_SERVER_ERROR, null, null);
    }

    private String lastPathSegment(String path) {
        int separator = path.lastIndexOf('.');
        return separator >= 0 ? path.substring(separator + 1) : path;
    }
}
