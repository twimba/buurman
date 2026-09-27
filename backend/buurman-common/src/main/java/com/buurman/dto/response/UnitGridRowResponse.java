package com.buurman.dto.response;

import java.math.BigDecimal;
import java.util.Optional;

import com.buurman.domain.Sid;
import com.buurman.domain.UnitStatus;
import com.buurman.domain.UnitType;
import com.buurman.util.SkipTestCoverage;

/**
 * A single row in the units grid view, joining unit identity with the current occupancy snapshot
 * (active tenant, rent, vacancy). Assembled by the service layer from {@code Unit} plus contract
 * data — not produced by {@code UnitMapper} directly.
 */
@SkipTestCoverage
public record UnitGridRowResponse(
    Sid identifier,
    String unitNumber,
    Optional<String> name,
    UnitType unitType,
    UnitStatus status,
    Optional<String> tenantName,
    Optional<BigDecimal> monthlyRent,
    Optional<String> monthlyRentCurrency,
    Optional<Integer> vacancyDays) {}
