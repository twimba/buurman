package com.buurman.service.backoffice.dashboard;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.buurman.dto.response.backoffice.dashboard.LatencyHeatmapResponse;
import com.buurman.service.backoffice.dashboard.metrics.MetricsQueryService;

import lombok.RequiredArgsConstructor;

/**
 * Latency heatmap panel — cluster-wide request-latency density from Prometheus (PREVIEW if absent).
 */
@Service
@RequiredArgsConstructor
public class LatencyHeatmapService {

  private final MetricsQueryService metricsQueryService;

  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public LatencyHeatmapResponse getLatencyHeatmap() {
    return metricsQueryService.latencyHeatmap();
  }
}
