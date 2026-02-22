package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record CreateRentPeriodRequest(
    @NotNull @DecimalMin(value = "0.01") BigDecimal rentAmount,
    @NotNull LocalDate effectiveFrom,
    @Nullable String notes) {}
