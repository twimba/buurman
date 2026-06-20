package com.buurman.dto.response.backoffice.dashboard;

import java.util.List;
import java.util.Optional;

import com.buurman.domain.backoffice.PanelStatus;
import com.buurman.util.SkipTestCoverage;

/** Top teams. MVP ranks by activity (entity count); MRR ranking arrives with billing. */
@SkipTestCoverage
public record TopTeamsResponse(
    PanelStatus status,
    Optional<String> previewCta,
    Optional<String> docsLink,
    String rankedBy,
    List<TopTeam> teams) {

  @SkipTestCoverage
  public record TopTeam(String identifier, String name, long activityScore, String deeplink) {}
}
