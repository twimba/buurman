package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.buurman.domain.identifier.ContractIdentifier;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record RentIncreaseItem(
    @NotNull ContractIdentifier contractIdentifier,
    @NotNull @DecimalMin(value = "0.01") BigDecimal increasePercentage,
    @NotNull LocalDate effectiveDate) {}
