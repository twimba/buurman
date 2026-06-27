package com.buurman.dto.response.backoffice.dashboard;

import java.util.List;
import java.util.Optional;

import com.buurman.domain.backoffice.PanelStatus;
import com.buurman.util.SkipTestCoverage;

/** Product entities created in the last 7 days (and totals), across non-demo teams. */
@SkipTestCoverage
public record ProductEntitiesResponse(
    PanelStatus status,
    Optional<String> previewCta,
    Optional<String> docsLink,
    List<EntityCount> entities) {

  @SkipTestCoverage
  public record EntityCount(String label, long last7Days, long total) {}
}
