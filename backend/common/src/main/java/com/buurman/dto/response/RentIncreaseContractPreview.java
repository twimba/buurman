package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record RentIncreaseContractPreview(
    ContractIdentifier contractIdentifier,
    PropertyIdentifier propertyIdentifier,
    String propertyName,
    String propertyAddress,
    String propertyCountryCode,
    Optional<String> propertyRegionCode,
    BigDecimal currentRentAmount,
    String currency,
    Optional<BigDecimal> regulationMinPercent,
    Optional<BigDecimal> regulationMaxPercent,
    Optional<LocalDate> suggestedEffectiveDate) {}
