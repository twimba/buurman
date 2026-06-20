package com.buurman.service.backoffice.dashboard.metrics;

import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.buurman.util.SkipTestCoverage;

/**
 * Configuration for the Prometheus-backed dashboard metrics (error rate, p95, latency heatmap).
 * When {@code prometheusUrl} is blank the panels degrade to PREVIEW.
 */
@ConfigurationProperties(prefix = "backoffice.dashboard.metrics")
@SkipTestCoverage
public record MetricsProperties(Optional<String> prometheusUrl, String selector, String window) {

  public MetricsProperties {
    prometheusUrl = Optional.ofNullable(prometheusUrl).flatMap(o -> o).filter(s -> !s.isBlank());
    if (selector == null) {
      selector = "application=\"buurman\"";
    }
    if (window == null || window.isBlank()) {
      window = "5m";
    }
  }

  public boolean configured() {
    return prometheusUrl.isPresent();
  }
}
