package com.edtech.platform.common.exception;

import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.response.ApiErrorDetail;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerContractTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void businessErrorUsesRegistryCodeAndHttpStatus() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(
                new BusinessException(ErrorCode.BOOKING_TIME_CONFLICT));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().data()).isNull();
        assertThat(response.getBody().errors()).singleElement()
                .satisfies(error -> {
                    assertThat(error.code()).isEqualTo("BOOKING_TIME_CONFLICT");
                    assertThat(error.field()).isNull();
                });
    }

    @Test
    void malformedJsonDoesNotExposeParserMessage() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleHttpMessageNotReadableException(
                new HttpMessageNotReadableException("sensitive parser detail"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo(ErrorCode.MALFORMED_JSON.getDefaultMessage());
        assertThat(response.getBody().message()).doesNotContain("sensitive");
    }

    @Test
    void missingParameterHasFieldLevelValidationError() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleMissingParameter(
                new MissingServletRequestParameterException("status", "String"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errors()).singleElement()
                .satisfies(error -> {
                    assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
                    assertThat(error.field()).isEqualTo("status");
                });
    }

    @Test
    void unexpectedExceptionReturnsGenericMessage() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleException(
                new RuntimeException("database password and SQL detail"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage());
        assertThat(response.getBody().message()).doesNotContain("password", "SQL");
    }

    @Test
    void jpaOptimisticLockUsesConflictEnvelope() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleJpaOptimisticLockException(
                new jakarta.persistence.OptimisticLockException("stale"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().data()).isNull();
        assertThat(response.getBody().errors()).singleElement()
                .extracting(ApiErrorDetail::code)
                .isEqualTo(ErrorCode.CONCURRENT_MODIFICATION.name());
    }
}
