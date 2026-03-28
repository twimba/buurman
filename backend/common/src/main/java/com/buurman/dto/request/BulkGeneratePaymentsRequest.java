package com.buurman.dto.request;

import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@SkipTestCoverage
public record BulkGeneratePaymentsRequest(
    @NotNull(message = "Month is required") @Pattern(regexp = "\\d{4}-\\d{2}", message = "Month must be in YYYY-MM format") String forMonth) {}
