package com.buurman.service.backoffice;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationState;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.core.env.Environment;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.buurman.config.models.AppProperties;
import com.buurman.dto.response.backoffice.BackofficeSystemInfoResponse;
import com.buurman.dto.response.backoffice.BackofficeSystemInfoResponse.BuildInfo;
import com.buurman.dto.response.backoffice.BackofficeSystemInfoResponse.GcInfo;
import com.buurman.dto.response.backoffice.BackofficeSystemInfoResponse.HttpLatencyStats;
import com.buurman.dto.response.backoffice.BackofficeSystemInfoResponse.MetricEntry;
import com.buurman.dto.response.backoffice.BackofficeSystemInfoResponse.MetricsSnapshot;
import com.buurman.dto.response.backoffice.BackofficeSystemInfoResponse.MigrationEntry;
import com.buurman.dto.response.backoffice.BackofficeSystemInfoResponse.RuntimeInfo;
import com.buurman.dto.response.backoffice.BackofficeSystemInfoResponse.SessionInfo;
import com.buurman.service.KeycloakService;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.search.Search;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BackofficeSystemInfoService {

  private final AppProperties appProperties;
  private final Environment environment;
  private final ObjectProvider<GitProperties> gitPropertiesProvider;
  private final ObjectProvider<BuildProperties> buildPropertiesProvider;
  private final Optional<Flyway> flyway;
  private final KeycloakService keycloakService;
  private final MeterRegistry meterRegistry;
  private final BackofficeHealthCheckService healthCheckService;
  private final BackofficeConfigInspector configInspector;

  public BackofficeSystemInfoService(
      AppProperties appProperties,
      Environment environment,
      ObjectProvider<GitProperties> gitPropertiesProvider,
      ObjectProvider<BuildProperties> buildPropertiesProvider,
      Optional<Flyway> flyway,
      KeycloakService keycloakService,
      MeterRegistry meterRegistry,
      BackofficeHealthCheckService healthCheckService,
      BackofficeConfigInspector configInspector) {
    this.appProperties = appProperties;
    this.environment = environment;
    this.gitPropertiesProvider = gitPropertiesProvider;
    this.buildPropertiesProvider = buildPropertiesProvider;
    this.flyway = flyway;
    this.keycloakService = keycloakService;
    this.meterRegistry = meterRegistry;
    this.healthCheckService = healthCheckService;
    this.configInspector = configInspector;
  }

  @PreAuthorize("hasRole('BACKOFFICE_SYSTEM')")
  public BackofficeSystemInfoResponse getSystemInfo() {
    return new BackofficeSystemInfoResponse(
        getBuildInfo(),
        getRuntimeInfo(),
        getMigrationInfo(),
        healthCheckService.checkAll(),
        configInspector.getConfiguration(),
        getMetricsSnapshot(),
        getSessionInfo());
  }

  // --- Build info ---

  private BuildInfo getBuildInfo() {
    var git = gitPropertiesProvider.getIfAvailable();
    var build = buildPropertiesProvider.getIfAvailable();

    var optGit = Optional.ofNullable(git);
    return new BuildInfo(
        appProperties.version(),
        optGit.map(GitProperties::getShortCommitId),
        optGit.map(g -> g.get("commit.id.full")),
        optGit.map(this::resolveGitBranch),
        optGit.map(GitProperties::getCommitTime),
        git != null && Boolean.parseBoolean(git.get("dirty")),
        Optional.ofNullable(build).map(BuildProperties::getTime));
  }

  private @Nullable String resolveGitBranch(GitProperties git) {
    String branch = git.getBranch();
    // Detached HEAD (e.g. tag checkout in CI) returns the commit SHA as branch
    if (branch != null && branch.matches("[0-9a-f]{40}")) {
      String tag = git.get("closest.tag.name");
      if (tag != null && !tag.isEmpty()) {
        return tag;
      }
    }
    return branch;
  }

  // --- Runtime info ---

  private RuntimeInfo getRuntimeInfo() {
    var runtime = Runtime.getRuntime();
    var activeProfiles = String.join(",", environment.getActiveProfiles());

    MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    var heapUsage = memoryBean.getHeapMemoryUsage();
    var nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

    double cpuUsage = -1;
    var osBean = ManagementFactory.getOperatingSystemMXBean();
    if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOs) {
      cpuUsage = sunOs.getProcessCpuLoad();
    }

    ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

    List<GcInfo> gcInfos =
        ManagementFactory.getGarbageCollectorMXBeans().stream()
            .map(gc -> new GcInfo(gc.getName(), gc.getCollectionCount(), gc.getCollectionTime()))
            .toList();

    return new RuntimeInfo(
        System.getProperty("java.version"),
        SpringBootVersion.getVersion(),
        activeProfiles.isEmpty() ? "default" : activeProfiles,
        ManagementFactory.getRuntimeMXBean().getUptime(),
        heapUsage.getUsed(),
        heapUsage.getMax(),
        nonHeapUsage.getUsed(),
        nonHeapUsage.getMax() > 0 ? nonHeapUsage.getMax() : nonHeapUsage.getCommitted(),
        cpuUsage,
        runtime.availableProcessors(),
        threadBean.getThreadCount(),
        threadBean.getPeakThreadCount(),
        threadBean.getDaemonThreadCount(),
        gcInfos,
        ProcessHandle.current().pid(),
        Instant.now());
  }

  // --- Migrations ---

  private BackofficeSystemInfoResponse.MigrationInfo getMigrationInfo() {
    if (flyway.isEmpty()) {
      return new BackofficeSystemInfoResponse.MigrationInfo(Optional.empty(), 0, 0, 0, List.of());
    }

    var info = flyway.get().info();
    var current = info.current();
    int failedCount =
        (int) Arrays.stream(info.all()).filter(m -> m.getState() == MigrationState.FAILED).count();

    List<MigrationEntry> entries =
        Arrays.stream(info.all())
            .map(
                m ->
                    new MigrationEntry(
                        Optional.ofNullable(
                            m.getVersion() != null ? m.getVersion().getVersion() : null),
                        Optional.ofNullable(m.getDescription()),
                        m.getState().name(),
                        Optional.ofNullable(
                            m.getInstalledOn() != null ? m.getInstalledOn().toInstant() : null),
                        Optional.ofNullable(m.getExecutionTime()),
                        m.getScript()))
            .toList();

    return new BackofficeSystemInfoResponse.MigrationInfo(
        Optional.ofNullable(current != null ? current.getVersion().getVersion() : null),
        info.applied().length,
        info.pending().length,
        failedCount,
        entries);
  }

  // --- Metrics ---

  private MetricsSnapshot getMetricsSnapshot() {
    var timeUnit = TimeUnit.MILLISECONDS;

    long httpCount = 0;
    double httpTotalTimeSec = 0;
    double meanMs = 0;
    double minMs = Double.MAX_VALUE;
    double maxMs = 0;
    double p50Ms = 0;
    double p75Ms = 0;
    double p95Ms = 0;
    double p99Ms = 0;

    var httpTimers = Search.in(meterRegistry).name("http.server.requests").timers();
    List<io.micrometer.core.instrument.distribution.HistogramSnapshot> snapshots =
        new ArrayList<>();

    for (Timer timer : httpTimers) {
      httpCount += timer.count();
      httpTotalTimeSec += timer.totalTime(TimeUnit.SECONDS);

      double timerMax = timer.max(timeUnit);
      if (timerMax > maxMs) {
        maxMs = timerMax;
      }

      double timerMean = timer.mean(timeUnit);
      if (timerMean > 0 && timerMean < minMs) {
        minMs = timerMean;
      }

      snapshots.add(timer.takeSnapshot());
    }

    if (httpCount > 0) {
      meanMs = (httpTotalTimeSec * 1000.0) / httpCount;
      if (minMs == Double.MAX_VALUE) {
        minMs = 0;
      }

      for (var snapshot : snapshots) {
        for (var pv : snapshot.percentileValues()) {
          double ms = pv.value(timeUnit);
          switch ((int) (pv.percentile() * 100)) {
            case 50 -> {
              if (ms > p50Ms) {
                p50Ms = ms;
              }
            }
            case 75 -> {
              if (ms > p75Ms) {
                p75Ms = ms;
              }
            }
            case 95 -> {
              if (ms > p95Ms) {
                p95Ms = ms;
              }
            }
            case 99 -> {
              if (ms > p99Ms) {
                p99Ms = ms;
              }
            }
            default -> {}
          }
        }
      }
    } else {
      minMs = 0;
    }

    var httpLatency = new HttpLatencyStats(meanMs, minMs, maxMs, p50Ms, p75Ms, p95Ms, p99Ms);

    List<MetricEntry> custom = new ArrayList<>();
    for (Meter meter : meterRegistry.getMeters()) {
      String name = meter.getId().getName();
      if (!name.startsWith("buurman.")) {
        continue;
      }

      Map<String, String> tags = new LinkedHashMap<>();
      meter.getId().getTags().forEach(tag -> tags.put(tag.getKey(), tag.getValue()));

      switch (meter) {
        case io.micrometer.core.instrument.Counter c ->
            custom.add(new MetricEntry(name, "COUNTER", c.count(), tags));
        case io.micrometer.core.instrument.Gauge g ->
            custom.add(new MetricEntry(name, "GAUGE", g.value(), tags));
        case Timer t -> custom.add(new MetricEntry(name, "TIMER", t.count(), tags));
        case io.micrometer.core.instrument.DistributionSummary ds ->
            custom.add(new MetricEntry(name, "DISTRIBUTION", ds.count(), tags));
        default -> {}
      }
    }

    return new MetricsSnapshot(httpCount, httpTotalTimeSec, Optional.of(httpLatency), custom);
  }

  // --- Sessions ---

  private SessionInfo getSessionInfo() {
    try {
      var counts = keycloakService.getActiveSessionCounts();
      return new SessionInfo(counts.getOrDefault("app", 0), counts.getOrDefault("backoffice", 0));
    } catch (Exception e) {
      log.warn("Failed to fetch session counts from Keycloak: {}", e.getMessage());
      return new SessionInfo(0, 0);
    }
  }
}
