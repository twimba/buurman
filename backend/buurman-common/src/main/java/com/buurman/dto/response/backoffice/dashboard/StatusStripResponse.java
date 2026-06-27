package com.buurman.dto.response.backoffice.dashboard;

import java.util.List;
import java.util.Optional;

import com.buurman.domain.backoffice.PanelStatus;
import com.buurman.util.SkipTestCoverage;

/**
 * The pinned 8-pillar status strip. Each pillar carries its own {@link PanelStatus} so live and
 * preview pillars can coexist in a single row.
 */
@SkipTestCoverage
public record StatusStripResponse(List<Pillar> pillars) {

  @SkipTestCoverage
  public record Pillar(
      String key,
      String label,
      PanelStatus status,
      Optional<String> value,
      Optional<String> severity,
      List<Double> sparkline,
      Optional<String> deeplink,
      Optional<String> previewCta) {}
}
