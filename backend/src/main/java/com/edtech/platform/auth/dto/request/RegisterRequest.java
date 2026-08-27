package com.edtech.platform.auth.dto.request;

import com.edtech.platform.auth.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(
    @NotBlank(message = "Email không được để trống") 
    @Email(message = "Email không đúng định dạng") 
    String email,
    
    @NotBlank(message = "Mật khẩu không được để trống") 
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$", 
             message = "Mật khẩu phải dài ít nhất 8 ký tự, bao gồm chữ cái, chữ số và ký tự đặc biệt")
    String password,
    
    @NotBlank(message = "Họ và tên không được để trống") 
    String fullName,
    
    @NotNull(message = "Vai trò không được để trống") 
    Role role,
    
    String parentFullName,
    String parentPhone,
    String parentEmail
) {}
