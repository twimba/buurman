package com.buurman.service.backoffice.dashboard.metrics;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.TreeMap;

import org.springframework.stereotype.Service;

import com.buurman.domain.backoffice.PanelStatus;
import com.buurman.dto.response.backoffice.dashboard.LatencyHeatmapResponse;
import com.buurman.dto.response.backoffice.dashboard.LatencyHeatmapResponse.HeatColumn;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Cluster-wide HTTP metrics from Prometheus. All queries aggregate across nodes, so values reflect
 * the whole fleet, not the node that happened to serve this request. Any failure (unconfigured,
 * unreachable, query error) degrades to {@link MetricState#UNAVAILABLE} / PREVIEW.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MetricsQueryService {

  /** Latency band upper bounds in seconds (last is the +Inf overflow). */
  private static final double[] BAND_BOUNDS = {
    0.05, 0.1, 0.25, 0.5, 1, 2, Double.POSITIVE_INFINITY
  };

  private static final String[] BAND_LABELS = {
    "<50ms", "50–100ms", "100–250ms", "250–500ms", "500ms–1s", "1–2s", ">2s"
  };

  private static final DateTimeFormatter HHMM =
      DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

  public enum MetricState {
    LIVE,
    NO_DATA,
    UNAVAILABLE
  }

  public record Metric(MetricState state, double value) {
    static Metric unavailable() {
      return new Metric(MetricState.UNAVAILABLE, 0);
    }
  }

  private final PrometheusClient client;
  private final MetricsProperties props;

  public boolean configured() {
    return client.configured();
  }

  /** Server-error ratio (5xx / total) over the window, as a percentage. */
  public Metric errorRatePct() {
    if (!client.configured()) {
      return Metric.unavailable();
    }
    String sel = props.selector();
    String w = props.window();
    String q =
        "sum(rate(http_server_requests_seconds_count{"
            + labels(sel, "status=~\"5..\"")
            + "}["
            + w
            + "])) / sum(rate(http_server_requests_seconds_count{"
            + labels(sel)
            + "}["
            + w
            + "]))";
    return scalar(q, 100.0);
  }

  /** 95th-percentile request latency over the window, in milliseconds. */
  public Metric p95Millis() {
    if (!client.configured()) {
      return Metric.unavailable();
    }
    String sel = props.selector();
    String w = props.window();
    String q =
        "histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{"
            + labels(sel)
            + "}["
            + w
            + "])) by (le)) * 1000";
    return scalar(q, 1.0);
  }

  private Metric scalar(String promql, double factor) {
    try {
      OptionalDouble v = client.queryScalar(promql);
      return v.isPresent()
          ? new Metric(MetricState.LIVE, v.getAsDouble() * factor)
          : new Metric(MetricState.NO_DATA, 0);
    } catch (RuntimeException e) {
      log.warn("Prometheus query failed: {}", e.getMessage());
      return Metric.unavailable();
    }
  }

  /** Latency density heatmap (time × latency band) over the last 3 hours. */
  public LatencyHeatmapResponse latencyHeatmap() {
    if (!client.configured()) {
      return preview("Please configure Prometheus");
    }
    try {
      Instant end = Instant.now();
      Instant start = end.minus(java.time.Duration.ofHours(3));
      long step = 600; // 10 minutes
      String q =
          "sum(rate(http_server_requests_seconds_bucket{"
              + labels(props.selector())
              + "}[5m])) by (le)";
      JsonNode result = client.queryRange(q, start.getEpochSecond(), end.getEpochSecond(), step);
      return buildHeatmap(result);
    } catch (RuntimeException e) {
      log.warn("Prometheus latency-heatmap query failed", e);
      return preview("An error occurred");
    }
  }

  /**
   * Converts Prometheus cumulative {@code le} bucket series into a fixed-band density matrix. For
   * each timestamp the per-bucket increment (bucket[le] − bucket[prevLe]) is assigned to the target
   * band whose upper bound contains {@code le}, so the chart height stays constant regardless of
   * how many histogram buckets Micrometer emits.
   */
  private LatencyHeatmapResponse buildHeatmap(JsonNode result) {
    if (result == null || !result.isArray() || result.isEmpty()) {
      // Configured and reachable, but no traffic in window.
      return new LatencyHeatmapResponse(
          PanelStatus.LIVE, Optional.empty(), Optional.empty(), List.of(BAND_LABELS), List.of());
    }

    // le -> (epochSecond -> cumulative value)
    TreeMap<Double, TreeMap<Long, Double>> byLe = new TreeMap<>();
    TreeMap<Long, double[]> columns = new TreeMap<>();
    for (JsonNode series : result) {
      double le = parseLe(series.path("metric").path("le").asText("+Inf"));
      TreeMap<Long, Double> points = byLe.computeIfAbsent(le, k -> new TreeMap<>());
      for (JsonNode pair : series.path("values")) {
        long ts = pair.get(0).asLong();
        double val = parseDouble(pair.get(1).asText("0"));
        points.put(ts, val);
        columns.computeIfAbsent(ts, k -> new double[BAND_LABELS.length]);
      }
    }

    for (Long ts : columns.keySet()) {
      double[] bands = columns.get(ts);
      double prev = 0;
      for (var entry : byLe.entrySet()) {
        double le = entry.getKey();
        double cumulative = entry.getValue().getOrDefault(ts, prev);
        double increment = Math.max(0, cumulative - prev);
        prev = cumulative;
        bands[bandIndex(le)] += increment;
      }
    }

    List<HeatColumn> cols = new ArrayList<>();
    for (var entry : columns.entrySet()) {
      List<Double> values = new ArrayList<>(BAND_LABELS.length);
      for (double v : entry.getValue()) {
        values.add(Math.round(v * 100.0) / 100.0);
      }
      cols.add(new HeatColumn(HHMM.format(Instant.ofEpochSecond(entry.getKey())), values));
    }
    return new LatencyHeatmapResponse(
        PanelStatus.LIVE, Optional.empty(), Optional.empty(), List.of(BAND_LABELS), cols);
  }

  private static int bandIndex(double le) {
    for (int i = 0; i < BAND_BOUNDS.length; i++) {
      if (le <= BAND_BOUNDS[i]) {
        return i;
      }
    }
    return BAND_LABELS.length - 1;
  }

  private static double parseLe(String le) {
    return le.equals("+Inf") ? Double.POSITIVE_INFINITY : parseDouble(le);
  }

  private static double parseDouble(String s) {
    try {
      return Double.parseDouble(s);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private static String labels(String... matchers) {
    return String.join(
        ",", java.util.Arrays.stream(matchers).filter(m -> m != null && !m.isBlank()).toList());
  }

  private LatencyHeatmapResponse preview(String cta) {
    return new LatencyHeatmapResponse(
        PanelStatus.PREVIEW, Optional.of(cta), Optional.empty(), List.of(BAND_LABELS), List.of());
  }
}
