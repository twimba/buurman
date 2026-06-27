package com.buurman.dto.response.backoffice.cost;

import java.util.List;
import java.util.Optional;

import com.buurman.domain.backoffice.CostSourceType;
import com.buurman.util.SkipTestCoverage;

/**
 * One provider's latest monthly cost, normalized to EUR (minor units = cents). {@code breakdown}
 * lists the components behind the figure (e.g. Mailgun's plan fee + accepted-email volume); empty
 * when the provider exposes no breakdown.
 */
@SkipTestCoverage
public record ProviderCost(
    String provider,
    String displayName,
    CostSourceType sourceType,
    String currency,
    long amountMinor,
    long amountEurMinor,
    boolean available,
    Optional<String> note,
    List<CostLineItem> breakdown) {}
