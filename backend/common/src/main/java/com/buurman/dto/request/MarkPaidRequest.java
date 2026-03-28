package com.buurman.dto.request;

import java.time.LocalDate;
import java.util.Optional;

import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.NotNull;

@SkipTestCoverage
public record MarkPaidRequest(
    @NotNull(message = "Payment date is required") LocalDate paymentDate, Optional<String> notes) {}
