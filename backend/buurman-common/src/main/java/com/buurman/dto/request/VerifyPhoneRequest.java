package com.buurman.dto.request;

import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@SkipTestCoverage
public record VerifyPhoneRequest(
    @NotBlank(message = "Verification code is required") @Size(min = 6, max = 6, message = "Verification code must be 6 digits") String code) {}
