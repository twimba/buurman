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

import lombok.RequiredArgsConstructor;

/** The pinned 8-pillar status strip. Live pillars are backed by the DB; the rest are PREVIEW. */
@Service
@RequiredArgsConstructor
public class StatusStripService {

  private final DashboardAggregateRepository aggregateRepository;
  private final ActionQueueService actionQueueService;

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
    pillars.add(preview("error-rate", "Error rate", "Connect Prometheus to enable"));
    pillars.add(preview("p95", "p95 latency", "Connect Prometheus to enable"));

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
