package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.Sid;
import com.buurman.domain.UnitStatus;
import com.buurman.domain.UnitType;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record UnitResponse(
    Sid identifier,
    Sid propertyIdentifier,
    String unitNumber,
    Optional<String> name,
    Optional<Integer> floor,
    UnitType unitType,
    UnitStatus status,
    boolean implicit,
    int sortOrder,
    Optional<BigDecimal> areaValue,
    Optional<String> areaUnit,
    Optional<BigDecimal> wozValue,
    Optional<String> wozValueCurrency,
    Optional<BigDecimal> wozSharePct,
    Optional<BigDecimal> allocationShare,
    Optional<String> energyEfficiencyRating,
    Optional<LocalDate> energyCertificateExpiryDate,
    Optional<String> heatingType,
    Optional<String> coolingType,
    Optional<String> hotWaterSystem,
    Optional<String> insulationNotes,
    Optional<String> flooringType,
    Optional<String> windowType,
    Optional<Boolean> hasSmokeDetectors,
    Optional<Boolean> hasCoDetectors,
    Optional<Boolean> hasFireExtinguisher,
    Optional<Boolean> hasAdaptedBathroom,
    Optional<String> accessibilityNotes,
    Instant createdAt,
    Optional<Instant> updatedAt) {}
