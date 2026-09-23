package com.buurman.dto.request;

import java.time.LocalDate;
import java.util.Optional;

import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@SkipTestCoverage
public record RefundContactCreditRequest(
    @NotNull(message = "Refund date is required") LocalDate refundDate,
    @Size(max = 1000) Optional<String> notes) {}
