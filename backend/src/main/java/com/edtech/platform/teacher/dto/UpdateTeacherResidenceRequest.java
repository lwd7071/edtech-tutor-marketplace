package com.edtech.platform.teacher.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;

public record UpdateTeacherResidenceRequest(
        @Pattern(regexp = "^[0-9]{2}$", message = "Mã tỉnh phải gồm 2 chữ số")
        String provinceCode,
        @Pattern(regexp = "^[0-9]{5}$", message = "Mã xã/phường phải gồm 5 chữ số")
        String wardCode
) {
    @AssertTrue(message = "Phải chọn tỉnh khi chọn xã/phường")
    public boolean isProvincePresentWhenWardSelected() {
        return wardCode == null || provinceCode != null;
    }
}
