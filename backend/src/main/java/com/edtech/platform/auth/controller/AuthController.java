package com.edtech.platform.auth.controller;

import com.edtech.platform.auth.dto.request.LoginRequest;
import com.edtech.platform.auth.dto.request.RefreshRequest;
import com.edtech.platform.auth.dto.request.RegisterRequest;
import com.edtech.platform.auth.dto.response.AuthResult;
import com.edtech.platform.auth.dto.response.RegistrationResult;
import com.edtech.platform.auth.service.AccountVerificationService;
import com.edtech.platform.auth.service.OAuthAccountService;
import com.edtech.platform.auth.service.PasswordRecoveryService;
import com.edtech.platform.auth.service.RegistrationService;
import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegistrationService registration;
    private final SessionService sessions;
    private final AccountVerificationService verification;
    private final PasswordRecoveryService passwordRecovery;
    private final OAuthAccountService oauthAccounts;

    public AuthController(RegistrationService registration, SessionService sessions,
                          AccountVerificationService verification, PasswordRecoveryService passwordRecovery,
                          OAuthAccountService oauthAccounts) {
        this.registration = registration;
        this.sessions = sessions;
        this.verification = verification;
        this.passwordRecovery = passwordRecovery;
        this.oauthAccounts = oauthAccounts;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RegistrationResult> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        String ipAddress = httpRequest.getRemoteAddr();
        RegistrationResult result = registration.register(request, ipAddress);
        return ApiResponse.created(result);
    }

    @PostMapping("/login")
    public ApiResponse<AuthResult> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ipAddress = httpRequest.getRemoteAddr();
        AuthResult result = sessions.login(request, ipAddress);
        return ApiResponse.ok(result);
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResult> refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest httpRequest) {
        String ipAddress = httpRequest.getRemoteAddr();
        AuthResult result = sessions.refresh(request.refreshToken(), ipAddress);
        return ApiResponse.ok(result);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshRequest request) {
        sessions.logout(request.refreshToken());
        return ApiResponse.ok(null);
    }

    @PostMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(@Valid @RequestBody com.edtech.platform.auth.dto.request.VerifyEmailRequest request) {
        verification.verify(request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/resend-verification")
    public ApiResponse<Void> resendVerification(@Valid @RequestBody com.edtech.platform.auth.dto.request.ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        String ipAddress = httpRequest.getRemoteAddr();
        verification.resend(request, ipAddress);
        return ApiResponse.ok(null);
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody com.edtech.platform.auth.dto.request.ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        String ipAddress = httpRequest.getRemoteAddr();
        passwordRecovery.requestReset(request, ipAddress);
        return ApiResponse.ok(null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody com.edtech.platform.auth.dto.request.ResetPasswordRequest request) {
        passwordRecovery.reset(request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/oauth2/exchange")
    public ApiResponse<AuthResult> exchangeOAuthToken(@Valid @RequestBody com.edtech.platform.auth.dto.request.OAuthExchangeRequest request, HttpServletRequest httpRequest) {
        String ipAddress = httpRequest.getRemoteAddr();
        AuthResult result = oauthAccounts.exchange(request, ipAddress);
        return ApiResponse.ok(result);
    }

    @PostMapping("/oauth2/complete-registration")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResult> completeOAuthRegistration(@Valid @RequestBody com.edtech.platform.auth.dto.request.CompleteOAuthRegistrationRequest request, HttpServletRequest httpRequest) {
        String ipAddress = httpRequest.getRemoteAddr();
        AuthResult result = oauthAccounts.completeRegistration(request, ipAddress);
        return ApiResponse.created(result);
    }
}
