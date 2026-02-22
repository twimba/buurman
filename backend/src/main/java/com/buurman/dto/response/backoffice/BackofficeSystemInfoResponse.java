package com.buurman.dto.response.backoffice;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

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
      @Nullable String gitCommit,
      @Nullable String gitCommitFull,
      @Nullable String gitBranch,
      @Nullable Instant gitCommitTime,
      boolean gitDirty,
      @Nullable Instant buildTime) {}

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
      @Nullable String currentVersion,
      int appliedCount,
      int pendingCount,
      int failedCount,
      List<MigrationEntry> entries) {}

  public record MigrationEntry(
      @Nullable String version,
      @Nullable String description,
      String state,
      @Nullable Instant installedOn,
      @Nullable Integer executionTimeMs,
      String script) {}

  public record ServiceHealth(
      String name,
      Status status,
      @Nullable Long latencyMs,
      @Nullable String details,
      @Nullable String error) {

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
      @Nullable HttpLatencyStats httpLatency,
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
