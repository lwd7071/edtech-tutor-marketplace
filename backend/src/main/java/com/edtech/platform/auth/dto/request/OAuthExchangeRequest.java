package com.edtech.platform.auth.dto.request;
import jakarta.validation.constraints.NotBlank;
public record OAuthExchangeRequest(@NotBlank String exchangeCode) {}
