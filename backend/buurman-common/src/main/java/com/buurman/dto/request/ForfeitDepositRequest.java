package com.buurman.dto.request;

import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@SkipTestCoverage
public record ForfeitDepositRequest(
    @NotBlank(message = "A reason is required") @Size(max = 1000) String reason) {}
