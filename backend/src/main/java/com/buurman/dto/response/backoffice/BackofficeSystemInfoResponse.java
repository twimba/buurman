package com.buurman.dto.response.backoffice;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record BackofficeSystemInfoResponse(
    BuildInfo build,
    RuntimeInfo runtime,
    MigrationInfo migrations,
    List<ServiceHealth> services,
    List<ConfigEntry> configuration,
    MetricsSnapshot metrics,
    SessionInfo sessions) {

  public record BuildInfo(
      String version,
      Optional<String> gitCommit,
      Optional<String> gitCommitFull,
      Optional<String> gitBranch,
      Optional<Instant> gitCommitTime,
      boolean gitDirty,
      Optional<Instant> buildTime) {}

  public record RuntimeInfo(
      String javaVersion,
      String springBootVersion,
      String activeProfiles,
      long uptimeMs,
      long heapUsedBytes,
      long heapMaxBytes,
      long nonHeapUsedBytes,
      long nonHeapMaxBytes,
      double cpuUsage,
      int availableProcessors,
      int threadCount,
      int peakThreadCount,
      int daemonThreadCount,
      List<GcInfo> garbageCollectors,
      long pid,
      Instant serverTime) {}

  public record GcInfo(String name, long collectionCount, long collectionTimeMs) {}

  public record MigrationInfo(
      Optional<String> currentVersion,
      int appliedCount,
      int pendingCount,
      int failedCount,
      List<MigrationEntry> entries) {}

  public record MigrationEntry(
      Optional<String> version,
      Optional<String> description,
      String state,
      Optional<Instant> installedOn,
      Optional<Integer> executionTimeMs,
      String script) {}

  public record ServiceHealth(
      String name,
      Status status,
      Optional<Long> latencyMs,
      Optional<String> details,
      Optional<String> error) {

    public enum Status {
      UP,
      DOWN,
      DISABLED,
      UNKNOWN
    }
  }

  public record ConfigEntry(String category, String key, String value) {}

  public record MetricsSnapshot(
      long httpRequestCount,
      double httpRequestTotalTimeSeconds,
      Optional<HttpLatencyStats> httpLatency,
      List<MetricEntry> custom) {}

  public record HttpLatencyStats(
      double meanMs,
      double minMs,
      double maxMs,
      double p50Ms,
      double p75Ms,
      double p95Ms,
      double p99Ms) {}

  public record MetricEntry(String name, String type, double value, Map<String, String> tags) {}

  public record SessionInfo(int appActiveUsers, int backofficeActiveUsers) {}
}
