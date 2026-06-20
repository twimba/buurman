package com.buurman.service.backoffice.dashboard;

import static com.buurman.repository.backoffice.DashboardAggregateRepository.hasContact;
import static com.buurman.repository.backoffice.DashboardAggregateRepository.hasContract;
import static com.buurman.repository.backoffice.DashboardAggregateRepository.hasDocument;
import static com.buurman.repository.backoffice.DashboardAggregateRepository.hasProperty;

import java.util.List;
import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.backoffice.PanelStatus;
import com.buurman.dto.response.backoffice.dashboard.FunnelResponse;
import com.buurman.dto.response.backoffice.dashboard.FunnelResponse.FunnelStage;
import com.buurman.repository.backoffice.DashboardAggregateRepository;

import lombok.RequiredArgsConstructor;

/**
 * Activation funnel over the cumulative onboarding journey: teams -> property -> contact ->
 * contract -> document. Each stage is a strict subset of the previous, so the funnel always tapers.
 */
@Service
@RequiredArgsConstructor
public class ActivationFunnelService {

  private final DashboardAggregateRepository aggregateRepository;

  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public FunnelResponse getFunnel() {
    // Cumulative milestones — each stage requires all previous ones, guaranteeing a monotonic
    // funnel (every stage is a subset of the one above it), so step-conversion and drop-off are
    // always well-defined.
    long teams = aggregateRepository.countActiveTeamsMatching();
    long withProperty = aggregateRepository.countActiveTeamsMatching(hasProperty());
    long withContact = aggregateRepository.countActiveTeamsMatching(hasProperty(), hasContact());
    long withContract =
        aggregateRepository.countActiveTeamsMatching(hasProperty(), hasContact(), hasContract());
    long withDocument =
        aggregateRepository.countActiveTeamsMatching(
            hasProperty(), hasContact(), hasContract(), hasDocument());
    List<FunnelStage> stages =
        List.of(
            // First stage is the funnel entry: 100% step-conversion, no drop-off by definition.
            new FunnelStage("Teams", teams, 100.0, 100.0, 0L),
            stage("With a property", withProperty, teams, teams),
            stage("With a contact", withContact, teams, withProperty),
            stage("With a contract", withContract, teams, withContact),
            stage("With a document", withDocument, teams, withContract));
    return new FunnelResponse(PanelStatus.LIVE, Optional.empty(), Optional.empty(), stages);
  }

  /**
   * Builds a non-entry stage with both cumulative ({@code pctOfTop}) and step ({@code
   * pctOfPrevious}) conversion plus the absolute drop-off from the preceding stage — the metrics
   * that reveal where the activation leak is.
   */
  private static FunnelStage stage(String label, long count, long top, long previous) {
    long dropOff = Math.max(0, previous - count);
    return new FunnelStage(label, count, pct(count, top), pct(count, previous), dropOff);
  }

  private static double pct(long value, long total) {
    if (total <= 0) {
      return 0.0;
    }
    return Math.round((value * 1000.0) / total) / 10.0;
  }
}
