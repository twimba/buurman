package com.buurman.dto.response.backoffice.dashboard;

import java.util.List;
import java.util.Optional;

import com.buurman.domain.backoffice.PanelStatus;
import com.buurman.util.SkipTestCoverage;

/** Activation funnel: teams -> teams with a property -> teams with a contract. */
@SkipTestCoverage
public record FunnelResponse(
    PanelStatus status,
    Optional<String> previewCta,
    Optional<String> docsLink,
    List<FunnelStage> stages) {

  @SkipTestCoverage
  public record FunnelStage(
      String label, long count, double pctOfTop, double pctOfPrevious, long dropOff) {}
}
