package com.buurman.dto.request;

import java.time.LocalDate;
import java.util.Optional;

import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@SkipTestCoverage
public record WriteOffPaymentRequest(
    @NotBlank(message = "A reason is required") @Size(max = 1000) String reason,
    Optional<LocalDate> writeOffDate) {}
