package com.buurman.dto.response.backoffice.cost;

import java.util.List;
import java.util.Optional;

import com.buurman.domain.backoffice.PanelStatus;
import com.buurman.util.SkipTestCoverage;

/** Full cost overview for the dedicated Costs page. All money is EUR minor units (cents). */
@SkipTestCoverage
public record CostOverviewResponse(
    PanelStatus status,
    Optional<String> asOf,
    String baseCurrency,
    long totalMonthlyEurMinor,
    Optional<Double> momChangePct,
    List<ProviderCost> providers,
    List<CostTrendPoint> trend,
    List<CostInsight> insights) {}
