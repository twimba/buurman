package com.buurman.service.backoffice.dashboard.metrics;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.OptionalDouble;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Thin client over the Prometheus HTTP API. All queries aggregate across nodes (the caller uses
 * {@code sum(...) by (le)} etc.), so results are cluster-wide and correct on multi-node setups.
 *
 * <p>When Prometheus is not configured the client is inert ({@link #configured()} is false).
 * Transport/HTTP errors propagate as {@link RuntimeException} so callers can fall back to PREVIEW.
 */
@Component
@Slf4j
public class PrometheusClient {

  // Circuit breaker: after a transport failure, short-circuit calls for a cooldown so a degraded
  // Prometheus can't pin a servlet thread for the full ~6s timeout on every dashboard poll.
  private static final long BREAKER_COOLDOWN_MS = 30_000;
  private final AtomicLong breakerOpenUntil = new AtomicLong(0);

  private final RestClient client;
  private final ObjectMapper objectMapper;

  public PrometheusClient(MetricsProperties props, ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.client = props.configured() ? build(props.prometheusUrl().orElseThrow()) : null;
    if (this.client == null) {
      log.info(
          "Prometheus URL not configured — dashboard latency/error-rate panels run in PREVIEW");
    }
  }

  public boolean configured() {
    return client != null;
  }

  /** Instant query returning a single scalar; empty when the result set is empty or NaN. */
  public OptionalDouble queryScalar(String promql) {
    String body =
        guarded(
            () ->
                client
                    .get()
                    // Pass PromQL as a URI variable so its {label="…"} braces are encoded as a
                    // value, not parsed as URI template placeholders.
                    .uri(b -> b.path("/api/v1/query").queryParam("query", "{q}").build(promql))
                    .retrieve()
                    .body(String.class));
    JsonNode root = readTree(body);
    JsonNode result = root == null ? null : root.path("data").path("result");
    if (result == null || !result.isArray() || result.isEmpty()) {
      return OptionalDouble.empty();
    }
    String value = result.get(0).path("value").path(1).asText("");
    return parse(value);
  }

  /** Range query returning the raw Prometheus matrix JSON ({@code data.result}). */
  public JsonNode queryRange(String promql, long startEpoch, long endEpoch, long stepSeconds) {
    String body =
        guarded(
            () ->
                client
                    .get()
                    .uri(
                        b ->
                            b.path("/api/v1/query_range")
                                .queryParam("query", "{q}")
                                .queryParam("start", startEpoch)
                                .queryParam("end", endEpoch)
                                .queryParam("step", stepSeconds + "s")
                                .build(promql))
                    .retrieve()
                    .body(String.class));
    JsonNode root = readTree(body);
    return root == null ? null : root.path("data").path("result");
  }

  /**
   * Run an HTTP call behind the circuit breaker: fail fast while the breaker is open, and trip it
   * (opening it for the cooldown) on any failure so repeated polls don't each eat the full timeout.
   */
  private <T> T guarded(Supplier<T> call) {
    long openUntil = breakerOpenUntil.get();
    if (System.currentTimeMillis() < openUntil) {
      throw new IllegalStateException("Prometheus circuit open (recent failure); skipping query");
    }
    try {
      T result = call.get();
      breakerOpenUntil.set(0);
      return result;
    } catch (RuntimeException e) {
      breakerOpenUntil.set(System.currentTimeMillis() + BREAKER_COOLDOWN_MS);
      throw e;
    }
  }

  // Parse with the app's Jackson 2 ObjectMapper — Boot 4's RestClient converter is Jackson 3 and
  // can't bind to a Jackson 2 JsonNode directly.
  private JsonNode readTree(String body) {
    if (body == null || body.isBlank()) {
      return null;
    }
    try {
      return objectMapper.readTree(body);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Invalid Prometheus response", e);
    }
  }

  private static OptionalDouble parse(String value) {
    if (value.isBlank() || value.equals("NaN") || value.equals("+Inf") || value.equals("-Inf")) {
      return OptionalDouble.empty();
    }
    try {
      return OptionalDouble.of(Double.parseDouble(value));
    } catch (NumberFormatException e) {
      return OptionalDouble.empty();
    }
  }

  private static RestClient build(String baseUrl) {
    HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
    factory.setReadTimeout(Duration.ofSeconds(4));
    return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
  }
}
