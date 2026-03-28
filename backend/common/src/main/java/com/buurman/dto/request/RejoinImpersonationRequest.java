package com.buurman.dto.request;

import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.NotBlank;

@SkipTestCoverage
public record RejoinImpersonationRequest(
    @NotBlank(message = "Password is required") String password) {}
