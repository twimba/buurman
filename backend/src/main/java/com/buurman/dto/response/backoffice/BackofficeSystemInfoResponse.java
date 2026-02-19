package com.buurman.dto.response.backoffice;

import java.time.Instant;
import java.util.List;
import java.util.Map;

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
      String gitCommit,
      String gitCommitFull,
      String gitBranch,
      Instant gitCommitTime,
      boolean gitDirty,
      Instant buildTime) {}

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
      String currentVersion,
      int appliedCount,
      int pendingCount,
      int failedCount,
      List<MigrationEntry> entries) {}

  public record MigrationEntry(
      String version,
      String description,
      String state,
      Instant installedOn,
      Integer executionTimeMs,
      String script) {}

  public record ServiceHealth(
      String name, Status status, Long latencyMs, String details, String error) {

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
      HttpLatencyStats httpLatency,
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
