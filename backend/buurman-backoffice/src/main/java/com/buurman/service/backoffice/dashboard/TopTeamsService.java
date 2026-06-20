package com.buurman.service.backoffice.dashboard;

import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.backoffice.PanelStatus;
import com.buurman.dto.response.backoffice.dashboard.TopTeamsResponse;
import com.buurman.repository.backoffice.DashboardAggregateRepository;

import lombok.RequiredArgsConstructor;

/** Top teams by activity (MRR ranking arrives with billing). */
@Service
@RequiredArgsConstructor
public class TopTeamsService {

  private static final int LIMIT = 5;

  private final DashboardAggregateRepository aggregateRepository;

  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public TopTeamsResponse getTopTeams() {
    return new TopTeamsResponse(
        PanelStatus.LIVE,
        Optional.empty(),
        Optional.empty(),
        "activity",
        aggregateRepository.topTeamsByActivity(LIMIT));
  }
}
