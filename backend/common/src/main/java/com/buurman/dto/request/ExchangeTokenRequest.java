package com.buurman.dto.request;

import java.util.UUID;

import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.NotNull;

@SkipTestCoverage
public record ExchangeTokenRequest(
    @NotNull(message = "Session token is required") UUID sessionToken) {}
