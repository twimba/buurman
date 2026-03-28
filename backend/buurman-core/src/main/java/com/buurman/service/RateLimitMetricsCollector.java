package com.buurman.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.jooq.DSLContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RateLimitMetricsCollector {

  private final DSLContext dsl;
  private final MeterRegistry registry;
  private final Map<String, AtomicLong> gauges = new ConcurrentHashMap<>();

  public RateLimitMetricsCollector(DSLContext dsl, MeterRegistry registry) {
    this.dsl = dsl;
    this.registry = registry;
  }

  @Scheduled(fixedRate = 60_000)
  public void collectActiveBuckets() {
    try {
      var results =
          dsl.fetch(
              """
              SELECT split_part(id, ':', 1) AS config_key, COUNT(*) AS bucket_count
              FROM rate_limit_buckets
              WHERE expires_at > extract(epoch FROM now()) * 1000
              GROUP BY split_part(id, ':', 1)
              """);

      for (var record : results) {
        String configKey = record.get("config_key", String.class);
        long count = record.get("bucket_count", Long.class);
        if (configKey != null) {
          gauges
              .computeIfAbsent(
                  configKey,
                  k -> {
                    AtomicLong gauge = new AtomicLong(0);
                    Gauge.builder(
                            "buurman.ratelimit.active_buckets", gauge, AtomicLong::doubleValue)
                        .tag("endpoint", k)
                        .register(registry);
                    return gauge;
                  })
              .set(count);
        }
      }
    } catch (Exception e) {
      log.warn("Failed to collect rate limit active bucket metrics: {}", e.getMessage());
    }
  }

  /** Get the current active bucket counts (used by backoffice summary). */
  public Map<String, Long> getActiveBucketCounts() {
    Map<String, Long> counts = new ConcurrentHashMap<>();
    gauges.forEach((k, v) -> counts.put(k, v.get()));
    return counts;
  }
}
