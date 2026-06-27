package com.buurman.dto.response.backoffice.dashboard;

import java.util.List;
import java.util.Optional;

import com.buurman.domain.backoffice.PanelStatus;
import com.buurman.util.SkipTestCoverage;

/**
 * Request-latency density over time, aggregated across all nodes from Prometheus histogram buckets.
 * {@code bands} are latency ranges (low → high); each column is one time step with per-band request
 * rates aligned to {@code bands}.
 */
@SkipTestCoverage
public record LatencyHeatmapResponse(
    PanelStatus status,
    Optional<String> previewCta,
    Optional<String> docsLink,
    List<String> bands,
    List<HeatColumn> columns) {

  @SkipTestCoverage
  public record HeatColumn(String time, List<Double> values) {}
}
