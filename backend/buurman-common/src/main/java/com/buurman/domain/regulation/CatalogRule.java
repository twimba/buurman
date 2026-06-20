package com.buurman.domain.regulation;

import java.math.BigDecimal;

import com.buurman.domain.MaxIncreaseType;
import com.buurman.domain.RentFrequency;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A single rent-regulation rule within a {@link CatalogCountry}.
 *
 * <p>{@code regionCode} links the rule to a {@link CatalogRegion} of the same country; a {@code
 * null} value denotes a national rule. The typed dimensional fields (regime, propertyType, …) use
 * {@code null} as a wildcard meaning "applies to all".
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CatalogRule(
    String regionCode,
    int year,
    String propertyCategory,
    BigDecimal maxIncreasePercentage,
    MaxIncreaseType maxIncreaseType,
    String indexName,
    BigDecimal indexValue,
    String effectiveDate,
    Integer noticePeriodDays,
    RentFrequency frequency,
    String additionalConditions,
    String sourceUrl,
    String notes,
    String regime,
    String propertyType,
    String contractType,
    String taxRegime,
    String tenancyPhase,
    Integer buildYearMin,
    Integer buildYearMax,
    String epcClassMin,
    String epcClassMax,
    String contractSignedAfter,
    String contractSignedBefore,
    Integer landlordMinProperties,
    String areaCode) {}
