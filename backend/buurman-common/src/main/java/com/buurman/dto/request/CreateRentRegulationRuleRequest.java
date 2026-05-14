package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.MaxIncreaseType;
import com.buurman.domain.RentFrequency;
import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@SkipTestCoverage
public record CreateRentRegulationRuleRequest(
    @Min(1900) int year,
    @NotBlank String propertyCategory,
    Optional<BigDecimal> maxIncreasePercentage,
    @NotNull MaxIncreaseType maxIncreaseType,
    Optional<String> indexName,
    Optional<BigDecimal> indexValue,
    Optional<LocalDate> effectiveDate,
    Optional<Integer> noticePeriodDays,
    Optional<RentFrequency> frequency,
    Optional<String> additionalConditions,
    Optional<String> sourceUrl,
    Optional<String> notes,
    // BUUR-93 dimensional fields
    Optional<String> regime,
    Optional<String> propertyType,
    Optional<String> contractType,
    Optional<String> taxRegime,
    Optional<String> tenancyPhase,
    Optional<Integer> buildYearMin,
    Optional<Integer> buildYearMax,
    Optional<String> epcClassMin,
    Optional<String> epcClassMax,
    Optional<LocalDate> contractSignedAfter,
    Optional<LocalDate> contractSignedBefore,
    Optional<Integer> landlordMinProperties,
    Optional<String> areaCode) {}
