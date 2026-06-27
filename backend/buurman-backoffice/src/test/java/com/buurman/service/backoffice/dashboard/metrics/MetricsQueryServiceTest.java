package com.buurman.service.backoffice.dashboard.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.OptionalDouble;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.buurman.domain.backoffice.PanelStatus;
import com.buurman.dto.response.backoffice.dashboard.LatencyHeatmapResponse;
import com.buurman.service.backoffice.dashboard.metrics.MetricsQueryService.MetricState;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("MetricsQueryService")
class MetricsQueryServiceTest {

  private final ObjectMapper mapper = new ObjectMapper();
  private PrometheusClient client;
  private MetricsQueryService service;

  @BeforeEach
  void setUp() {
    client = mock(PrometheusClient.class);
    service =
        new MetricsQueryService(
            client, new MetricsProperties(Optional.of("http://prom:9090"), null, null));
  }

  @Nested
  @DisplayName("error rate")
  class ErrorRate {

    @Test
    @DisplayName("UNAVAILABLE when Prometheus is not configured")
    void unavailableWhenUnconfigured() {
      when(client.configured()).thenReturn(false);
      assertThat(service.errorRatePct().state()).isEqualTo(MetricState.UNAVAILABLE);
    }

    @Test
    @DisplayName("LIVE as a percentage of the ratio")
    void liveAsPercentage() {
      when(client.configured()).thenReturn(true);
      when(client.queryScalar(anyString())).thenReturn(OptionalDouble.of(0.02));

      MetricsQueryService.Metric m = service.errorRatePct();

      assertThat(m.state()).isEqualTo(MetricState.LIVE);
      assertThat(m.value()).isCloseTo(2.0, within(1e-9));
    }

    @Test
    @DisplayName("NO_DATA when the result set is empty (no traffic)")
    void noDataWhenEmpty() {
      when(client.configured()).thenReturn(true);
      when(client.queryScalar(anyString())).thenReturn(OptionalDouble.empty());
      assertThat(service.errorRatePct().state()).isEqualTo(MetricState.NO_DATA);
    }

    @Test
    @DisplayName("UNAVAILABLE when the query throws")
    void unavailableOnError() {
      when(client.configured()).thenReturn(true);
      when(client.queryScalar(anyString())).thenThrow(new RuntimeException("boom"));
      assertThat(service.errorRatePct().state()).isEqualTo(MetricState.UNAVAILABLE);
    }
  }

  @Nested
  @DisplayName("p95")
  class P95 {

    @Test
    @DisplayName("LIVE in milliseconds")
    void liveMillis() {
      when(client.configured()).thenReturn(true);
      when(client.queryScalar(anyString())).thenReturn(OptionalDouble.of(123.4));

      MetricsQueryService.Metric m = service.p95Millis();

      assertThat(m.state()).isEqualTo(MetricState.LIVE);
      assertThat(m.value()).isCloseTo(123.4, within(1e-9));
    }
  }

  @Nested
  @DisplayName("latency heatmap")
  class Heatmap {

    @Test
    @DisplayName("PREVIEW with a config CTA when Prometheus is not configured")
    void previewWhenUnconfigured() {
      when(client.configured()).thenReturn(false);

      LatencyHeatmapResponse response = service.latencyHeatmap();

      assertThat(response.status()).isEqualTo(PanelStatus.PREVIEW);
      assertThat(response.previewCta()).contains("Please configure Prometheus");
      assertThat(response.bands()).hasSize(7);
    }

    @Test
    @DisplayName("PREVIEW with a failure CTA when the query throws")
    void previewWhenQueryFails() {
      when(client.configured()).thenReturn(true);
      when(client.queryRange(anyString(), anyLong(), anyLong(), anyLong()))
          .thenThrow(new RuntimeException("boom"));

      LatencyHeatmapResponse response = service.latencyHeatmap();

      assertThat(response.status()).isEqualTo(PanelStatus.PREVIEW);
      assertThat(response.previewCta()).contains("An error occurred");
    }

    @Test
    @DisplayName("LIVE with no columns when configured but there is no traffic")
    void liveButEmpty() throws Exception {
      when(client.configured()).thenReturn(true);
      when(client.queryRange(anyString(), anyLong(), anyLong(), anyLong()))
          .thenReturn(mapper.readTree("[]"));

      LatencyHeatmapResponse response = service.latencyHeatmap();

      assertThat(response.status()).isEqualTo(PanelStatus.LIVE);
      assertThat(response.columns()).isEmpty();
      assertThat(response.bands()).hasSize(7);
    }

    @Test
    @DisplayName("buckets cumulative le series into fixed bands per timestamp")
    void bucketsCumulativeLeSeries() throws Exception {
      lenient().when(client.configured()).thenReturn(true);
      JsonNode matrix =
          mapper.readTree(
              """
              [
                {"metric":{"le":"0.05"},"values":[[1000,"5"]]},
                {"metric":{"le":"0.1"},"values":[[1000,"8"]]},
                {"metric":{"le":"+Inf"},"values":[[1000,"10"]]}
              ]
              """);
      when(client.queryRange(anyString(), anyLong(), anyLong(), anyLong())).thenReturn(matrix);

      LatencyHeatmapResponse response = service.latencyHeatmap();

      assertThat(response.status()).isEqualTo(PanelStatus.LIVE);
      assertThat(response.columns()).hasSize(1);
      // <50ms = 5, 50–100ms = 8−5 = 3, …, >2s (+Inf) = 10−8 = 2
      assertThat(response.columns().get(0).values())
          .containsExactly(5.0, 3.0, 0.0, 0.0, 0.0, 0.0, 2.0);
    }
  }
}
