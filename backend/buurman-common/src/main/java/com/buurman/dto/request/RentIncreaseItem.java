package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.NotNull;

@SkipTestCoverage
public record RentIncreaseItem(
    @NotNull ContractIdentifier contractIdentifier,
    @NotNull BigDecimal increasePercentage,
    @NotNull LocalDate effectiveDate) {}
