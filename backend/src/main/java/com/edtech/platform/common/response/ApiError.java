package com.edtech.platform.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

public record ApiError(
    String code,
    String message,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    List<FieldErrorDetail> fieldErrors
) {
    public record FieldErrorDetail(String field, String message) {}
}
