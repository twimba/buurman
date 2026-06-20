package com.buurman.dto.response.backoffice.cost;

import java.util.Optional;

import com.buurman.domain.backoffice.CostSourceType;
import com.buurman.util.SkipTestCoverage;

/** One provider's latest monthly cost, normalized to EUR (minor units = cents). */
@SkipTestCoverage
public record ProviderCost(
    String provider,
    String displayName,
    CostSourceType sourceType,
    String currency,
    long amountMinor,
    long amountEurMinor,
    boolean available,
    Optional<String> note) {}
