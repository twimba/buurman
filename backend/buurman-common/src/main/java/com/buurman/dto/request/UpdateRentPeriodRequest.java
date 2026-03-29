package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.buurman.util.SkipTestCoverage;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@SkipTestCoverage
public record UpdateRentPeriodRequest(
    @NotNull @DecimalMin(value = "0.01") BigDecimal rentAmount,
    @NotNull LocalDate effectiveFrom,
    Optional<String> notes,
    Optional<List<@Valid RentComponentRequest>> components) {}
