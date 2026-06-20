package com.buurman.service.backoffice.dashboard;

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

/** Activation funnel: teams -> teams with a property -> teams with a contract. */
@Service
@RequiredArgsConstructor
public class ActivationFunnelService {

  private final DashboardAggregateRepository aggregateRepository;

  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public FunnelResponse getFunnel() {
    long teams = aggregateRepository.countActiveTeams();
    long withProperty = aggregateRepository.countActiveTeamsWithProperties();
    long withContract = aggregateRepository.countActiveTeamsWithContracts();
    List<FunnelStage> stages =
        List.of(
            new FunnelStage("Teams", teams, 100.0),
            new FunnelStage("With a property", withProperty, pct(withProperty, teams)),
            new FunnelStage("With a contract", withContract, pct(withContract, teams)));
    return new FunnelResponse(PanelStatus.LIVE, Optional.empty(), Optional.empty(), stages);
  }

  private static double pct(long value, long total) {
    if (total <= 0) {
      return 0.0;
    }
    return Math.round((value * 1000.0) / total) / 10.0;
  }
}
