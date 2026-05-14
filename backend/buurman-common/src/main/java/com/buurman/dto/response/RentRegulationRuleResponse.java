package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.MaxIncreaseType;
import com.buurman.domain.RentFrequency;
import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record RentRegulationRuleResponse(
    Sid identifier,
    int year,
    String propertyCategory,
    Optional<BigDecimal> maxIncreasePercentage,
    MaxIncreaseType maxIncreaseType,
    Optional<String> indexName,
    Optional<BigDecimal> indexValue,
    Optional<LocalDate> effectiveDate,
    Optional<Integer> noticePeriodDays,
    RentFrequency frequency,
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
