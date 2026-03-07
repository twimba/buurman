package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.MaxIncreaseType;
import com.buurman.domain.RentFrequency;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateRentRegulationRuleRequest(
    @Min(1900) int year,
    @NotBlank String propertyCategory,
    Optional<String> sector,
    Optional<BigDecimal> maxIncreasePercentage,
    @NotNull MaxIncreaseType maxIncreaseType,
    Optional<String> indexName,
    Optional<BigDecimal> indexValue,
    Optional<LocalDate> effectiveDate,
    Optional<Integer> noticePeriodDays,
    Optional<RentFrequency> frequency,
    Optional<String> additionalConditions,
    Optional<String> sourceUrl,
    Optional<String> notes) {}
