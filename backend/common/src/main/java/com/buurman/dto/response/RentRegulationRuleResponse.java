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
    Optional<String> sector,
    Optional<BigDecimal> maxIncreasePercentage,
    MaxIncreaseType maxIncreaseType,
    Optional<String> indexName,
    Optional<BigDecimal> indexValue,
    Optional<LocalDate> effectiveDate,
    Optional<Integer> noticePeriodDays,
    RentFrequency frequency,
    Optional<String> additionalConditions,
    Optional<String> sourceUrl,
    Optional<String> notes) {}
