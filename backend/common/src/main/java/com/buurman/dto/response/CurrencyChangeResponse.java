package com.buurman.dto.response;

import java.math.BigDecimal;
import java.util.Optional;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record CurrencyChangeResponse(
    String oldCurrency,
    String newCurrency,
    String mode,
    Optional<BigDecimal> conversionRate,
    int affectedContracts,
    int affectedPayments,
    int affectedExpenses,
    int affectedFinancials) {}
