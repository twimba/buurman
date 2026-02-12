package com.buurman.dto.request.backoffice;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record UpdatePhoneNumberPolicyRequest(
    @NotNull(message = "Policy matrix is required")
    Map<String, List<String>> policyMatrix,

    @Min(value = 1, message = "Max codes per hour must be at least 1")
    @Max(value = 20, message = "Max codes per hour must be at most 20")
    int maxCodesPerHour,

    @Min(value = 1, message = "Verification code expiry must be at least 1 minute")
    @Max(value = 60, message = "Verification code expiry must be at most 60 minutes")
    int verificationCodeExpiryMinutes
) {}
