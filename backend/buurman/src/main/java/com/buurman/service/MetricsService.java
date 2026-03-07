package com.buurman.service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Service
@RequiredArgsConstructor
public class MetricsService {

  private static final String PREFIX = "buurman.";

  private final MeterRegistry registry;

  public void incrementCounter(String name, String... tags) {
    Counter.builder(PREFIX + name).tags(tags).register(registry).increment();
  }

  public void recordTimer(String name, Duration duration, String... tags) {
    Timer.builder(PREFIX + name).tags(tags).register(registry).record(duration);
  }

  public <T> T recordTimer(String name, Supplier<T> supplier, String... tags) {
    return Timer.builder(PREFIX + name).tags(tags).register(registry).record(supplier);
  }

  public <T> T recordTimerCallable(String name, Callable<T> callable, String... tags)
      throws Exception {
    return Timer.builder(PREFIX + name).tags(tags).register(registry).recordCallable(callable);
  }

  public void recordHistogram(String name, double value, String... tags) {
    DistributionSummary.builder(PREFIX + name).tags(tags).register(registry).record(value);
  }

  public void registerGauge(String name, Number number, String... tags) {
    io.micrometer.core.instrument.Gauge.builder(PREFIX + name, number, Number::doubleValue)
        .tags(tags)
        .register(registry);
  }

  public <T> void registerGauge(
      String name, T obj, java.util.function.ToDoubleFunction<T> valueFunction, String... tags) {
    io.micrometer.core.instrument.Gauge.builder(PREFIX + name, obj, valueFunction)
        .tags(tags)
        .register(registry);
  }

  public void recordNotificationSend(
      Instant start, String channel, String provider, String result) {
    Duration duration = Duration.between(start, Instant.now());
    recordTimer(
        "notification.send.seconds",
        duration,
        "channel",
        channel,
        "provider",
        provider,
        "result",
        result);
    incrementCounter(
        "notification.send.total", "channel", channel, "provider", provider, "result", result);
  }
}
