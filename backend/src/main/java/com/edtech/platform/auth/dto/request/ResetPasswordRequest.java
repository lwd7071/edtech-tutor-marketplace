package com.edtech.platform.auth.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
public record ResetPasswordRequest(
    @NotBlank(message = "Token không được để trống") String token,
    
    @NotBlank(message = "Mật khẩu mới không được để trống") 
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$", 
             message = "Mật khẩu phải dài ít nhất 8 ký tự, bao gồm chữ cái, chữ số và ký tự đặc biệt")
    String newPassword
) {}
