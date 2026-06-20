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

  private static final String DEFAULT_SELECTOR = "application=\"buurman\"";
  // A comma-separated list of `label(=|=~)"value"` matchers — no braces/backslashes so the value
  // can't break out of the PromQL `{...}` it's concatenated into. Invalid input falls back safe.
  private static final java.util.regex.Pattern SELECTOR =
      java.util.regex.Pattern.compile(
          "[A-Za-z_][A-Za-z0-9_]*=~?\"[^\"{}\\\\]*\"(,[A-Za-z_][A-Za-z0-9_]*=~?\"[^\"{}\\\\]*\")*");
  private static final java.util.regex.Pattern WINDOW =
      java.util.regex.Pattern.compile("\\d+[smhdwy]");

  public MetricsProperties {
    prometheusUrl = Optional.ofNullable(prometheusUrl).flatMap(o -> o).filter(s -> !s.isBlank());
    if (selector == null || !SELECTOR.matcher(selector).matches()) {
      selector = DEFAULT_SELECTOR;
    }
    if (window == null || !WINDOW.matcher(window).matches()) {
      window = "5m";
    }
  }

  public boolean configured() {
    return prometheusUrl.isPresent();
  }
}
