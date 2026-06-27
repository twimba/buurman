package com.buurman.dto.response.backoffice.cost;

import java.util.List;
import java.util.Optional;

import com.buurman.domain.backoffice.PanelStatus;
import com.buurman.util.SkipTestCoverage;

/** Compact cost summary for the mission-control "Cost watch" panel. EUR minor units (cents). */
@SkipTestCoverage
public record CostWatchResponse(
    PanelStatus status,
    Optional<String> previewCta,
    Optional<String> docsLink,
    long totalMonthlyEurMinor,
    Optional<Double> momChangePct,
    List<ProviderCost> topProviders,
    Optional<CostInsight> headline,
    Optional<String> asOf) {}
