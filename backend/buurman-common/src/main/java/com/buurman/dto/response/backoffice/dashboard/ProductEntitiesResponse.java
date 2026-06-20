package com.buurman.dto.response.backoffice.dashboard;

import java.util.List;
import java.util.Optional;

import com.buurman.domain.backoffice.PanelStatus;
import com.buurman.util.SkipTestCoverage;

/** Product entities created today (and totals), across all non-demo teams. */
@SkipTestCoverage
public record ProductEntitiesResponse(
    PanelStatus status,
    Optional<String> previewCta,
    Optional<String> docsLink,
    List<EntityCount> entities) {

  @SkipTestCoverage
  public record EntityCount(String label, long today, long total) {}
}
