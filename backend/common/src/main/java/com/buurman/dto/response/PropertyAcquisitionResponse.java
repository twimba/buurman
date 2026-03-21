package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.PropertyAcquisition;
import com.buurman.domain.Sid;
import com.buurman.util.Generated;

@Generated
public record PropertyAcquisitionResponse(
    Sid identifier,
    PropertyAcquisition.AcquisitionType acquisitionType,
    Optional<LocalDate> acquisitionDate,
    Optional<BigDecimal> purchasePrice,
    Optional<String> purchasePriceCurrency,
    Optional<BigDecimal> closingCosts,
    Optional<String> closingCostsCurrency,
    Optional<BigDecimal> renovationCosts,
    Optional<String> renovationCostsCurrency,
    Optional<BigDecimal> landValue,
    Optional<String> landValueCurrency,
    Optional<PropertyAcquisition.DepreciationMethod> depreciationMethod,
    Optional<Integer> depreciationYears,
    Optional<String> notes,
    Instant createdAt,
    Instant updatedAt) {}
