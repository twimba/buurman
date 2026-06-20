package com.buurman.service.backoffice.dashboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.backoffice.PanelStatus;
import com.buurman.dto.response.backoffice.dashboard.StatusStripResponse;
import com.buurman.dto.response.backoffice.dashboard.StatusStripResponse.Pillar;
import com.buurman.repository.backoffice.DashboardAggregateRepository;
import com.buurman.security.SecurityUtils;
import com.buurman.service.backoffice.dashboard.metrics.MetricsQueryService;
import com.buurman.service.backoffice.dashboard.metrics.MetricsQueryService.Metric;

import lombok.RequiredArgsConstructor;

/**
 * The pinned 8-pillar status strip. Live pillars are backed by the DB / Prometheus; rest PREVIEW.
 */
@Service
@RequiredArgsConstructor
public class StatusStripService {

  private final DashboardAggregateRepository aggregateRepository;
  private final ActionQueueService actionQueueService;
  private final MetricsQueryService metricsQueryService;

  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public StatusStripResponse getStatusStrip() {
    List<Pillar> pillars = new ArrayList<>();

    pillars.add(preview("mrr", "MRR", "Connect billing to enable"));

    long activeTeams = aggregateRepository.countActiveTeams();
    pillars.add(
        live(
            "active-teams",
            "Active teams",
            String.valueOf(activeTeams),
            "ok",
            Optional.of("/teams")));

    pillars.add(preview("churn-30d", "Churn 30d", "Connect billing to enable"));
    pillars.add(preview("dau", "DAU", "Connect PostHog to enable"));
    pillars.add(errorRatePillar());
    pillars.add(p95Pillar());

    long backlog = aggregateRepository.outboxBacklog();
    long deadLetter = aggregateRepository.outboxDeadLetter();
    String outboxSeverity = deadLetter > 0 || backlog > 100 ? "crit" : backlog > 0 ? "warn" : "ok";
    pillars.add(
        live(
            "outbox-depth",
            "Outbox depth",
            String.valueOf(backlog + deadLetter),
            outboxSeverity,
            Optional.of("/notifications")));

    int alerts = actionQueueService.openCount(SecurityUtils.getBackofficePrincipal());
    pillars.add(
        live(
            "alerts",
            "Alerts",
            String.valueOf(alerts),
            alerts > 0 ? "warn" : "ok",
            Optional.empty()));

    return new StatusStripResponse(pillars);
  }

  private Pillar errorRatePillar() {
    Metric m = metricsQueryService.errorRatePct();
    return switch (m.state()) {
      case UNAVAILABLE -> preview("error-rate", "Error rate", metricsUnavailableCta());
      // No traffic in window: neutral "—", never a green "0%" that could mask a down fleet.
      case NO_DATA -> live("error-rate", "Error rate", "—", "info", Optional.empty());
      case LIVE ->
          live(
              "error-rate",
              "Error rate",
              formatPct(m.value()),
              m.value() >= 5 ? "crit" : m.value() >= 1 ? "warn" : "ok",
              Optional.empty());
    };
  }

  private Pillar p95Pillar() {
    Metric m = metricsQueryService.p95Millis();
    return switch (m.state()) {
      case UNAVAILABLE -> preview("p95", "p95 latency", metricsUnavailableCta());
      case NO_DATA -> live("p95", "p95 latency", "—", "info", Optional.empty());
      case LIVE ->
          live(
              "p95",
              "p95 latency",
              Math.round(m.value()) + " ms",
              m.value() >= 1000 ? "crit" : m.value() >= 500 ? "warn" : "ok",
              Optional.empty());
    };
  }

  /** Distinguishes "Prometheus not wired" (dev) from "configured but unreachable" (prod). */
  private String metricsUnavailableCta() {
    return metricsQueryService.configured()
        ? "Prometheus unreachable — check backend logs"
        : "Set PROMETHEUS_URL to enable";
  }

  private static String formatPct(double pct) {
    return (Math.round(pct * 10.0) / 10.0) + "%";
  }

  private static Pillar live(
      String key, String label, String value, String severity, Optional<String> deeplink) {
    return new Pillar(
        key,
        label,
        PanelStatus.LIVE,
        Optional.of(value),
        Optional.of(severity),
        List.of(),
        deeplink,
        Optional.empty());
  }

  private static Pillar preview(String key, String label, String cta) {
    return new Pillar(
        key,
        label,
        PanelStatus.PREVIEW,
        Optional.empty(),
        Optional.empty(),
        List.of(),
        Optional.empty(),
        Optional.of(cta));
  }
}
