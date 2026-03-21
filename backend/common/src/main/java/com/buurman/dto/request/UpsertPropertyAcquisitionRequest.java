package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.PropertyAcquisition;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import com.buurman.util.Generated;

@Generated
public record UpsertPropertyAcquisitionRequest(
    @NotNull(message = "Acquisition type is required") PropertyAcquisition.AcquisitionType acquisitionType,
    Optional<LocalDate> acquisitionDate,
    Optional<@Positive(message = "Purchase price must be positive") BigDecimal> purchasePrice,
    Optional<String> purchasePriceCurrency,
    Optional<@PositiveOrZero(message = "Closing costs must be zero or positive") BigDecimal>
        closingCosts,
    Optional<String> closingCostsCurrency,
    Optional<@PositiveOrZero(message = "Renovation costs must be zero or positive") BigDecimal>
        renovationCosts,
    Optional<String> renovationCostsCurrency,
    Optional<@PositiveOrZero(message = "Land value must be zero or positive") BigDecimal> landValue,
    Optional<String> landValueCurrency,
    Optional<PropertyAcquisition.DepreciationMethod> depreciationMethod,
    Optional<@Positive(message = "Depreciation years must be positive") Integer> depreciationYears,
    Optional<String> notes) {}
